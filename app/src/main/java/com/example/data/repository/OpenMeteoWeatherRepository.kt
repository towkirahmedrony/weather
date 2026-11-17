package com.example.data.repository

import com.example.data.local.SavedLocationEntity
import com.example.data.local.WeatherCacheEntity
import com.example.data.local.WeatherDao
import com.example.data.location.LocationTracker
import com.example.data.remote.NominatimApi
import com.example.data.remote.OpenMeteoAirQualityApi
import com.example.data.remote.OpenMeteoForecastApi
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.data.remote.OpenMeteoGeocodingApi
import com.example.data.remote.RainViewerApi
import com.example.domain.model.AirQualityData
import com.example.domain.model.CurrentWeather
import com.example.domain.model.DailyForecast
import com.example.domain.model.GridWeatherPoint
import com.example.domain.model.HourlyForecast
import com.example.domain.model.PressureTrend
import com.example.domain.model.SavedLocation
import com.example.domain.model.WeatherSnapshot
import com.example.domain.util.AstronomyCalculator
import com.example.domain.util.BangladeshWeatherAnalyzer
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class OpenMeteoWeatherRepository(
    private val forecastApi: OpenMeteoForecastApi,
    private val airQualityApi: OpenMeteoAirQualityApi,
    private val geocodingApi: OpenMeteoGeocodingApi,
    private val rainViewerApi: RainViewerApi,
    private val nominatimApi: NominatimApi,
    private val weatherDao: WeatherDao,
    private val locationTracker: LocationTracker,
    moshi: Moshi
) : WeatherRepository {

    private val snapshotAdapter = moshi.adapter(WeatherSnapshot::class.java)

    // Rate-limit & freshness controls
    private val freshCacheWindowMillis = 10 * 60 * 1000L // 10 minutes
    private var rateLimitCooldownUntilMillis = 0L

    override fun observeSavedLocations(): Flow<List<SavedLocation>> =
        weatherDao.observeSavedLocations().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureDefaultLocationsSeeded() = withContext(Dispatchers.IO) {
        val existing = weatherDao.getSavedLocationsOnce()
        if (existing.isEmpty()) {
            val seeds = listOf(
                SavedLocation(
                    id = "bd_dhaka",
                    name = "Dhaka",
                    admin1 = "Dhaka Division",
                    country = "Bangladesh",
                    countryCode = "BD",
                    latitude = 23.8103,
                    longitude = 90.4125,
                    timezone = "Asia/Dhaka",
                    sortOrder = 0
                ),
                SavedLocation(
                    id = "bd_chattogram",
                    name = "Chattogram",
                    admin1 = "Chattogram Division",
                    country = "Bangladesh",
                    countryCode = "BD",
                    latitude = 22.3569,
                    longitude = 91.7832,
                    timezone = "Asia/Dhaka",
                    sortOrder = 1
                ),
                SavedLocation(
                    id = "bd_sylhet",
                    name = "Sylhet",
                    admin1 = "Sylhet Division",
                    country = "Bangladesh",
                    countryCode = "BD",
                    latitude = 24.8949,
                    longitude = 91.8687,
                    timezone = "Asia/Dhaka",
                    sortOrder = 2
                )
            )
            weatherDao.upsertLocations(seeds.map { SavedLocationEntity.fromDomain(it) })
        }
    }

    override suspend fun getCachedWeather(locationId: String): WeatherSnapshot? = withContext(Dispatchers.IO) {
        val cached = weatherDao.getCachedWeather(locationId) ?: return@withContext null
        runCatching { snapshotAdapter.fromJson(cached.snapshotJson) }
            .getOrNull()
            ?.copy(isFromCache = true)
    }

    override suspend fun getLocationWeather(
        location: SavedLocation,
        forceRefresh: Boolean
    ): WeatherResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cachedSnapshot = getCachedWeather(location.id)

        // Return fresh cache when within 10-minute window and not force-refreshed
        if (!forceRefresh && cachedSnapshot != null &&
            (now - cachedSnapshot.lastUpdatedEpochMillis) < freshCacheWindowMillis
        ) {
            return@withContext WeatherResult.Success(cachedSnapshot.copy(isFromCache = false))
        }

        // Respect HTTP 429 rate-limit backoff window
        if (now < rateLimitCooldownUntilMillis) {
            return@withContext if (cachedSnapshot != null) {
                WeatherResult.Success(cachedSnapshot.copy(isFromCache = true))
            } else {
                WeatherResult.Error(
                    message = "Weather provider rate limit reached. Please wait a moment.",
                    isRateLimited = true
                )
            }
        }

        return@withContext try {
            val snapshot = coroutineScope {
                val forecastDeferred = async {
                    forecastApi.getForecast(location.latitude, location.longitude)
                }
                val aqiDeferred = async {
                    runCatching {
                        airQualityApi.getAirQuality(location.latitude, location.longitude)
                    }.getOrNull()
                }
                val forecast = forecastDeferred.await()
                val aqi = aqiDeferred.await()
                mapToSnapshot(location, forecast, aqi?.current, now)
            }

            // Persist snapshot JSON to Room for offline access
            val json = snapshotAdapter.toJson(snapshot)
            weatherDao.upsertWeatherCache(
                WeatherCacheEntity(
                    locationId = location.id,
                    snapshotJson = json,
                    updatedAtEpochMillis = now
                )
            )

            // Update saved location summary fields
            val updatedLocation = location.copy(
                timezone = snapshot.timezoneId,
                lastTempC = snapshot.current.tempC,
                lastConditionCode = snapshot.current.wmoCode,
                lastIsDay = snapshot.current.isDay,
                lastHighC = snapshot.current.todayHighC,
                lastLowC = snapshot.current.todayLowC,
                lastHumidity = snapshot.current.humidityPercent,
                lastWindKmh = snapshot.current.windSpeedKmh,
                lastPrecipProb = snapshot.hourly.firstOrNull()?.precipProbPercent,
                lastAqi = snapshot.airQuality?.usAqi,
                lastUv = snapshot.current.uvIndex,
                lastUpdatedEpochMillis = now
            )
            if (weatherDao.getLocationById(location.id) != null || location.isCurrentLocation) {
                weatherDao.upsertLocation(SavedLocationEntity.fromDomain(updatedLocation))
            }

            WeatherResult.Success(snapshot.copy(location = updatedLocation))
        } catch (e: HttpException) {
            if (e.code() == 429) {
                rateLimitCooldownUntilMillis = now + 60_000L
            }
            WeatherResult.Error(
                message = "Weather service error (${e.code()})",
                isRateLimited = e.code() == 429,
                cachedFallback = cachedSnapshot
            )
        } catch (e: IOException) {
            WeatherResult.Error(
                message = "Network unavailable",
                cachedFallback = cachedSnapshot
            )
        } catch (e: Exception) {
            WeatherResult.Error(
                message = e.localizedMessage ?: "Unexpected weather data error",
                cachedFallback = cachedSnapshot
            )
        }
    }

    override suspend fun refreshFromCurrentGpsLocation(): WeatherResult? = withContext(Dispatchers.IO) {
        val coords = locationTracker.getCurrentCoordinates() ?: return@withContext null
        val reverse = runCatching {
            nominatimApi.reverseGeocode(coords.latitude, coords.longitude)
        }.getOrNull()

        val addr = reverse?.address
        val cityName = addr?.city ?: addr?.town ?: addr?.village ?: addr?.county
            ?: reverse?.name ?: "Current Location"
        val gpsLocation = SavedLocation(
            id = "gps_current",
            name = cityName,
            admin1 = addr?.state.orEmpty(),
            country = addr?.country.orEmpty(),
            countryCode = addr?.countryCode?.uppercase(Locale.US).orEmpty(),
            latitude = coords.latitude,
            longitude = coords.longitude,
            timezone = "auto",
            isCurrentLocation = true,
            sortOrder = -1
        )
        weatherDao.upsertLocation(SavedLocationEntity.fromDomain(gpsLocation))
        getLocationWeather(gpsLocation, forceRefresh = true)
    }

    override suspend fun searchLocations(query: String): Result<List<SavedLocation>> =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.length < 2) return@withContext Result.success(emptyList())
            runCatching {
                val resp = geocodingApi.searchCities(trimmed)
                resp.results.orEmpty().mapIndexed { idx, dto ->
                    SavedLocation(
                        id = "geo_${dto.id}",
                        name = dto.name,
                        admin1 = dto.admin1.orEmpty(),
                        country = dto.country.orEmpty(),
                        countryCode = dto.countryCode.orEmpty(),
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        timezone = dto.timezone ?: "auto",
                        isCurrentLocation = false,
                        sortOrder = 100 + idx
                    )
                }
            }
        }

    override suspend fun saveLocation(location: SavedLocation) = withContext(Dispatchers.IO) {
        val existingCount = weatherDao.getSavedLocationsOnce().size
        val toSave = location.copy(sortOrder = if (location.isCurrentLocation) -1 else existingCount)
        weatherDao.upsertLocation(SavedLocationEntity.fromDomain(toSave))
    }

    override suspend fun removeLocation(locationId: String) = withContext(Dispatchers.IO) {
        weatherDao.deleteLocationById(locationId)
        weatherDao.deleteWeatherCache(locationId)
    }

    override suspend fun refreshAllSavedLocationsSummary() = withContext(Dispatchers.IO) {
        val saved = weatherDao.getSavedLocationsOnce().map { it.toDomain() }
        saved.forEach { loc ->
            getLocationWeather(loc, forceRefresh = false)
        }
    }

    override suspend fun getRainViewerRadarPath(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resp = rainViewerApi.getWeatherMaps()
            val latest = resp.radar?.nowcast?.lastOrNull()
                ?: resp.radar?.past?.lastOrNull()
                ?: return@runCatching null
            "${resp.host}${latest.path}"
        }.getOrNull()
    }

    override suspend fun getSpatialGrid(
        centerLat: Double,
        centerLon: Double,
        spanDeg: Double
    ): List<GridWeatherPoint> = withContext(Dispatchers.IO) {
        val halfSpan = (spanDeg / 2.0).coerceIn(1.0, 14.0)
        val steps = listOf(-1.0, -0.5, 0.0, 0.5, 1.0)
        val lats = mutableListOf<Double>()
        val lons = mutableListOf<Double>()
        for (dy in steps) {
            for (dx in steps) {
                lats.add((centerLat + dy * halfSpan).coerceIn(-85.0, 85.0))
                lons.add(((centerLon + dx * halfSpan + 180.0) % 360.0 + 360.0) % 360.0 - 180.0)
            }
        }
        val latCsv = lats.joinToString(",") { "%.2f".format(Locale.US, it) }
        val lonCsv = lons.joinToString(",") { "%.2f".format(Locale.US, it) }

        val gridResult = runCatching {
            forecastApi.getSpatialGrid(latCsv, lonCsv)
        }.getOrDefault(emptyList())

        val gridPoints = gridResult.map { item ->
            val cur = item.current
            GridWeatherPoint(
                latitude = item.latitude,
                longitude = item.longitude,
                label = null,
                tempC = cur?.temperature2m ?: 0.0,
                windSpeedKmh = cur?.windSpeed10m ?: 0.0,
                windDirectionDeg = cur?.windDirection10m ?: 0,
                cloudCoverPercent = cur?.cloudCover ?: 0,
                precipMm = cur?.precipitation ?: 0.0,
                wmoCode = cur?.weatherCode ?: 0
            )
        }

        // Also overlay key named Bangladesh & regional cities when viewing South Asia
        val bdStations = if (centerLat in 15.0..30.0 && centerLon in 82.0..98.0) {
            fetchNamedCitiesGrid(BangladeshQuickCities.all)
        } else {
            emptyList()
        }

        bdStations + gridPoints
    }

    private suspend fun fetchNamedCitiesGrid(cities: List<SavedLocation>): List<GridWeatherPoint> {
        val latCsv = cities.joinToString(",") { "%.2f".format(Locale.US, it.latitude) }
        val lonCsv = cities.joinToString(",") { "%.2f".format(Locale.US, it.longitude) }
        val items = runCatching { forecastApi.getSpatialGrid(latCsv, lonCsv) }.getOrDefault(emptyList())
        return items.mapIndexed { index, item ->
            val cur = item.current
            GridWeatherPoint(
                latitude = item.latitude,
                longitude = item.longitude,
                label = cities.getOrNull(index)?.name,
                tempC = cur?.temperature2m ?: 0.0,
                windSpeedKmh = cur?.windSpeed10m ?: 0.0,
                windDirectionDeg = cur?.windDirection10m ?: 0,
                cloudCoverPercent = cur?.cloudCover ?: 0,
                precipMm = cur?.precipitation ?: 0.0,
                wmoCode = cur?.weatherCode ?: 0
            )
        }
    }

    private fun mapToSnapshot(
        location: SavedLocation,
        resp: OpenMeteoForecastResponse,
        aqiDto: com.example.data.remote.AirQualityCurrentDto?,
        nowMillis: Long
    ): WeatherSnapshot {
        val tzId = resp.timezone.ifBlank { "UTC" }
        val tz = runCatching { TimeZone.getTimeZone(tzId) }.getOrDefault(TimeZone.getDefault())
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = tz }

        val hourlyDto = resp.hourly
        val allHourly = mutableListOf<HourlyForecast>()
        val times = hourlyDto?.time.orEmpty()
        for (i in times.indices) {
            val iso = times[i]
            val epoch = runCatching { parser.parse(iso)?.time }.getOrNull() ?: nowMillis
            allHourly.add(
                HourlyForecast(
                    isoTime = iso,
                    epochMillis = epoch,
                    tempC = hourlyDto?.temperature2m?.getOrNull(i) ?: 0.0,
                    feelsLikeC = hourlyDto?.apparentTemperature?.getOrNull(i) ?: 0.0,
                    precipProbPercent = hourlyDto?.precipitationProbability?.getOrNull(i) ?: 0,
                    precipMm = hourlyDto?.precipitation?.getOrNull(i) ?: 0.0,
                    wmoCode = hourlyDto?.weatherCode?.getOrNull(i) ?: 0,
                    isDay = (hourlyDto?.isDay?.getOrNull(i) ?: 1) == 1,
                    windSpeedKmh = hourlyDto?.windSpeed10m?.getOrNull(i) ?: 0.0,
                    humidityPercent = hourlyDto?.relativeHumidity2m?.getOrNull(i) ?: 0,
                    uvIndex = hourlyDto?.uvIndex?.getOrNull(i) ?: 0.0,
                    cape = hourlyDto?.cape?.getOrNull(i) ?: 0.0
                )
            )
        }

        // Align 24-hour slice starting from current local hour
        val currentHourFloor = nowMillis - 3_600_000L
        val next24Hourly = allHourly
            .filter { it.epochMillis >= currentHourFloor }
            .take(24)
            .ifEmpty { allHourly.take(24) }

        val currentIdxInHourly = allHourly.indexOfFirst { it.epochMillis >= currentHourFloor }
            .coerceAtLeast(0)

        val currentPressure = resp.current?.surfacePressure ?: 1013.0
        val prevPressure = hourlyDto?.surfacePressure?.getOrNull((currentIdxInHourly - 3).coerceAtLeast(0))
            ?: currentPressure
        val pressureDelta = currentPressure - prevPressure
        val pressureTrend = when {
            pressureDelta >= 1.0 -> PressureTrend.RISING
            pressureDelta <= -1.0 -> PressureTrend.FALLING
            else -> PressureTrend.STEADY
        }

        val currentVisibilityKm = (hourlyDto?.visibility?.getOrNull(currentIdxInHourly) ?: 10_000.0) / 1000.0
        val currentUv = hourlyDto?.uvIndex?.getOrNull(currentIdxInHourly) ?: 0.0
        val currentCape = hourlyDto?.cape?.getOrNull(currentIdxInHourly) ?: 0.0

        val dailyDto = resp.daily
        val dailyDates = dailyDto?.time.orEmpty()
        val dailyList = dailyDates.indices.map { i ->
            DailyForecast(
                isoDate = dailyDates[i],
                minTempC = dailyDto?.temperature2mMin?.getOrNull(i) ?: 0.0,
                maxTempC = dailyDto?.temperature2mMax?.getOrNull(i) ?: 0.0,
                wmoCode = dailyDto?.weatherCode?.getOrNull(i) ?: 0,
                precipProbMaxPercent = dailyDto?.precipitationProbabilityMax?.getOrNull(i) ?: 0,
                precipSumMm = dailyDto?.precipitationSum?.getOrNull(i) ?: 0.0,
                windSpeedMaxKmh = dailyDto?.windSpeed10mMax?.getOrNull(i) ?: 0.0,
                windGustsMaxKmh = dailyDto?.windGusts10mMax?.getOrNull(i) ?: 0.0,
                uvIndexMax = dailyDto?.uvIndexMax?.getOrNull(i) ?: 0.0,
                sunriseIso = dailyDto?.sunrise?.getOrNull(i).orEmpty(),
                sunsetIso = dailyDto?.sunset?.getOrNull(i).orEmpty(),
                daylightDurationSec = dailyDto?.daylightDuration?.getOrNull(i) ?: 43200.0,
                humidityMeanPercent = dailyDto?.relativeHumidity2mMean?.getOrNull(i) ?: 65
            )
        }

        val c = resp.current
        val today = dailyList.firstOrNull()
        val current = CurrentWeather(
            tempC = c?.temperature2m ?: next24Hourly.firstOrNull()?.tempC ?: 0.0,
            feelsLikeC = c?.apparentTemperature ?: next24Hourly.firstOrNull()?.feelsLikeC ?: 0.0,
            humidityPercent = c?.relativeHumidity2m ?: next24Hourly.firstOrNull()?.humidityPercent ?: 0,
            dewPointC = c?.dewPoint2m ?: estimateDewPoint(
                c?.temperature2m ?: 25.0,
                c?.relativeHumidity2m ?: 60
            ),
            windSpeedKmh = c?.windSpeed10m ?: 0.0,
            windGustsKmh = c?.windGusts10m ?: 0.0,
            windDirectionDeg = c?.windDirection10m ?: 0,
            pressureHpa = currentPressure,
            pressureTrend = pressureTrend,
            visibilityKm = currentVisibilityKm,
            uvIndex = currentUv,
            cloudCoverPercent = c?.cloudCover ?: 0,
            precipMm = c?.precipitation ?: 0.0,
            wmoCode = c?.weatherCode ?: 0,
            isDay = (c?.isDay ?: 1) == 1,
            capeJoulesPerKg = currentCape,
            todayHighC = today?.maxTempC ?: next24Hourly.maxOfOrNull { it.tempC } ?: 0.0,
            todayLowC = today?.minTempC ?: next24Hourly.minOfOrNull { it.tempC } ?: 0.0
        )

        val airQuality = aqiDto?.let {
            AirQualityData(
                usAqi = it.usAqi ?: 0,
                europeanAqi = it.europeanAqi ?: 0,
                pm25 = it.pm25 ?: 0.0,
                pm10 = it.pm10 ?: 0.0,
                ozone = it.ozone ?: 0.0,
                nitrogenDioxide = it.nitrogenDioxide ?: 0.0,
                sulphurDioxide = it.sulphurDioxide ?: 0.0,
                carbonMonoxide = it.carbonMonoxide ?: 0.0
            )
        }

        val astronomy = AstronomyCalculator.calculate(
            latitude = resp.latitude,
            longitude = resp.longitude,
            timezoneId = tzId,
            sunriseIso = today?.sunriseIso.orEmpty(),
            sunsetIso = today?.sunsetIso.orEmpty(),
            daylightDurationSec = today?.daylightDurationSec ?: 43200.0,
            nowMillis = nowMillis
        )

        val bdAdvisory = BangladeshWeatherAnalyzer.analyze(
            location = location,
            timezoneId = tzId,
            current = current,
            hourlyNext24 = next24Hourly
        )

        return WeatherSnapshot(
            location = location.copy(timezone = tzId),
            timezoneId = tzId,
            utcOffsetSeconds = resp.utcOffsetSeconds,
            current = current,
            hourly = next24Hourly,
            daily = dailyList,
            airQuality = airQuality,
            astronomy = astronomy,
            bangladeshAdvisory = bdAdvisory,
            lastUpdatedEpochMillis = nowMillis,
            isFromCache = false,
            providerName = "Open-Meteo"
        )
    }

    private fun estimateDewPoint(tempC: Double, rh: Int): Double {
        val a = 17.27
        val b = 237.7
        val safeRh = rh.coerceIn(5, 100) / 100.0
        val gamma = ((a * tempC) / (b + tempC)) + kotlin.math.ln(safeRh)
        return (b * gamma) / (a - gamma)
    }
}

