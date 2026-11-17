package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.SavedLocation
import com.example.domain.model.UserPreferences
import com.example.domain.util.UnitFormatter
import com.example.ui.components.AtmosBottomNav
import com.example.ui.components.AtmosDivider
import com.example.ui.components.AtmosEmptyState
import com.example.ui.components.AtmosScreenHeader
import com.example.ui.components.AtmosTab
import com.example.ui.theme.AtmosColors
import com.example.ui.theme.AtmosType
import com.example.ui.viewmodel.HomeUiState

/**
 * Host for the three redesigned destinations. The bottom navigation bar is part of the
 * screen (not a separate NavHost destination) so the selected tab and the scroll
 * position are simply state on this screen; the system back button returns to Today.
 */
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
    var activeTab by rememberSaveable { mutableStateOf(AtmosTab.TODAY) }
    var showLocationSheet by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
        if (granted) onUseGps()
    }

    BackHandler(enabled = activeTab != AtmosTab.TODAY) {
        activeTab = AtmosTab.TODAY
    }

    LaunchedEffect(activeTab) {
        scrollState.scrollTo(0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AtmosColors.Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                when (homeState) {
                    is HomeUiState.Loading -> LoadingState()

                    is HomeUiState.Error -> AtmosEmptyState(
                        title = stringResource(R.string.error_network_title),
                        message = homeState.message.ifBlank {
                            stringResource(R.string.error_network_subtitle)
                        },
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = onRefresh
                    )

                    is HomeUiState.Success -> {
                        val snapshot = homeState.snapshot
                        PullToRefreshBox(
                            isRefreshing = homeState.isRefreshing,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 20.dp)
                            ) {
                                Spacer(Modifier.height(8.dp))

                                when (activeTab) {
                                    AtmosTab.TODAY -> TodayContent(
                                        snapshot = snapshot,
                                        prefs = prefs,
                                        bannerError = homeState.bannerError,
                                        onOpenAir = { activeTab = AtmosTab.AIR },
                                        onOpenSettings = onOpenSettings,
                                        onOpenLocationSheet = { showLocationSheet = true },
                                        onRetry = onRefresh
                                    )

                                    AtmosTab.AIR -> {
                                        val aqi = snapshot.airQuality
                                        if (aqi != null) {
                                            AirContent(
                                                airQuality = aqi,
                                                locationName = snapshot.location.name,
                                                timeText = UnitFormatter.formatLocalClock(
                                                    snapshot.timezoneId,
                                                    prefs.language
                                                ),
                                                onBack = { activeTab = AtmosTab.TODAY }
                                            )
                                        } else {
                                            AqiUnavailable(
                                                locationName = snapshot.location.name,
                                                timeText = UnitFormatter.formatLocalClock(
                                                    snapshot.timezoneId,
                                                    prefs.language
                                                ),
                                                onBack = { activeTab = AtmosTab.TODAY }
                                            )
                                        }
                                    }

                                    AtmosTab.DETAILS -> DetailsContent(
                                        snapshot = snapshot,
                                        prefs = prefs,
                                        onBack = { activeTab = AtmosTab.TODAY }
                                    )
                                }

                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            AtmosBottomNav(
                selected = activeTab,
                onSelect = { tab -> activeTab = tab }
            )
        }
    }

    if (showLocationSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            sheetState = sheetState,
            containerColor = AtmosColors.Card,
            contentColor = AtmosColors.TextPrimary,
            scrimColor = Color.Black.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(R.string.locations_title),
                    style = AtmosType.CategoryLarge,
                    color = AtmosColors.TextPrimary
                )

                Spacer(Modifier.height(10.dp))

                // GPS option - the old top-bar "use my location" behaviour.
                SheetAction(
                    icon = Icons.Outlined.MyLocation,
                    label = stringResource(R.string.cd_gps_location),
                    tint = AtmosColors.Accent
                ) {
                    showLocationSheet = false
                    if (hasLocationPermission) {
                        onUseGps()
                    } else {
                        locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }

                savedLocations.forEach { location ->
                    SavedLocationRow(
                        location = location,
                        isSelected = location.id == prefs.selectedLocationId,
                        prefs = prefs,
                        onClick = {
                            showLocationSheet = false
                            onSelectLocation(location)
                        }
                    )
                }

                Spacer(Modifier.height(6.dp))
                AtmosDivider()
                Spacer(Modifier.height(6.dp))

                // Refresh / map / manage-locations used to sit in the top bar.
                SheetAction(
                    icon = Icons.Outlined.Refresh,
                    label = stringResource(R.string.cd_refresh)
                ) {
                    showLocationSheet = false
                    onRefresh()
                }
                SheetAction(
                    icon = Icons.Outlined.Map,
                    label = stringResource(R.string.cd_open_map)
                ) {
                    showLocationSheet = false
                    onOpenMap()
                }
                SheetAction(
                    icon = Icons.Outlined.AddLocationAlt,
                    label = stringResource(R.string.cd_open_locations)
                ) {
                    showLocationSheet = false
                    onOpenLocations()
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = AtmosColors.Accent,
            strokeWidth = 2.dp,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.status_loading),
            style = AtmosType.Meta,
            color = AtmosColors.TextMuted
        )
    }
}

@Composable
private fun AqiUnavailable(locationName: String, timeText: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AtmosScreenHeader(
            title = stringResource(R.string.title_air_quality),
            locationName = locationName,
            timeText = timeText,
            onBack = onBack
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.aqi_unavailable),
            style = AtmosType.Body,
            color = AtmosColors.TextMuted
        )
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    tint: Color = AtmosColors.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = AtmosType.Body,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavedLocationRow(
    location: SavedLocation,
    isSelected: Boolean,
    prefs: UserPreferences,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) AtmosColors.Divider else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.name,
                style = AtmosType.Body,
                color = AtmosColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (location.country.isNotBlank()) {
                Text(
                    text = listOf(location.admin1, location.country)
                        .filter { it.isNotBlank() }
                        .joinToString(", "),
                    style = AtmosType.Caption,
                    color = AtmosColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        location.lastTempC?.let { temp ->
            Text(
                text = UnitFormatter.formatTemp(temp, prefs.temperatureUnit),
                style = AtmosType.Value,
                color = AtmosColors.TextPrimary
            )
            Spacer(Modifier.width(8.dp))
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = AtmosColors.Accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
