package com.example.domain.util

import com.example.domain.model.AstronomyData
import com.example.domain.model.MoonPhaseType
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes lunar phase, illumination percentage, and approximate local moonrise/moonset
 * using standard Meeus/astronomical Julian-date synodic ephemeris algorithms.
 */
object AstronomyCalculator {

    private const val SYNODIC_MONTH = 29.53058867
    // Known New Moon reference: 2000-01-06 18:14 UTC (JD 2451550.25972)
    private const val REF_NEW_MOON_JD = 2451550.25972

    fun calculate(
        latitude: Double,
        longitude: Double,
        timezoneId: String,
        sunriseIso: String,
        sunsetIso: String,
        daylightDurationSec: Double,
        nowMillis: Long = System.currentTimeMillis()
    ): AstronomyData {
        val tz = runCatching { TimeZone.getTimeZone(timezoneId) }.getOrDefault(TimeZone.getDefault())
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }

        val jd = millisToJulianDay(nowMillis)
        val daysSinceRef = jd - REF_NEW_MOON_JD
        val lunations = daysSinceRef / SYNODIC_MONTH
        val phaseFraction = (lunations - floor(lunations)).let { if (it < 0) it + 1.0 else it }

        val illumination = ((1.0 - cos(phaseFraction * 2.0 * PI)) / 2.0 * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

        val phaseType = when {
            phaseFraction < 0.0625 || phaseFraction >= 0.9375 -> MoonPhaseType.NEW_MOON
            phaseFraction < 0.1875 -> MoonPhaseType.WAXING_CRESCENT
            phaseFraction < 0.3125 -> MoonPhaseType.FIRST_QUARTER
            phaseFraction < 0.4375 -> MoonPhaseType.WAXING_GIBBOUS
            phaseFraction < 0.5625 -> MoonPhaseType.FULL_MOON
            phaseFraction < 0.6875 -> MoonPhaseType.WANING_GIBBOUS
            phaseFraction < 0.8125 -> MoonPhaseType.LAST_QUARTER
            else -> MoonPhaseType.WANING_CRESCENT
        }

        val (moonrise, moonset) = estimateMoonRiseSet(
            cal = cal,
            latitude = latitude,
            longitude = longitude,
            phaseFraction = phaseFraction
        )

        return AstronomyData(
            sunriseIso = sunriseIso,
            sunsetIso = sunsetIso,
            daylightDurationSeconds = daylightDurationSec,
            moonPhaseType = phaseType,
            moonIlluminationPercent = illumination,
            moonriseTime = moonrise,
            moonsetTime = moonset
        )
    }

    private fun millisToJulianDay(millis: Long): Double {
        // Unix epoch 1970-01-01 00:00:00 UTC = JD 2440587.5
        return millis / 86_400_000.0 + 2440587.5
    }

    /**
     * Estimates local moonrise & moonset times based on lunar phase lag behind the Sun
     * (~48.8 min/day, or phaseFraction * 24h) combined with seasonal declination shift.
     */
    private fun estimateMoonRiseSet(
        cal: Calendar,
        latitude: Double,
        longitude: Double,
        phaseFraction: Double
    ): Pair<String, String> {
        val tzOffsetHours = cal.timeZone.getOffset(cal.timeInMillis) / 3_600_000.0
        val solarNoonLocal = 12.0 + tzOffsetHours - (longitude / 15.0)
        // Moon transits at solarNoon + phaseFraction * 24h (wrapped 0..24)
        val moonTransitHours = (solarNoonLocal + phaseFraction * 24.0).mod(24.0)

        // Approximate half-day arc of moon depending on latitude and lunar ecliptic longitude
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val sunLngRad = (2.0 * PI * (dayOfYear - 80) / 365.25)
        val moonLngRad = sunLngRad + phaseFraction * 2.0 * PI
        val moonDeclRad = 0.409 * sin(moonLngRad) // ~23.4 deg obliquity
        val latRad = Math.toRadians(latitude.coerceIn(-65.0, 65.0))
        val cosH0 = (-sin(latRad) * sin(moonDeclRad)) / (cos(latRad) * cos(moonDeclRad) + 1e-9)
        val clampedCos = cosH0.coerceIn(-0.98, 0.98)
        val halfArcHours = Math.toDegrees(atan2(sqrt(1.0 - clampedCos * clampedCos), clampedCos)) / 15.0

        val riseHours = (moonTransitHours - halfArcHours).mod(24.0)
        val setHours = (moonTransitHours + halfArcHours).mod(24.0)

        return formatHourMinute(riseHours) to formatHourMinute(setHours)
    }

    private fun formatHourMinute(decimalHours: Double): String {
        val totalMinutes = (decimalHours * 60.0).roundToInt().mod(1440)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return "%02d:%02d".format(h, m)
    }

    private fun Double.mod(other: Double): Double {
        val r = this % other
        return if (r < 0) r + other else r
    }
}
