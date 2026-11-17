package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.domain.model.SavedLocation
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val admin1: String,
    val country: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val isCurrentLocation: Boolean,
    val sortOrder: Int,
    val lastTempC: Double?,
    val lastConditionCode: Int?,
    val lastIsDay: Boolean,
    val lastHighC: Double?,
    val lastLowC: Double?,
    val lastHumidity: Int?,
    val lastWindKmh: Double?,
    val lastPrecipProb: Int?,
    val lastAqi: Int?,
    val lastUv: Double?,
    val lastUpdatedEpochMillis: Long
) {
    fun toDomain(): SavedLocation = SavedLocation(
        id = id,
        name = name,
        admin1 = admin1,
        country = country,
        countryCode = countryCode,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
        isCurrentLocation = isCurrentLocation,
        sortOrder = sortOrder,
        lastTempC = lastTempC,
        lastConditionCode = lastConditionCode,
        lastIsDay = lastIsDay,
        lastHighC = lastHighC,
        lastLowC = lastLowC,
        lastHumidity = lastHumidity,
        lastWindKmh = lastWindKmh,
        lastPrecipProb = lastPrecipProb,
        lastAqi = lastAqi,
        lastUv = lastUv,
        lastUpdatedEpochMillis = lastUpdatedEpochMillis
    )

    companion object {
        fun fromDomain(loc: SavedLocation): SavedLocationEntity = SavedLocationEntity(
            id = loc.id,
            name = loc.name,
            admin1 = loc.admin1,
            country = loc.country,
            countryCode = loc.countryCode,
            latitude = loc.latitude,
            longitude = loc.longitude,
            timezone = loc.timezone,
            isCurrentLocation = loc.isCurrentLocation,
            sortOrder = loc.sortOrder,
            lastTempC = loc.lastTempC,
            lastConditionCode = loc.lastConditionCode,
            lastIsDay = loc.lastIsDay,
            lastHighC = loc.lastHighC,
            lastLowC = loc.lastLowC,
            lastHumidity = loc.lastHumidity,
            lastWindKmh = loc.lastWindKmh,
            lastPrecipProb = loc.lastPrecipProb,
            lastAqi = loc.lastAqi,
            lastUv = loc.lastUv,
            lastUpdatedEpochMillis = loc.lastUpdatedEpochMillis
        )
    }
}

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationId: String,
    val snapshotJson: String,
    val updatedAtEpochMillis: Long
)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM saved_locations ORDER BY isCurrentLocation DESC, sortOrder ASC, name ASC")
    fun observeSavedLocations(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations ORDER BY isCurrentLocation DESC, sortOrder ASC, name ASC")
    suspend fun getSavedLocationsOnce(): List<SavedLocationEntity>

    @Query("SELECT * FROM saved_locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: SavedLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocations(locations: List<SavedLocationEntity>)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: String)

    @Query("SELECT * FROM weather_cache WHERE locationId = :locationId LIMIT 1")
    suspend fun getCachedWeather(locationId: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache WHERE locationId = :locationId LIMIT 1")
    fun observeCachedWeather(locationId: String): Flow<WeatherCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeatherCache(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE locationId = :locationId")
    suspend fun deleteWeatherCache(locationId: String)
}

@Database(
    entities = [SavedLocationEntity::class, WeatherCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AtmosDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