object BangladeshQuickCities {
    val all = listOf(
        SavedLocation("bd_dhaka", "Dhaka", "Dhaka Division", "Bangladesh", "BD", 23.8103, 90.4125, "Asia/Dhaka"),
        SavedLocation("bd_chattogram", "Chattogram", "Chattogram Division", "Bangladesh", "BD", 22.3569, 91.7832, "Asia/Dhaka"),
        SavedLocation("bd_sylhet", "Sylhet", "Sylhet Division", "Bangladesh", "BD", 24.8949, 91.8687, "Asia/Dhaka"),
        SavedLocation("bd_coxsbazar", "Cox's Bazar", "Chattogram Division", "Bangladesh", "BD", 21.4272, 92.0058, "Asia/Dhaka"),
        SavedLocation("bd_khulna", "Khulna", "Khulna Division", "Bangladesh", "BD", 22.8456, 89.5403, "Asia/Dhaka"),
        SavedLocation("bd_rajshahi", "Rajshahi", "Rajshahi Division", "Bangladesh", "BD", 24.3745, 88.6042, "Asia/Dhaka"),
        SavedLocation("bd_rangpur", "Rangpur", "Rangpur Division", "Bangladesh", "BD", 25.7439, 89.2752, "Asia/Dhaka"),
        SavedLocation("bd_barishal", "Barishal", "Barishal Division", "Bangladesh", "BD", 22.7010, 90.3535, "Asia/Dhaka"),
        SavedLocation("bd_mymensingh", "Mymensingh", "Mymensingh Division", "Bangladesh", "BD", 24.7471, 90.4203, "Asia/Dhaka")
    )
}
