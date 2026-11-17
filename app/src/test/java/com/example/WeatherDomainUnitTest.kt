package com.example

import com.example.domain.model.AdvisorySeverity
import com.example.domain.model.CurrentWeather
import com.example.domain.model.HourlyForecast
import com.example.domain.model.PressureTrend
import com.example.domain.model.SavedLocation
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WindSpeedUnit
import com.example.domain.util.AstronomyCalculator
import com.example.domain.util.BangladeshWeatherAnalyzer
import com.example.domain.util.UnitFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDomainUnitTest {

    @Test
    fun `heat index rises significantly under high Bangladesh temperature and humidity`() {
        val hi = BangladeshWeatherAnalyzer.computeHeatIndexC(tempC = 35.0, humidityPercent = 75)
        assertTrue("Expected heat index > 44C, got $hi", hi > 44.0)
    }

    @Test
    fun `bangladesh analyzer flags storm and heavy rain risks from real atmospheric thresholds`() {
        val dhaka = SavedLocation(
            id = "bd_dhaka",
            name = "Dhaka",
            country = "Bangladesh",
            countryCode = "BD",
            latitude = 23.81,
            longitude = 90.41
        )
        val current = CurrentWeather(
            tempC = 33.0,
            feelsLikeC = 39.0,
            humidityPercent = 84,
            dewPointC = 26.5,
            windSpeedKmh = 28.0,
            windGustsKmh = 62.0,
            windDirectionDeg = 180,
            pressureHpa = 1002.0,
            pressureTrend = PressureTrend.FALLING,
            visibilityKm = 6.0,
            uvIndex = 6.0,
            cloudCoverPercent = 85,
            precipMm = 12.0,
            wmoCode = 95,
            isDay = true,
            capeJoulesPerKg = 2200.0,
            todayHighC = 34.0,
            todayLowC = 26.0
        )
        val hourly = List(6) { idx ->
            HourlyForecast(
                isoTime = "2026-06-15T1${idx}:00",
                epochMillis = 1_780_000_000_000L + idx * 3600_000L,
                tempC = 31.0,
                feelsLikeC = 37.0,
                precipProbPercent = 90,
                precipMm = 10.0,
                wmoCode = 95,
                isDay = true,
                windSpeedKmh = 30.0,
                humidityPercent = 85,
                uvIndex = 4.0,
                cape = 2200.0
            )
        }

        val report = BangladeshWeatherAnalyzer.analyze(dhaka, "Asia/Dhaka", current, hourly)
        assertTrue(report.isBangladeshOrTropicalContext)
        assertEquals(AdvisorySeverity.WARNING, report.stormSeverity)
        assertEquals(AdvisorySeverity.WARNING, report.heavyRainSeverity) // 60 mm total
        assertEquals(AdvisorySeverity.WARNING, report.coastalWindSeverity) // 62 km/h gusts
    }

    @Test
    fun `astronomy calculator computes valid illumination and moon times`() {
        val astro = AstronomyCalculator.calculate(
            latitude = 23.8103,
            longitude = 90.4125,
            timezoneId = "Asia/Dhaka",
            sunriseIso = "2026-11-17T06:14",
            sunsetIso = "2026-11-17T17:12",
            daylightDurationSec = 39480.0
        )
        assertTrue(astro.moonIlluminationPercent in 0..100)
        assertTrue(astro.moonriseTime.contains(":"))
        assertTrue(astro.moonsetTime.contains(":"))
    }

    @Test
    fun `unit formatter converts celsius to fahrenheit and wind units accurately`() {
        assertEquals("77°F", UnitFormatter.formatTemp(25.0, TemperatureUnit.FAHRENHEIT, true))
        assertEquals("10.0 m/s", UnitFormatter.formatWind(36.0, WindSpeedUnit.MS))
        assertEquals("S", UnitFormatter.compassDirection(180, isBangla = false))
    }
}
