package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.SavedLocation
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherSnapshot
import com.example.domain.util.UnitFormatter
import com.example.ui.components.AirQualitySection
import com.example.ui.components.AtmosphericBackdrop
import com.example.ui.components.AtmosphericDetailsSection
import com.example.ui.components.BangladeshAdvisorySection
import com.example.ui.components.HeroWeatherIllustration
import com.example.ui.components.HourlyForecastSection
import com.example.ui.components.RainForecastSection
import com.example.ui.components.SevenDayForecastSection
import com.example.ui.components.SunMoonSection
import com.example.ui.components.WeatherConditionBadgeIcon
import com.example.ui.theme.AtmosSkyBlue
import com.example.ui.viewmodel.HomeUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeState: HomeUiState,
    savedLocations: List<SavedLocation>,
    prefs: UserPreferences,
    hasLocationPermission: Boolean,
    onRefresh: () -> Unit,
    onUseGps: () -> Unit,
    onSelectLocation: (SavedLocation) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onOpenMap: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLocationSheet by remember { mutableStateOf(false) }
    var dismissedLocationBanner by remember { mutableStateOf(false) }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
        if (granted) onUseGps()
    }

    val snapshot = (homeState as? HomeUiState.Success)?.snapshot
    val bgCondition = snapshot?.current?.condition ?: WeatherCondition.PARTLY_CLOUDY
    val bgIsDay = snapshot?.current?.isDay ?: true

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphericBackdrop(condition = bgCondition, isDay = bgIsDay)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Chrome Bar
            HomeTopBar(
                activeLocationName = snapshot?.location?.name
                    ?: savedLocations.firstOrNull { it.id == prefs.selectedLocationId }?.name
                    ?: "Dhaka",
                isGps = snapshot?.location?.isCurrentLocation == true,
                isRefreshing = (homeState as? HomeUiState.Success)?.isRefreshing == true,
                onSwitchLocationClick = { showLocationSheet = true },
                onRefreshClick = onRefresh,
                onOpenMap = onOpenMap,
                onOpenLocations = onOpenLocations,
                onOpenSettings = onOpenSettings
            )

            when (homeState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.status_loading),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .widthIn(max = 420.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Black.copy(alpha = 0.30f))
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.error_network_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = homeState.message.ifBlank {
                                    stringResource(R.string.error_network_subtitle)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = onRefresh,
                                    modifier = Modifier.testTag("error_retry_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AtmosSkyBlue,
                                        contentColor = Color(0xFF0B1628)
                                    )
                                ) {
                                    Text(stringResource(R.string.action_retry))
                                }
                                OutlinedButton(onClick = onOpenLocations) {
                                    Text(
                                        text = stringResource(R.string.action_choose_city),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                is HomeUiState.Success -> {
                    val data = homeState.snapshot
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 640.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Contextual GPS Permission Prompt (only if not yet granted)
                        if (!hasLocationPermission && !dismissedLocationBanner) {
                            item("gps_prompt") {
                                LocationPermissionPromptBar(
                                    onAllow = {
                                        locationPermLauncher.launch(
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    },
                                    onDismiss = { dismissedLocationBanner = true }
                                )
                            }
                        }

                        // Offline / Banner Notice
                        if (data.isFromCache || homeState.bannerError != null) {
                            item("cache_notice") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.28f))
                                        .clickable(onClick = onRefresh)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = buildString {
                                            append(stringResource(R.string.status_offline_cache))
                                            homeState.bannerError?.let { append(" · $it") }
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.88f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_retry),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AtmosSkyBlue,
                                        modifier = Modifier.testTag("retry_banner_button")
                                    )
                                }
                            }
                        }

                        // Hero Current Weather
                        item("hero_weather") {
                            HeroWeatherBlock(snapshot = data, prefs = prefs)
                        }

                        // Hourly 24h Timeline + Curve
                        item("hourly") {
                            HourlyForecastSection(
                                hourly = data.hourly,
                                prefs = prefs
                            )
                        }

                        // Prioritize Bangladesh Meteorological Insights right below Hourly when in Bangladesh
                        if (data.bangladeshAdvisory.isBangladeshOrTropicalContext) {
                            item("bd_advisory_top") {
                                BangladeshAdvisorySection(
                                    report = data.bangladeshAdvisory,
                                    prefs = prefs
                                )
                            }
                        }

                        // 7-Day Forecast
                        item("daily") {
                            SevenDayForecastSection(
                                daily = data.daily,
                                prefs = prefs
                            )
                        }

                        // Rain Forecast & Graph
                        item("rain") {
                            RainForecastSection(
                                hourly = data.hourly,
                                prefs = prefs
                            )
                        }

                        // Atmospheric Weather Details
                        item("details") {
                            AtmosphericDetailsSection(
                                current = data.current,
                                prefs = prefs
                            )
                        }

                        // Air Quality
                        data.airQuality?.let { aqi ->
                            item("aqi") {
                                AirQualitySection(aqi = aqi)
                            }
                        }

                        // Sun & Moon
                        item("sun_moon") {
                            SunMoonSection(
                                astronomy = data.astronomy,
                                prefs = prefs
                            )
                        }

                        if (!data.bangladeshAdvisory.isBangladeshOrTropicalContext) {
                            item("bd_advisory_bottom") {
                                BangladeshAdvisorySection(
                                    report = data.bangladeshAdvisory,
                                    prefs = prefs
                                )
                            }
                        }

                        item("footer_spacer") {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showLocationSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.locations_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(4.dp))

                // Use GPS option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        .clickable {
                            showLocationSheet = false
                            if (hasLocationPermission) {
                                onUseGps()
                            } else {
                                locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        }
                        .padding(14.dp)
                        .testTag("sheet_gps_option"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.cd_gps_location),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                savedLocations.forEach { loc ->
                    val isSelected = loc.id == prefs.selectedLocationId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surfaceVariant
                                else Color.Transparent
                            )
                            .clickable {
                                showLocationSheet = false
                                onSelectLocation(loc)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loc.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (loc.country.isNotBlank()) {
                                Text(
                                    text = listOf(loc.admin1, loc.country).filter { it.isNotBlank() }.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        loc.lastTempC?.let { t ->
                            Text(
                                text = UnitFormatter.formatTemp(t, prefs.temperatureUnit),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = {
                        showLocationSheet = false
                        onOpenLocations()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.cd_open_locations))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    activeLocationName: String,
    isGps: Boolean,
    isRefreshing: Boolean,
    onSwitchLocationClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location switcher pill
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .minimumInteractiveComponentSize()
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.22f))
                .border(0.8.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(onClick = onSwitchLocationClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("location_switcher_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGps) Icons.Rounded.MyLocation else Icons.Rounded.LocationOn,
                contentDescription = stringResource(R.string.cd_switch_location),
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = activeLocationName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        if (isRefreshing) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(18.dp)
            )
        } else {
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier.testTag("refresh_weather_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.cd_refresh),
                    tint = Color.White
                )
            }
        }

        IconButton(
            onClick = onOpenMap,
            modifier = Modifier.testTag("open_map_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Map,
                contentDescription = stringResource(R.string.cd_open_map),
                tint = Color.White
            )
        }

        IconButton(
            onClick = onOpenLocations,
            modifier = Modifier.testTag("open_locations_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.AddLocationAlt,
                contentDescription = stringResource(R.string.cd_open_locations),
                tint = Color.White
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("open_settings_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.cd_open_settings),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun HeroWeatherBlock(
    snapshot: WeatherSnapshot,
    prefs: UserPreferences
) {
    val context = LocalContext.current
    val current = snapshot.current
    val conditionText = UnitFormatter.conditionLabel(current.condition, context)
    val localClock = UnitFormatter.formatLocalClock(snapshot.timezoneId, prefs.language)
    val updatedAgo = UnitFormatter.formatUpdatedAgo(snapshot.lastUpdatedEpochMillis, context)
    val peakProb = snapshot.hourly.take(12).maxOfOrNull { it.precipProbPercent } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 10.dp)
            .testTag("hero_weather_block")
    ) {
        Text(
            text = listOf(localClock, updatedAgo).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.78f)
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = UnitFormatter.formatTemp(current.tempC, prefs.temperatureUnit),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    text = conditionText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            HeroWeatherIllustration(
                condition = current.condition,
                isDay = current.isDay,
                size = 104.dp
            )
        }

        Spacer(Modifier.height(10.dp))

        val feelsStr = UnitFormatter.formatTemp(current.feelsLikeC, prefs.temperatureUnit)
        val highStr = UnitFormatter.formatTemp(current.todayHighC, prefs.temperatureUnit)
        val lowStr = UnitFormatter.formatTemp(current.todayLowC, prefs.temperatureUnit)
        Text(
            text = "${stringResource(R.string.feels_like_fmt, feelsStr)}   ·   ${
                stringResource(R.string.high_low_fmt, highStr, lowStr)
            }",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.92f)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = UnitFormatter.buildTodaySummary(
                conditionLabel = conditionText,
                highC = current.todayHighC,
                lowC = current.todayLowC,
                feelsLikeC = current.feelsLikeC,
                windKmh = current.windSpeedKmh,
                peakPrecipProb = peakProb,
                prefs = prefs
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.84f)
        )
    }
}

@Composable
private fun LocationPermissionPromptBar(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.MyLocation,
            contentDescription = null,
            tint = AtmosSkyBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.location_permission_banner_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.location_permission_banner_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onAllow) {
            Text(stringResource(R.string.action_allow_location), color = AtmosSkyBlue)
        }
        TextButton(onClick = onDismiss) {
            Text("×", color = Color.White.copy(alpha = 0.7f))
        }
    }
}
