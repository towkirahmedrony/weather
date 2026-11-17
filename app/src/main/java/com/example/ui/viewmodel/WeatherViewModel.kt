package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AtmosApplication
import com.example.data.local.PreferencesManager
import com.example.data.location.LocationTracker
import com.example.data.repository.WeatherRepository
import com.example.data.repository.WeatherResult
import com.example.domain.model.AppLanguage
import com.example.domain.model.GridWeatherPoint
import com.example.domain.model.PrecipitationUnit
import com.example.domain.model.SavedLocation
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.ThemePreference
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherMapLayer
import com.example.domain.model.WeatherSnapshot
import com.example.domain.model.WindSpeedUnit
import com.example.widget.WeatherWidgetUpdater
import com.example.worker.WeatherSyncWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val snapshot: WeatherSnapshot,
        val isRefreshing: Boolean = false,
        val bannerError: String? = null
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

data class LocationsSearchState(
    val query: String = "",
    val results: List<SavedLocation> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

data class WeatherMapState(
    val activeLayer: WeatherMapLayer = WeatherMapLayer.PRECIPITATION,
    val centerLat: Double = 23.8103,
    val centerLon: Double = 90.4125,
    val zoomLevel: Int = 6,
    val radarPath: String? = null,
    val gridPoints: List<GridWeatherPoint> = emptyList(),
    val selectedPoint: GridWeatherPoint? = null,
    val isLoading: Boolean = false
)

class WeatherViewModel(
    private val app: Application,
    private val repository: WeatherRepository,
    private val preferencesManager: PreferencesManager,
    private val locationTracker: LocationTracker
) : AndroidViewModel(app) {

    val preferences: StateFlow<UserPreferences> = preferencesManager.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    val savedLocations: StateFlow<List<SavedLocation>> = repository.observeSavedLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _homeState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(LocationsSearchState())
    val searchState: StateFlow<LocationsSearchState> = _searchState.asStateFlow()

    private val _mapState = MutableStateFlow(WeatherMapState())
    val mapState: StateFlow<WeatherMapState> = _mapState.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(locationTracker.hasLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private var searchJob: Job? = null
    private var mapJob: Job? = null

    init {
        initializeWeather()
    }

    private fun initializeWeather() {
        viewModelScope.launch {
            repository.ensureDefaultLocationsSeeded()
            val prefs = preferencesManager.preferencesFlow.first()
            val locations = repository.observeSavedLocations().first()
            val selected = locations.firstOrNull { it.id == prefs.selectedLocationId }
                ?: locations.firstOrNull()

            if (selected != null) {
                loadWeatherFor(selected, forceRefresh = false)
                _mapState.update {
                    it.copy(centerLat = selected.latitude, centerLon = selected.longitude)
                }
            }
            // Refresh summaries in background for saved cities comparison
            launch { repository.refreshAllSavedLocationsSummary() }
        }
    }

    fun updatePermissionState() {
        _hasLocationPermission.value = locationTracker.hasLocationPermission()
    }

    fun selectLocation(location: SavedLocation) {
        viewModelScope.launch {
            preferencesManager.setSelectedLocationId(location.id)
            _mapState.update {
                it.copy(centerLat = location.latitude, centerLon = location.longitude)
            }
            loadWeatherFor(location, forceRefresh = false)
        }
    }

    fun refreshActiveLocation() {
        viewModelScope.launch {
            val prefs = preferences.value
            val locations = savedLocations.value
            val active = locations.firstOrNull { it.id == prefs.selectedLocationId }
                ?: locations.firstOrNull()
                ?: return@launch
            loadWeatherFor(active, forceRefresh = true)
        }
    }

    fun useCurrentGpsLocation() {
        updatePermissionState()
        viewModelScope.launch {
            val currentSuccess = _homeState.value as? HomeUiState.Success
            if (currentSuccess != null) {
                _homeState.value = currentSuccess.copy(isRefreshing = true, bannerError = null)
            } else {
                _homeState.value = HomeUiState.Loading
            }

            when (val res = repository.refreshFromCurrentGpsLocation()) {
                is WeatherResult.Success -> {
                    preferencesManager.setSelectedLocationId(res.snapshot.location.id)
                    _homeState.value = HomeUiState.Success(res.snapshot, isRefreshing = false)
                    _mapState.update {
                        it.copy(
                            centerLat = res.snapshot.location.latitude,
                            centerLon = res.snapshot.location.longitude
                        )
                    }
                    WeatherWidgetUpdater.updateAllWidgets(app, res.snapshot, preferences.value)
                }
                is WeatherResult.Error -> {
                    val fallback = res.cachedFallback ?: currentSuccess?.snapshot
                    if (fallback != null) {
                        _homeState.value = HomeUiState.Success(
                            snapshot = fallback,
                            isRefreshing = false,
                            bannerError = res.message
                        )
                    } else {
                        _homeState.value = HomeUiState.Error(res.message)
                    }
                }
                null -> {
                    if (currentSuccess != null) {
                        _homeState.value = currentSuccess.copy(
                            isRefreshing = false,
                            bannerError = "GPS unavailable — showing saved city"
                        )
                    } else {
                        refreshActiveLocation()
                    }
                }
            }
        }
    }

    private suspend fun loadWeatherFor(location: SavedLocation, forceRefresh: Boolean) {
        val currentSuccess = _homeState.value as? HomeUiState.Success
        val cached = repository.getCachedWeather(location.id)

        if (cached != null) {
            _homeState.value = HomeUiState.Success(cached, isRefreshing = true)
        } else if (currentSuccess == null) {
            _homeState.value = HomeUiState.Loading
        } else {
            _homeState.value = currentSuccess.copy(isRefreshing = true, bannerError = null)
        }

        when (val result = repository.getLocationWeather(location, forceRefresh)) {
            is WeatherResult.Success -> {
                _homeState.value = HomeUiState.Success(result.snapshot, isRefreshing = false)
                WeatherWidgetUpdater.updateAllWidgets(app, result.snapshot, preferences.value)
            }
            is WeatherResult.Error -> {
                val fallback = result.cachedFallback ?: cached
                if (fallback != null) {
                    _homeState.value = HomeUiState.Success(
                        snapshot = fallback.copy(isFromCache = true),
                        isRefreshing = false,
                        bannerError = result.message
                    )
                } else {
                    _homeState.value = HomeUiState.Error(result.message)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchState.update { it.copy(query = query, errorMessage = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _searchState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(320)
            _searchState.update { it.copy(isSearching = true) }
            repository.searchLocations(query)
                .onSuccess { list ->
                    _searchState.update { it.copy(results = list, isSearching = false) }
                }
                .onFailure { err ->
                    _searchState.update {
                        it.copy(isSearching = false, errorMessage = err.localizedMessage)
                    }
                }
        }
    }

    fun addAndSelectLocation(location: SavedLocation) {
        viewModelScope.launch {
            repository.saveLocation(location)
            _searchState.value = LocationsSearchState()
            selectLocation(location)
        }
    }

    fun removeLocation(locationId: String) {
        viewModelScope.launch {
            val wasSelected = preferences.value.selectedLocationId == locationId
            repository.removeLocation(locationId)
            if (wasSelected) {
                val remaining = repository.observeSavedLocations().first()
                remaining.firstOrNull()?.let { selectLocation(it) }
            }
        }
    }

    fun refreshSavedSummaries() {
        viewModelScope.launch {
            repository.refreshAllSavedLocationsSummary()
        }
    }

    fun setMapLayer(layer: WeatherMapLayer) {
        _mapState.update { it.copy(activeLayer = layer) }
        loadMapData(_mapState.value.centerLat, _mapState.value.centerLon, _mapState.value.zoomLevel)
    }

    fun updateMapViewport(centerLat: Double, centerLon: Double, zoom: Int) {
        _mapState.update {
            it.copy(centerLat = centerLat, centerLon = centerLon, zoomLevel = zoom)
        }
        loadMapData(centerLat, centerLon, zoom)
    }

    fun selectMapPoint(point: GridWeatherPoint?) {
        _mapState.update { it.copy(selectedPoint = point) }
    }

    fun loadMapData(centerLat: Double, centerLon: Double, zoom: Int) {
        mapJob?.cancel()
        mapJob = viewModelScope.launch {
            _mapState.update { it.copy(isLoading = true) }
            val spanDeg = when {
                zoom <= 4 -> 22.0
                zoom == 5 -> 14.0
                zoom == 6 -> 8.5
                zoom == 7 -> 5.0
                else -> 3.0
            }
            val radarDeferred = if (_mapState.value.radarPath == null) {
                repository.getRainViewerRadarPath()
            } else {
                _mapState.value.radarPath
            }
            val grid = repository.getSpatialGrid(centerLat, centerLon, spanDeg)
            _mapState.update {
                it.copy(
                    radarPath = radarDeferred,
                    gridPoints = grid,
                    isLoading = false
                )
            }
        }
    }

    // Settings mutations
    fun setTheme(theme: ThemePreference) = viewModelScope.launch {
        preferencesManager.setTheme(theme)
    }

    fun setTemperatureUnit(unit: TemperatureUnit) = viewModelScope.launch {
        preferencesManager.setTemperatureUnit(unit)
        (homeState.value as? HomeUiState.Success)?.snapshot?.let {
            WeatherWidgetUpdater.updateAllWidgets(app, it, preferences.value.copy(temperatureUnit = unit))
        }
    }

    fun setWindSpeedUnit(unit: WindSpeedUnit) = viewModelScope.launch {
        preferencesManager.setWindSpeedUnit(unit)
    }

    fun setPrecipitationUnit(unit: PrecipitationUnit) = viewModelScope.launch {
        preferencesManager.setPrecipitationUnit(unit)
    }

    fun setLanguage(language: AppLanguage, onApplied: () -> Unit = {}) = viewModelScope.launch {
        preferencesManager.setLanguage(language)
        onApplied()
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setNotificationsEnabled(enabled)
    }

    fun setRainAlertsEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setRainAlertsEnabled(enabled)
    }

    fun setSevereAlertsEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setSevereAlertsEnabled(enabled)
    }

    fun setSyncIntervalHours(hours: Int) = viewModelScope.launch {
        preferencesManager.setSyncIntervalHours(hours)
        WeatherSyncWorker.schedulePeriodic(app, hours)
    }

    fun triggerManualSync() {
        WeatherSyncWorker.triggerImmediateSync(app)
        refreshActiveLocation()
    }

    class Factory(private val app: AtmosApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WeatherViewModel(
                app = app,
                repository = app.container.weatherRepository,
                preferencesManager = app.container.preferencesManager,
                locationTracker = app.container.locationTracker
            ) as T
        }
    }
}
