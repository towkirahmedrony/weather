package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String = "UTC",
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val current: CurrentDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentDto(
    val time: String = "",
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int = 0,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1,
    val precipitation: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "cloud_cover") val cloudCover: Int = 0,
    @Json(name = "surface_pressure") val surfacePressure: Double = 1013.0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
    @Json(name = "wind_direction_10m") val windDirection10m: Int = 0,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double = 0.0,
    @Json(name = "dew_point_2m") val dewPoint2m: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyDto(
    val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int?> = emptyList(),
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
    @Json(name = "surface_pressure") val surfacePressure: List<Double?> = emptyList(),
    @Json(name = "cloud_cover") val cloudCover: List<Int?> = emptyList(),
    val visibility: List<Double?> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @Json(name = "uv_index") val uvIndex: List<Double?> = emptyList(),
    @Json(name = "is_day") val isDay: List<Int?> = emptyList(),
    val cape: List<Double?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyDto(
    val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @Json(name = "daylight_duration") val daylightDuration: List<Double?> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @Json(name = "precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double?> = emptyList(),
    @Json(name = "wind_gusts_10m_max") val windGusts10mMax: List<Double?> = emptyList(),
    @Json(name = "relative_humidity_2m_mean") val relativeHumidity2mMean: List<Int?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OpenMeteoGridResponseItem(
    val latitude: Double,
    val longitude: Double,
    val current: GridCurrentDto? = null
)

@JsonClass(generateAdapter = true)
data class GridCurrentDto(
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
    @Json(name = "wind_direction_10m") val windDirection10m: Int = 0,
    @Json(name = "cloud_cover") val cloudCover: Int = 0,
    val precipitation: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0
)

@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityResponse(
    val latitude: Double,
    val longitude: Double,
    val current: AirQualityCurrentDto? = null
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrentDto(
    @Json(name = "european_aqi") val europeanAqi: Int? = null,
    @Json(name = "us_aqi") val usAqi: Int? = null,
    val pm10: Double? = null,
    @Json(name = "pm2_5") val pm25: Double? = null,
    @Json(name = "carbon_monoxide") val carbonMonoxide: Double? = null,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @Json(name = "sulphur_dioxide") val sulphurDioxide: Double? = null,
    val ozone: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResultDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val timezone: String? = null
)

@JsonClass(generateAdapter = true)
data class RainViewerMapsResponse(
    val host: String = "https://tilecache.rainviewer.com",
    val radar: RainViewerRadarDto? = null
)

@JsonClass(generateAdapter = true)
data class RainViewerRadarDto(
    val past: List<RainViewerFrameDto> = emptyList(),
    val nowcast: List<RainViewerFrameDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RainViewerFrameDto(
    val time: Long,
    val path: String
)

@JsonClass(generateAdapter = true)
data class NominatimReverseResponse(
    val name: String? = null,
    val address: NominatimAddressDto? = null
)

@JsonClass(generateAdapter = true)
data class NominatimAddressDto(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null
)
