package com.example.domain.util

import java.util.Calendar
import java.util.TimeZone

/** Which solar event the reported interval refers to. */
enum class SunEvent { SUNRISE, SUNSET }

/**
 * Where the sun is relative to today's daylight window.
 *
 * [progress] is 0f at sunrise and 1f at sunset; it is null while the sun is below the
 * horizon, which is what the daylight arc uses to park a hollow marker on the correct
 * end of the arc instead of drawing the sun in the middle of the sky.
 */
data class SunPosition(
    val progress: Float?,
    val isDaytime: Boolean,
    val beforeSunrise: Boolean,
    val event: SunEvent,
    /** Absolute distance between [event] and now, in milliseconds. */
    val millisFromEvent: Long
)

/**
 * Computes the sun's position on the daylight arc from the current time and the
 * sunrise / sunset ISO timestamps Open-Meteo returns for the location's timezone.
 */
object SunPositionCalculator {

    fun compute(
        nowMillis: Long,
        sunriseIso: String,
        sunsetIso: String,
        timezoneId: String
    ): SunPosition {
        val timezone = runCatching { TimeZone.getTimeZone(timezoneId) }
            .getOrDefault(TimeZone.getDefault())

        val sunrise = parseLocalIso(sunriseIso, timezone)
        val sunset = parseLocalIso(sunsetIso, timezone)

        if (sunrise == null || sunset == null || sunset <= sunrise) {
            // Unparseable input: keep the marker parked at the end of the arc.
            return SunPosition(
                progress = null,
                isDaytime = false,
                beforeSunrise = false,
                event = SunEvent.SUNSET,
                millisFromEvent = 0L
            )
        }

        return when {
            nowMillis < sunrise -> SunPosition(
                progress = null,
                isDaytime = false,
                beforeSunrise = true,
                event = SunEvent.SUNRISE,
                millisFromEvent = sunrise - nowMillis
            )

            nowMillis > sunset -> SunPosition(
                progress = null,
                isDaytime = false,
                beforeSunrise = false,
                event = SunEvent.SUNSET,
                millisFromEvent = nowMillis - sunset
            )

            else -> SunPosition(
                progress = (nowMillis - sunrise).toFloat() / (sunset - sunrise).toFloat(),
                isDaytime = true,
                beforeSunrise = false,
                event = SunEvent.SUNSET,
                millisFromEvent = sunset - nowMillis
            )
        }
    }

    /** Parses Open-Meteo's zone-less "2026-11-17T06:14" in the given timezone. */
    private fun parseLocalIso(iso: String, timezone: TimeZone): Long? {
        val datePart = iso.substringBefore('T', iso)
        val timePart = iso.substringAfter('T', "")
        if (timePart.isBlank()) return null

        val dateParts = datePart.split("-")
        if (dateParts.size != 3) return null
        val year = dateParts[0].toIntOrNull() ?: return null
        val month = dateParts[1].toIntOrNull() ?: return null
        val day = dateParts[2].toIntOrNull() ?: return null

        val timeParts = timePart.split(":")
        if (timeParts.size < 2) return null
        val hour = timeParts[0].take(2).toIntOrNull() ?: return null
        val minute = timeParts[1].take(2).toIntOrNull() ?: return null

        return Calendar.getInstance(timezone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis
    }
}
