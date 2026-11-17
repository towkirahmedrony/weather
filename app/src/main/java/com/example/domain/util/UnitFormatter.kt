package com.example.domain.util

import android.content.Context
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.AqiCategory
import com.example.domain.model.MoonPhaseType
import com.example.domain.model.PrecipitationUnit
import com.example.domain.model.PressureTrend
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WindSpeedUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object UnitFormatter {

    fun formatTemp(tempC: Double, unit: TemperatureUnit, includeUnit: Boolean = false): String {
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> tempC.roundToInt()
            TemperatureUnit.FAHRENHEIT -> (tempC * 9.0 / 5.0 + 32.0).roundToInt()
        }
        return if (includeUnit) {
            "$value°${if (unit == TemperatureUnit.CELSIUS) "C" else "F"}"
        } else {
            "$value°"
        }
    }

    fun convertTempValue(tempC: Double, unit: TemperatureUnit): Double = when (unit) {
        TemperatureUnit.CELSIUS -> tempC
        TemperatureUnit.FAHRENHEIT -> tempC * 9.0 / 5.0 + 32.0
    }

    fun formatWind(speedKmh: Double, unit: WindSpeedUnit): String = when (unit) {
        WindSpeedUnit.KMH -> "${speedKmh.roundToInt()} km/h"
        WindSpeedUnit.MS -> "${"%.1f".format(Locale.US, speedKmh / 3.6)} m/s"
        WindSpeedUnit.MPH -> "${(speedKmh * 0.621371).roundToInt()} mph"
        WindSpeedUnit.KNOTS -> "${(speedKmh * 0.539957).roundToInt()} kt"
    }

    fun formatPrecip(mm: Double, unit: PrecipitationUnit): String = when (unit) {
        PrecipitationUnit.MM -> "${"%.1f".format(Locale.US, mm)} mm"
        PrecipitationUnit.INCH -> "${"%.2f".format(Locale.US, mm / 25.4)} in"
    }

    fun compassDirection(degrees: Int, isBangla: Boolean = false): String {
        val en = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val bn = listOf("উঃ", "উঃ-পূঃ", "পূঃ", "দঃ-পূঃ", "দঃ", "দঃ-পঃ", "পঃ", "উঃ-পঃ")
        val index = (((degrees % 360) + 360) % 360 + 22.5).toInt() / 45 % 8
        return if (isBangla) bn[index] else en[index]
    }

    fun formatHourLabel(isoTime: String, language: AppLanguage): String {
        // isoTime format from Open-Meteo: "2026-11-17T14:00"
        val timePart = isoTime.substringAfter("T", isoTime)
        val hour = timePart.substringBefore(":").toIntOrNull() ?: return timePart
        val isBn = language == AppLanguage.BANGLA
        return when {
            hour == 0 -> if (isBn) "১২ রাত" else "12 AM"
            hour < 12 -> if (isBn) "$hour সকাল" else "$hour AM"
            hour == 12 -> if (isBn) "১২ দুপুর" else "12 PM"
            else -> if (isBn) "${hour - 12} বিকেল" else "${hour - 12} PM"
        }
    }

    fun formatTime12h(isoOrTime: String): String {
        val raw = if (isoOrTime.contains("T")) isoOrTime.substringAfter("T") else isoOrTime
        val parts = raw.split(":")
        if (parts.size < 2) return raw
        val h = parts[0].toIntOrNull() ?: return raw
        val m = parts[1].take(2).toIntOrNull() ?: return raw
        val suffix = if (h >= 12) "PM" else "AM"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "%d:%02d %s".format(h12, m, suffix)
    }

    fun formatDayName(isoDate: String, index: Int, context: Context, language: AppLanguage): String {
        if (index == 0) return context.getString(R.string.daily_today)
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(isoDate) ?: return isoDate
            val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.ENGLISH
            SimpleDateFormat("EEE", locale).format(date)
        }.getOrDefault(isoDate)
    }

    fun formatDaylightDuration(seconds: Double, isBangla: Boolean): String {
        val totalMinutes = (seconds / 60.0).roundToInt().coerceAtLeast(0)
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (isBangla) "${h}ঘ ${m}মি" else "${h}h ${m}m"
    }

    fun formatUpdatedAgo(epochMillis: Long, context: Context): String {
        if (epochMillis <= 0L) return ""
        val diffMin = ((System.currentTimeMillis() - epochMillis) / 60_000L).coerceAtLeast(0)
        return when {
            diffMin < 1 -> context.getString(R.string.status_updated_just_now)
            diffMin < 60 -> context.getString(R.string.status_updated_mins_ago, diffMin.toInt())
            else -> context.getString(R.string.status_updated_hours_ago, (diffMin / 60).toInt())
        }
    }

    fun formatLocalClock(timezoneId: String, language: AppLanguage): String {
        return runCatching {
            val tz = TimeZone.getTimeZone(timezoneId)
            val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.ENGLISH
            SimpleDateFormat("h:mm a", locale).apply { timeZone = tz }.format(Date())
        }.getOrDefault("")
    }

    fun conditionLabel(condition: WeatherCondition, context: Context): String = when (condition) {
        WeatherCondition.CLEAR -> context.getString(R.string.cond_clear)
        WeatherCondition.MOSTLY_CLEAR -> context.getString(R.string.cond_mostly_clear)
        WeatherCondition.PARTLY_CLOUDY -> context.getString(R.string.cond_partly_cloudy)
        WeatherCondition.OVERCAST -> context.getString(R.string.cond_overcast)
        WeatherCondition.FOG -> context.getString(R.string.cond_fog)
        WeatherCondition.DRIZZLE -> context.getString(R.string.cond_drizzle)
        WeatherCondition.RAIN -> context.getString(R.string.cond_rain)
        WeatherCondition.HEAVY_RAIN -> context.getString(R.string.cond_heavy_rain)
        WeatherCondition.THUNDERSTORM -> context.getString(R.string.cond_thunderstorm)
        WeatherCondition.SNOW -> context.getString(R.string.cond_snow)
        WeatherCondition.SQUALL -> context.getString(R.string.cond_squall)
    }

    fun aqiCategoryLabel(cat: AqiCategory, context: Context): String = when (cat) {
        AqiCategory.GOOD -> context.getString(R.string.aqi_good)
        AqiCategory.MODERATE -> context.getString(R.string.aqi_moderate)
        AqiCategory.SENSITIVE -> context.getString(R.string.aqi_sensitive)
        AqiCategory.UNHEALTHY -> context.getString(R.string.aqi_unhealthy)
        AqiCategory.VERY_UNHEALTHY -> context.getString(R.string.aqi_very_unhealthy)
        AqiCategory.HAZARDOUS -> context.getString(R.string.aqi_hazardous)
    }

    fun aqiAdvice(cat: AqiCategory, context: Context): String = when (cat) {
        AqiCategory.GOOD -> context.getString(R.string.aqi_advice_good)
        AqiCategory.MODERATE -> context.getString(R.string.aqi_advice_moderate)
        AqiCategory.SENSITIVE -> context.getString(R.string.aqi_advice_sensitive)
        AqiCategory.UNHEALTHY -> context.getString(R.string.aqi_advice_unhealthy)
        AqiCategory.VERY_UNHEALTHY -> context.getString(R.string.aqi_advice_very_unhealthy)
        AqiCategory.HAZARDOUS -> context.getString(R.string.aqi_advice_hazardous)
    }

    fun moonPhaseLabel(phase: MoonPhaseType, context: Context): String = when (phase) {
        MoonPhaseType.NEW_MOON -> context.getString(R.string.moon_new)
        MoonPhaseType.WAXING_CRESCENT -> context.getString(R.string.moon_waxing_crescent)
        MoonPhaseType.FIRST_QUARTER -> context.getString(R.string.moon_first_quarter)
        MoonPhaseType.WAXING_GIBBOUS -> context.getString(R.string.moon_waxing_gibbous)
        MoonPhaseType.FULL_MOON -> context.getString(R.string.moon_full)
        MoonPhaseType.WANING_GIBBOUS -> context.getString(R.string.moon_waning_gibbous)
        MoonPhaseType.LAST_QUARTER -> context.getString(R.string.moon_last_quarter)
        MoonPhaseType.WANING_CRESCENT -> context.getString(R.string.moon_waning_crescent)
    }

    fun pressureTrendLabel(trend: PressureTrend, context: Context): String = when (trend) {
        PressureTrend.RISING -> context.getString(R.string.pressure_rising)
        PressureTrend.FALLING -> context.getString(R.string.pressure_falling)
        PressureTrend.STEADY -> context.getString(R.string.pressure_steady)
    }

    fun buildTodaySummary(
        conditionLabel: String,
        highC: Double,
        lowC: Double,
        feelsLikeC: Double,
        windKmh: Double,
        peakPrecipProb: Int,
        prefs: UserPreferences
    ): String {
        val hi = formatTemp(highC, prefs.temperatureUnit)
        val lo = formatTemp(lowC, prefs.temperatureUnit)
        val wind = formatWind(windKmh, prefs.windSpeedUnit)
        val isBn = prefs.language == AppLanguage.BANGLA
        return if (isBn) {
            val rainNote = if (peakPrecipProb >= 35) "বৃষ্টির সম্ভাবনা $peakPrecipProb% পর্যন্ত।" else "বৃষ্টির সম্ভাবনা কম।"
            "$conditionLabel — আজ সর্বোচ্চ $hi এবং সর্বনিম্ন $lo। বাতাস $wind। $rainNote"
        } else {
            val rainNote = if (peakPrecipProb >= 35) "Rain chances peak around $peakPrecipProb%." else "Minimal precipitation expected."
            "$conditionLabel today with a high of $hi and low near $lo. Winds around $wind. $rainNote"
        }
    }
}
