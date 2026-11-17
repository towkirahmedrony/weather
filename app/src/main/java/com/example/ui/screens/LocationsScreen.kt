package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.BangladeshQuickCities
import com.example.domain.model.SavedLocation
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherCondition
import com.example.domain.util.UnitFormatter
import com.example.ui.components.WeatherConditionBadgeIcon
import com.example.ui.components.skyColorsFor
import com.example.ui.viewmodel.LocationsSearchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    savedLocations: List<SavedLocation>,
    searchState: LocationsSearchState,
    prefs: UserPreferences,
    onBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectLocation: (SavedLocation) -> Unit,
    onAddLocation: (SavedLocation) -> Unit,
    onRemoveLocation: (String) -> Unit,
    onRefreshSummaries: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var compareMode by remember { mutableStateOf(false) }
    val selectedForCompare = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        onRefreshSummaries()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.locations_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("locations_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (savedLocations.size >= 2) {
                        TextButton(
                            onClick = {
                                compareMode = !compareMode
                                if (compareMode && selectedForCompare.isEmpty()) {
                                    selectedForCompare.addAll(savedLocations.take(2).map { it.id })
                                }
                            },
                            modifier = Modifier.testTag("compare_mode_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (compareMode) stringResource(R.string.compare_done)
                                else stringResource(R.string.compare_mode)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Box
                item("search_bar") {
                    OutlinedTextField(
                        value = searchState.query,
                        onValueChange = onSearchQueryChanged,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_city_hint),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchState.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else if (searchState.query.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_search_input")
                    )
                }

                // Search Results (when typing)
                if (searchState.query.trim().length >= 2) {
                    if (searchState.results.isEmpty() && !searchState.isSearching) {
                        item("no_results") {
                            Text(
                                text = stringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    } else {
                        items(searchState.results, key = { it.id }) { result ->
                            val alreadySaved = savedLocations.any { it.id == result.id }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddLocation(result)
                                        onBack()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = listOf(result.admin1, result.country)
                                                .filter { it.isNotBlank() }
                                                .joinToString(", "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = if (alreadySaved) Icons.Rounded.CheckCircle else Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Bangladesh Quick Cities
                item("bd_quick") {
                    Column {
                        Text(
                            text = stringResource(R.string.bangladesh_quick_cities),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BangladeshQuickCities.all.forEach { city ->
                                val isSaved = savedLocations.any { it.id == city.id }
                                FilterChip(
                                    selected = isSaved,
                                    onClick = {
                                        onAddLocation(city)
                                        onBack()
                                    },
                                    label = { Text(city.name) },
                                    leadingIcon = {
                                        if (!isSaved) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Compare Table
                item("compare_table") {
                    val compared = savedLocations.filter { it.id in selectedForCompare }
                    AnimatedVisibility(visible = compareMode && compared.size >= 2) {
                        CompareWeatherMatrix(
                            cities = compared.take(3),
                            prefs = prefs
                        )
                    }
                }

                if (compareMode) {
                    item("compare_hint") {
                        Text(
                            text = stringResource(R.string.compare_select_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Saved Locations List
                items(savedLocations, key = { it.id }) { loc ->
                    val condition = loc.lastConditionCode?.let {
                        WeatherCondition.fromWmoCode(it)
                    } ?: WeatherCondition.PARTLY_CLOUDY
                    val sky = skyColorsFor(condition, loc.lastIsDay)
                    val isSelected = loc.id == prefs.selectedLocationId
                    val isCompared = loc.id in selectedForCompare

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.horizontalGradient(listOf(sky.top, sky.mid)))
                            .then(
                                if (isSelected) Modifier.border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                                else Modifier
                            )
                            .clickable {
                                if (compareMode) {
                                    if (isCompared) {
                                        selectedForCompare.remove(loc.id)
                                    } else if (selectedForCompare.size < 3) {
                                        selectedForCompare.add(loc.id)
                                    }
                                } else {
                                    onSelectLocation(loc)
                                    onBack()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("saved_location_${loc.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (compareMode) {
                                Checkbox(
                                    checked = isCompared,
                                    onCheckedChange = { checked ->
                                        if (checked && selectedForCompare.size < 3) {
                                            selectedForCompare.add(loc.id)
                                        } else if (!checked) {
                                            selectedForCompare.remove(loc.id)
                                        }
                                    }
                                )
                                Spacer(Modifier.width(4.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (loc.isCurrentLocation) {
                                        Icon(
                                            imageVector = Icons.Rounded.MyLocation,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = loc.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = listOf(
                                        UnitFormatter.formatLocalClock(loc.timezone, prefs.language),
                                        loc.country
                                    ).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    WeatherConditionBadgeIcon(
                                        condition = condition,
                                        isDay = loc.lastIsDay,
                                        contentDescription = null,
                                        size = 16.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = UnitFormatter.conditionLabel(condition, context),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = loc.lastTempC?.let {
                                        UnitFormatter.formatTemp(it, prefs.temperatureUnit)
                                    } ?: "--°",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White
                                )
                                if (loc.lastHighC != null && loc.lastLowC != null) {
                                    Text(
                                        text = stringResource(
                                            R.string.high_low_fmt,
                                            UnitFormatter.formatTemp(loc.lastHighC, prefs.temperatureUnit),
                                            UnitFormatter.formatTemp(loc.lastLowC, prefs.temperatureUnit)
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.82f)
                                    )
                                }
                            }

                            if (!compareMode && savedLocations.size > 1) {
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onRemoveLocation(loc.id) },
                                    modifier = Modifier.testTag("delete_${loc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.delete_location),
                                        tint = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareWeatherMatrix(
    cities: List<SavedLocation>,
    prefs: UserPreferences
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("compare_matrix")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(96.dp))
                cities.forEach { c ->
                    Text(
                        text = c.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            CompareRow(
                label = stringResource(R.string.settings_temp_unit),
                values = cities.map {
                    it.lastTempC?.let { t -> UnitFormatter.formatTemp(t, prefs.temperatureUnit) } ?: "--"
                }
            )
            CompareRow(
                label = stringResource(R.string.detail_humidity),
                values = cities.map { it.lastHumidity?.let { h -> "$h%" } ?: "--" }
            )
            CompareRow(
                label = stringResource(R.string.detail_wind),
                values = cities.map {
                    it.lastWindKmh?.let { w -> UnitFormatter.formatWind(w, prefs.windSpeedUnit) } ?: "--"
                }
            )
            CompareRow(
                label = stringResource(R.string.layer_precipitation),
                values = cities.map { it.lastPrecipProb?.let { p -> "$p%" } ?: "--" }
            )
            CompareRow(
                label = stringResource(R.string.detail_uv_index),
                values = cities.map { it.lastUv?.let { u -> "${u.toInt()}" } ?: "--" }
            )
            CompareRow(
                label = stringResource(R.string.aqi_us_label),
                values = cities.map { it.lastAqi?.toString() ?: "--" }
            )
        }
    }
}

@Composable
private fun CompareRow(label: String, values: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        values.forEach { v ->
            Text(
                text = v,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
