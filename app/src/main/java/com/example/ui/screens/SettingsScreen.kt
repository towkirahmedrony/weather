package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.PrecipitationUnit
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.ThemePreference
import com.example.domain.model.UserPreferences
import com.example.domain.model.WindSpeedUnit
import com.example.worker.WeatherNotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    hasLocationPermission: Boolean,
    onBack: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onTempUnitChange: (TemperatureUnit) -> Unit,
    onWindUnitChange: (WindSpeedUnit) -> Unit,
    onPrecipUnitChange: (PrecipitationUnit) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onRainAlertsChange: (Boolean) -> Unit,
    onSevereAlertsChange: (Boolean) -> Unit,
    onSyncIntervalChange: (Int) -> Unit,
    onSyncNow: () -> Unit,
    onPermissionUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionUpdated()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onNotificationsChange(true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Appearance & Language
                item("appearance") {
                    SettingsGroupCard(
                        title = stringResource(R.string.settings_appearance),
                        icon = Icons.Rounded.Palette
                    ) {
                        Text(
                            text = stringResource(R.string.settings_theme),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        val themes = listOf(
                            ThemePreference.SYSTEM to stringResource(R.string.theme_system),
                            ThemePreference.LIGHT to stringResource(R.string.theme_light),
                            ThemePreference.DARK to stringResource(R.string.theme_dark)
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themes.forEachIndexed { idx, (pref, label) ->
                                SegmentedButton(
                                    selected = prefs.themePreference == pref,
                                    onClick = { onThemeChange(pref) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, themes.size),
                                    modifier = Modifier.testTag("theme_${pref.name.lowercase()}")
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_language),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val langs = listOf(
                            AppLanguage.ENGLISH to stringResource(R.string.lang_english),
                            AppLanguage.BANGLA to stringResource(R.string.lang_bangla)
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            langs.forEachIndexed { idx, (lang, label) ->
                                SegmentedButton(
                                    selected = prefs.language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, langs.size),
                                    modifier = Modifier.testTag("lang_${lang.code}")
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Units
                item("units") {
                    SettingsGroupCard(
                        title = stringResource(R.string.settings_units),
                        icon = Icons.Rounded.Speed
                    ) {
                        Text(
                            text = stringResource(R.string.settings_temp_unit),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        val temps = listOf(
                            TemperatureUnit.CELSIUS to "°C",
                            TemperatureUnit.FAHRENHEIT to "°F"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            temps.forEachIndexed { idx, (unit, label) ->
                                SegmentedButton(
                                    selected = prefs.temperatureUnit == unit,
                                    onClick = { onTempUnitChange(unit) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, temps.size),
                                    modifier = Modifier.testTag("unit_${unit.name.lowercase()}")
                                ) {
                                    Text(label)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.settings_wind_unit),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        val winds = listOf(
                            WindSpeedUnit.KMH to "km/h",
                            WindSpeedUnit.MS to "m/s",
                            WindSpeedUnit.MPH to "mph",
                            WindSpeedUnit.KNOTS to "kn"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            winds.forEachIndexed { idx, (unit, label) ->
                                SegmentedButton(
                                    selected = prefs.windSpeedUnit == unit,
                                    onClick = { onWindUnitChange(unit) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, winds.size)
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.settings_precip_unit),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        val precips = listOf(
                            PrecipitationUnit.MM to "mm",
                            PrecipitationUnit.INCH to "in"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            precips.forEachIndexed { idx, (unit, label) ->
                                SegmentedButton(
                                    selected = prefs.precipitationUnit == unit,
                                    onClick = { onPrecipUnitChange(unit) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, precips.size)
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }

                // Location & Sync
                item("location_sync") {
                    SettingsGroupCard(
                        title = stringResource(R.string.settings_location_section),
                        icon = Icons.Rounded.LocationOn
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_location_perm),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (hasLocationPermission) stringResource(R.string.perm_granted)
                                    else stringResource(R.string.perm_denied),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (hasLocationPermission) {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                }
                            ) {
                                Text(
                                    if (hasLocationPermission) stringResource(R.string.settings_title)
                                    else stringResource(R.string.action_allow_location)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Text(
                            text = stringResource(R.string.settings_sync_interval),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        val intervals = listOf(1, 3, 6, 12)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            intervals.forEachIndexed { idx, hrs ->
                                SegmentedButton(
                                    selected = prefs.syncIntervalHours == hrs,
                                    onClick = { onSyncIntervalChange(hrs) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, intervals.size)
                                ) {
                                    Text("${hrs}h")
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onSyncNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_now_button")
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_sync_now))
                        }
                    }
                }

                // Notifications
                item("notifications") {
                    SettingsGroupCard(
                        title = stringResource(R.string.settings_notifications),
                        icon = Icons.Rounded.Notifications
                    ) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_notif_updates),
                            checked = prefs.notificationsEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !WeatherNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onNotificationsChange(enable)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_notif_rain),
                            checked = prefs.rainAlertsEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !WeatherNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onRainAlertsChange(enable)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        SettingsSwitchRow(
                            title = stringResource(R.string.settings_notif_severe),
                            checked = prefs.severeAlertsEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !WeatherNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSevereAlertsChange(enable)
                                }
                            }
                        )
                    }
                }

                // Data Sources & Attribution
                item("attribution") {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_data_sources),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.settings_data_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
