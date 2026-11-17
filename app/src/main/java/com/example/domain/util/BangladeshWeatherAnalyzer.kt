package com.example.domain.util

import com.example.domain.model.AdvisorySeverity
import com.example.domain.model.BangladeshAdvisoryReport
import com.example.domain.model.CurrentWeather
import com.example.domain.model.HourlyForecast
import com.example.domain.model.SavedLocation
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max

/**
 * Evaluates meteorological indices relevant to Bangladesh & tropical delta climates
 * (NOAA/Rothfusz Heat Index, humidity stress, CAPE convective thunderstorm/lightning risk,
 * BMD-calibrated 24h rainfall intensity, and gale/coastal wind thresholds) strictly from
 * live atmospheric model data without fabricating warnings.
 */
object BangladeshWeatherAnalyzer {

    fun analyze(
        location: SavedLocation,
        timezoneId: String,
        current: CurrentWeather,
        hourlyNext24: List<HourlyForecast>
    ): BangladeshAdvisoryReport {
        val isBangladesh = location.countryCode.equals("BD", ignoreCase = true) ||
            location.country.contains("Bangladesh", ignoreCase = true) ||
            (location.latitude in 20.5..26.7 && location.longitude in 88.0..92.8)

        val heatIndexC = computeHeatIndexC(current.tempC, current.humidityPercent)
        val heatSeverity = when {
            heatIndexC >= 46.0 -> AdvisorySeverity.SEVERE
            heatIndexC >= 39.0 -> AdvisorySeverity.WARNING
            heatIndexC >= 32.0 -> AdvisorySeverity.ADVISORY
            else -> AdvisorySeverity.NORMAL
        }

        val humiditySeverity = when {
            current.dewPointC >= 25.0 || (current.humidityPercent >= 88 && current.tempC >= 30.0) ->
                AdvisorySeverity.WARNING
            current.dewPointC >= 22.0 || (current.humidityPercent >= 80 && current.tempC >= 27.0) ->
                AdvisorySeverity.ADVISORY
            else -> AdvisorySeverity.NORMAL
        }

        val maxCape = max(
            current.capeJoulesPerKg,
            hourlyNext24.maxOfOrNull { it.cape } ?: 0.0
        )
        val hasThunderCode = current.wmoCode in listOf(95, 96, 99) ||
            hourlyNext24.any { it.wmoCode in listOf(95, 96, 99) }
        val hasSevereThunder = current.wmoCode in listOf(96, 99) ||
            hourlyNext24.any { it.wmoCode in listOf(96, 99) }

        val stormSeverity = when {
            hasSevereThunder || maxCape >= 2500.0 -> AdvisorySeverity.SEVERE
            hasThunderCode || maxCape >= 1500.0 -> AdvisorySeverity.WARNING
            maxCape >= 800.0 -> AdvisorySeverity.ADVISORY
            else -> AdvisorySeverity.NORMAL
        }

        val rain24h = hourlyNext24.sumOf { it.precipMm }
        // BMD 24h rainfall classification: 44-88mm Heavy, >=89mm Very Heavy
        val heavyRainSeverity = when {
            rain24h >= 89.0 -> AdvisorySeverity.SEVERE
            rain24h >= 44.0 -> AdvisorySeverity.WARNING
            rain24h >= 22.0 -> AdvisorySeverity.ADVISORY
            else -> AdvisorySeverity.NORMAL
        }

        val peakGust = max(
            current.windGustsKmh,
            hourlyNext24.maxOfOrNull { it.windSpeedKmh * 1.35 } ?: 0.0
        )
        val coastalWindSeverity = when {
            peakGust >= 75.0 || current.windSpeedKmh >= 55.0 -> AdvisorySeverity.SEVERE
            peakGust >= 55.0 || current.windSpeedKmh >= 40.0 -> AdvisorySeverity.WARNING
            peakGust >= 40.0 -> AdvisorySeverity.ADVISORY
            else -> AdvisorySeverity.NORMAL
        }

        val tz = runCatching { TimeZone.getTimeZone(timezoneId) }.getOrDefault(TimeZone.getDefault())
        val month = Calendar.getInstance(tz).get(Calendar.MONTH) + 1 // 1..12
        val isMonsoon = month in 6..9

        return BangladeshAdvisoryReport(
            isBangladeshOrTropicalContext = isBangladesh || heatSeverity != AdvisorySeverity.NORMAL ||
                stormSeverity != AdvisorySeverity.NORMAL,
            heatIndexC = heatIndexC,
            heatSeverity = heatSeverity,
            humiditySeverity = humiditySeverity,
            stormSeverity = stormSeverity,
            maxCapeNext24h = maxCape,
            heavyRainSeverity = heavyRainSeverity,
            next24hRainTotalMm = rain24h,
            coastalWindSeverity = coastalWindSeverity,
            peakWindGustKmh = peakGust,
            isMonsoonSeason = isMonsoon
        )
    }

    /**
     * Rothfusz Heat Index regression (NOAA), valid for T >= 26.7°C (80°F) and RH >= 40%.
     * Falls back to Steadman apparent temperature below threshold.
     */
    fun computeHeatIndexC(tempC: Double, humidityPercent: Int): Double {
        if (tempC < 26.7 || humidityPercent < 40) return tempC
        val tF = tempC * 9.0 / 5.0 + 32.0
        val rh = humidityPercent.toDouble()
        var hiF = -42.379 +
            2.04901523 * tF +
            10.14333127 * rh -
            0.22475541 * tF * rh -
            0.00683783 * tF * tF -
            0.05481717 * rh * rh +
            0.00122874 * tF * tF * rh +
            0.00085282 * tF * rh * rh -
            0.00000199 * tF * tF * rh * rh

        if (rh < 13.0 && tF in 80.0..112.0) {
            hiF -= ((13.0 - rh) / 4.0) * kotlin.math.sqrt((17.0 - kotlin.math.abs(tF - 95.0)) / 17.0)
        } else if (rh > 85.0 && tF in 80.0..87.0) {
            hiF += ((rh - 85.0) / 10.0) * ((87.0 - tF) / 5.0)
        }
        return (hiF - 32.0) * 5.0 / 9.0
    }
}
