package com.example.data.repository

import com.example.domain.model.GridWeatherPoint
import com.example.domain.model.SavedLocation
import com.example.domain.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow

sealed interface WeatherResult {
    data class Success(val snapshot: WeatherSnapshot) : WeatherResult
    data class Error(
        val message: String,
        val isRateLimited: Boolean = false,
        val cachedFallback: WeatherSnapshot? = null
    ) : WeatherResult
}

interface WeatherRepository {
    fun observeSavedLocations(): Flow<List<SavedLocation>>
    suspend fun ensureDefaultLocationsSeeded()
    suspend fun getLocationWeather(location: SavedLocation, forceRefresh: Boolean = false): WeatherResult
    suspend fun getCachedWeather(locationId: String): WeatherSnapshot?
    suspend fun refreshFromCurrentGpsLocation(): WeatherResult?
    suspend fun searchLocations(query: String): Result<List<SavedLocation>>
    suspend fun saveLocation(location: SavedLocation)
    suspend fun removeLocation(locationId: String)
    suspend fun refreshAllSavedLocationsSummary()
    suspend fun getRainViewerRadarPath(): String?
    suspend fun getSpatialGrid(centerLat: Double, centerLon: Double, spanDeg: Double): List<GridWeatherPoint>
}
