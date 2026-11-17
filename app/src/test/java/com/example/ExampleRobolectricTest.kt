package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AdvisorySeverity
import com.example.domain.model.AirQualityData
import com.example.domain.model.AstronomyData
import com.example.domain.model.BangladeshAdvisoryReport
import com.example.domain.model.CurrentWeather
import com.example.domain.model.DailyForecast
import com.example.domain.model.HourlyForecast
import com.example.domain.model.MoonPhaseType
import com.example.domain.model.PressureTrend
import com.example.domain.model.SavedLocation
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherSnapshot
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.AtmosTheme
import com.example.ui.viewmodel.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app name matches Atmos`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Atmos", appName)
    }

    @Test
    fun `home screen renders current weather and navigation controls`() {
        val dhaka = SavedLocation(
            id = "bd_dhaka",
            name = "Dhaka",
            admin1 = "Dhaka Division",
            country = "Bangladesh",
            countryCode = "BD",
            latitude = 23.8103,
            longitude = 90.4125,
            timezone = "Asia/Dhaka"
        )
        val snapshot = WeatherSnapshot(
            location = dhaka,
            timezoneId = "Asia/Dhaka",
            utcOffsetSeconds = 21600,
            current = CurrentWeather(
                tempC = 31.2,
                feelsLikeC = 36.4,
                humidityPercent = 78,
                dewPointC = 26.8,
                windSpeedKmh = 14.0,
                windGustsKmh = 24.0,
                windDirectionDeg = 165,
                pressureHpa = 1006.0,
                pressureTrend = PressureTrend.FALLING,
                visibilityKm = 10.0,
                uvIndex = 7.0,
                cloudCoverPercent = 40,
                precipMm = 0.0,
                wmoCode = 2,
                isDay = true,
                capeJoulesPerKg = 1600.0,
                todayHighC = 33.0,
                todayLowC = 26.0
            ),
            hourly = listOf(
                HourlyForecast(
                    isoTime = "2026-11-17T14:00",
                    epochMillis = System.currentTimeMillis(),
                    tempC = 31.2,
                    feelsLikeC = 36.4,
                    precipProbPercent = 35,
                    precipMm = 0.4,
                    wmoCode = 2,
                    isDay = true,
                    windSpeedKmh = 14.0,
                    humidityPercent = 78,
                    uvIndex = 7.0,
                    cape = 1600.0
                )
            ),
            daily = listOf(
                DailyForecast(
                    isoDate = "2026-11-17",
                    minTempC = 26.0,
                    maxTempC = 33.0,
                    wmoCode = 2,
                    precipProbMaxPercent = 40,
                    precipSumMm = 2.0,
                    windSpeedMaxKmh = 16.0,
                    windGustsMaxKmh = 26.0,
                    uvIndexMax = 8.0,
                    sunriseIso = "2026-11-17T06:14",
                    sunsetIso = "2026-11-17T17:12",
                    daylightDurationSec = 39480.0,
                    humidityMeanPercent = 76
                )
            ),
            airQuality = AirQualityData(124, 65, 45.2, 78.0, 40.0, 22.0, 8.0, 410.0),
            astronomy = AstronomyData(
                sunriseIso = "2026-11-17T06:14",
                sunsetIso = "2026-11-17T17:12",
                daylightDurationSeconds = 39480.0,
                moonPhaseType = MoonPhaseType.FIRST_QUARTER,
                moonIlluminationPercent = 52,
                moonriseTime = "12:15",
                moonsetTime = "23:45"
            ),
            bangladeshAdvisory = BangladeshAdvisoryReport(
                isBangladeshOrTropicalContext = true,
                heatIndexC = 38.5,
                heatSeverity = AdvisorySeverity.ADVISORY,
                humiditySeverity = AdvisorySeverity.WARNING,
                stormSeverity = AdvisorySeverity.WARNING,
                maxCapeNext24h = 1600.0,
                heavyRainSeverity = AdvisorySeverity.NORMAL,
                next24hRainTotalMm = 2.0,
                coastalWindSeverity = AdvisorySeverity.NORMAL,
                peakWindGustKmh = 24.0,
                isMonsoonSeason = false
            ),
            lastUpdatedEpochMillis = System.currentTimeMillis()
        )

        composeRule.setContent {
            AtmosTheme {
                HomeScreen(
                    homeState = HomeUiState.Success(snapshot),
                    savedLocations = listOf(dhaka),
                    prefs = UserPreferences(),
                    hasLocationPermission = true,
                    onRefresh = {},
                    onUseGps = {},
                    onSelectLocation = {},
                    onPermissionResult = {},
                    onOpenMap = {},
                    onOpenLocations = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("location_switcher_button").assertIsDisplayed()
        composeRule.onNodeWithTag("hero_weather_block").assertIsDisplayed()
        composeRule.onNodeWithText("Dhaka").assertIsDisplayed()
        composeRule.onNodeWithTag("open_map_button").assertIsDisplayed()
        composeRule.onNodeWithTag("open_locations_button").assertIsDisplayed()
        composeRule.onNodeWithTag("open_settings_button").assertIsDisplayed()
    }
}
