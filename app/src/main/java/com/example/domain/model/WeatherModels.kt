package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
enum class WindSpeedUnit { KMH, MS, MPH, KNOTS }
enum class PrecipitationUnit { MM, INCH }
enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    BANGLA("bn")
}

data class UserPreferences(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.KMH,
    val precipitationUnit: PrecipitationUnit = PrecipitationUnit.MM,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val notificationsEnabled: Boolean = true,
    val rainAlertsEnabled: Boolean = true,
    val severeAlertsEnabled: Boolean = true,
    val syncIntervalHours: Int = 3,
    val selectedLocationId: String = "bd_dhaka"
)

enum class WeatherCondition {
    CLEAR,
    MOSTLY_CLEAR,
    PARTLY_CLOUDY,
    OVERCAST,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    THUNDERSTORM,
    SNOW,
    SQUALL;

    companion object {
        fun fromWmoCode(code: Int, windGustKmh: Double = 0.0): WeatherCondition = when (code) {
            0 -> CLEAR
            1 -> MOSTLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 80, 81 -> RAIN
            65, 66, 67, 82 -> HEAVY_RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> THUNDERSTORM
            else -> if (windGustKmh >= 60.0) SQUALL else PARTLY_CLOUDY
        }
    }
}

enum class PressureTrend { RISING, FALLING, STEADY }

enum class AqiCategory {
    GOOD, MODERATE, SENSITIVE, UNHEALTHY, VERY_UNHEALTHY, HAZARDOUS;

    companion object {
        fun fromUsAqi(aqi: Int): AqiCategory = when {
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> SENSITIVE
            aqi <= 200 -> UNHEALTHY
            aqi <= 300 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }
    }
}

enum class MoonPhaseType {
    NEW_MOON,
    WAXING_CRESCENT,
    FIRST_QUARTER,
    WAXING_GIBBOUS,
    FULL_MOON,
    WANING_GIBBOUS,
    LAST_QUARTER,
    WANING_CRESCENT
}

enum class AdvisorySeverity { NORMAL, ADVISORY, WARNING, SEVERE }

@JsonClass(generateAdapter = true)
data class SavedLocation(
    val id: String,
    val name: String,
    val admin1: String = "",
    val country: String = "",
    val countryCode: String = "",
    val latitude: Double,
    val longitude: Double,
    val timezone: String = "auto",
    val isCurrentLocation: Boolean = false,
    val sortOrder: Int = 0,
    val lastTempC: Double? = null,
    val lastConditionCode: Int? = null,
    val lastIsDay: Boolean = true,
    val lastHighC: Double? = null,
    val lastLowC: Double? = null,
    val lastHumidity: Int? = null,
    val lastWindKmh: Double? = null,
    val lastPrecipProb: Int? = null,
    val lastAqi: Int? = null,
    val lastUv: Double? = null,
    val lastUpdatedEpochMillis: Long = 0L
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    val tempC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val dewPointC: Double,
    val windSpeedKmh: Double,
    val windGustsKmh: Double,
    val windDirectionDeg: Int,
    val pressureHpa: Double,
    val pressureTrend: PressureTrend,
    val visibilityKm: Double,
    val uvIndex: Double,
    val cloudCoverPercent: Int,
    val precipMm: Double,
    val wmoCode: Int,
    val isDay: Boolean,
    val capeJoulesPerKg: Double,
    val todayHighC: Double,
    val todayLowC: Double
) {
    val condition: WeatherCondition
        get() = WeatherCondition.fromWmoCode(wmoCode, windGustsKmh)
}

@JsonClass(generateAdapter = true)
data class HourlyForecast(
    val isoTime: String,
    val epochMillis: Long,
    val tempC: Double,
    val feelsLikeC: Double,
    val precipProbPercent: Int,
    val precipMm: Double,
    val wmoCode: Int,
    val isDay: Boolean,
    val windSpeedKmh: Double,
    val humidityPercent: Int,
    val uvIndex: Double,
    val cape: Double
) {
    val condition: WeatherCondition
        get() = WeatherCondition.fromWmoCode(wmoCode)
}

@JsonClass(generateAdapter = true)
data class DailyForecast(
    val isoDate: String,
    val minTempC: Double,
    val maxTempC: Double,
    val wmoCode: Int,
    val precipProbMaxPercent: Int,
    val precipSumMm: Double,
    val windSpeedMaxKmh: Double,
    val windGustsMaxKmh: Double,
    val uvIndexMax: Double,
    val sunriseIso: String,
    val sunsetIso: String,
    val daylightDurationSec: Double,
    val humidityMeanPercent: Int
) {
    val condition: WeatherCondition
        get() = WeatherCondition.fromWmoCode(wmoCode, windGustsMaxKmh)
}

@JsonClass(generateAdapter = true)
data class AirQualityData(
    val usAqi: Int,
    val europeanAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val ozone: Double,
    val nitrogenDioxide: Double,
    val sulphurDioxide: Double,
    val carbonMonoxide: Double
) {
    val category: AqiCategory
        get() = AqiCategory.fromUsAqi(usAqi)
}

@JsonClass(generateAdapter = true)
data class AstronomyData(
    val sunriseIso: String,
    val sunsetIso: String,
    val daylightDurationSeconds: Double,
    val moonPhaseType: MoonPhaseType,
    val moonIlluminationPercent: Int,
    val moonriseTime: String,
    val moonsetTime: String
)

@JsonClass(generateAdapter = true)
data class BangladeshAdvisoryReport(
    val isBangladeshOrTropicalContext: Boolean,
    val heatIndexC: Double,
    val heatSeverity: AdvisorySeverity,
    val humiditySeverity: AdvisorySeverity,
    val stormSeverity: AdvisorySeverity,
    val maxCapeNext24h: Double,
    val heavyRainSeverity: AdvisorySeverity,
    val next24hRainTotalMm: Double,
    val coastalWindSeverity: AdvisorySeverity,
    val peakWindGustKmh: Double,
    val isMonsoonSeason: Boolean
)

@JsonClass(generateAdapter = true)
data class WeatherSnapshot(
    val location: SavedLocation,
    val timezoneId: String,
    val utcOffsetSeconds: Int,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val airQuality: AirQualityData?,
    val astronomy: AstronomyData,
    val bangladeshAdvisory: BangladeshAdvisoryReport,
    val lastUpdatedEpochMillis: Long,
    val isFromCache: Boolean = false,
    val providerName: String = "Open-Meteo"
)

data class GridWeatherPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String?,
    val tempC: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val cloudCoverPercent: Int,
    val precipMm: Double,
    val wmoCode: Int
)

enum class WeatherMapLayer {
    PRECIPITATION, TEMPERATURE, CLOUDS, WIND
}
