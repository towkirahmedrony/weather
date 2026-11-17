package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AtmosRoute
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LocationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeatherMapScreen
import com.example.ui.theme.AtmosTheme
import com.example.ui.viewmodel.WeatherViewModel
import com.example.util.LocaleHelper

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val app = application as AtmosApplication

        setContent {
            val vm: WeatherViewModel = viewModel(factory = WeatherViewModel.Factory(app))
            val prefs by vm.preferences.collectAsStateWithLifecycle()
            val homeState by vm.homeState.collectAsStateWithLifecycle()
            val savedLocations by vm.savedLocations.collectAsStateWithLifecycle()
            val searchState by vm.searchState.collectAsStateWithLifecycle()
            val mapState by vm.mapState.collectAsStateWithLifecycle()
            val hasLocPerm by vm.hasLocationPermission.collectAsStateWithLifecycle()

            AtmosTheme(themePreference = prefs.themePreference) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = AtmosRoute.Home
                ) {
                    composable<AtmosRoute.Home> {
                        HomeScreen(
                            homeState = homeState,
                            savedLocations = savedLocations,
                            prefs = prefs,
                            hasLocationPermission = hasLocPerm,
                            onRefresh = vm::refreshActiveLocation,
                            onUseGps = vm::useCurrentGpsLocation,
                            onSelectLocation = vm::selectLocation,
                            onPermissionResult = { vm.updatePermissionState() },
                            onOpenMap = { navController.navigate(AtmosRoute.Map) },
                            onOpenLocations = { navController.navigate(AtmosRoute.Locations) },
                            onOpenSettings = { navController.navigate(AtmosRoute.Settings) }
                        )
                    }

                    composable<AtmosRoute.Map> {
                        WeatherMapScreen(
                            mapState = mapState,
                            prefs = prefs,
                            onBack = { navController.popBackStack() },
                            onSelectLayer = vm::setMapLayer,
                            onViewportChanged = vm::updateMapViewport,
                            onSelectPoint = vm::selectMapPoint
                        )
                    }

                    composable<AtmosRoute.Locations> {
                        LocationsScreen(
                            savedLocations = savedLocations,
                            searchState = searchState,
                            prefs = prefs,
                            onBack = { navController.popBackStack() },
                            onSearchQueryChanged = vm::onSearchQueryChanged,
                            onSelectLocation = vm::selectLocation,
                            onAddLocation = vm::addAndSelectLocation,
                            onRemoveLocation = vm::removeLocation,
                            onRefreshSummaries = vm::refreshSavedSummaries
                        )
                    }

                    composable<AtmosRoute.Settings> {
                        SettingsScreen(
                            prefs = prefs,
                            hasLocationPermission = hasLocPerm,
                            onBack = { navController.popBackStack() },
                            onThemeChange = vm::setTheme,
                            onTempUnitChange = vm::setTemperatureUnit,
                            onWindUnitChange = vm::setWindSpeedUnit,
                            onPrecipUnitChange = vm::setPrecipitationUnit,
                            onLanguageChange = { lang ->
                                LocaleHelper.saveLanguageSync(this@MainActivity, lang)
                                vm.setLanguage(lang) {
                                    recreate()
                                }
                            },
                            onNotificationsChange = vm::setNotificationsEnabled,
                            onRainAlertsChange = vm::setRainAlertsEnabled,
                            onSevereAlertsChange = vm::setSevereAlertsEnabled,
                            onSyncIntervalChange = vm::setSyncIntervalHours,
                            onSyncNow = vm::triggerManualSync,
                            onPermissionUpdated = vm::updatePermissionState
                        )
                    }
                }
            }
        }
    }
}
