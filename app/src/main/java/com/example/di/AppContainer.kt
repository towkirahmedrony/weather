package com.example.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.data.local.AtmosDatabase
import com.example.data.local.PreferencesManager
import com.example.data.location.LocationTracker
import com.example.data.remote.NominatimApi
import com.example.data.remote.OpenMeteoAirQualityApi
import com.example.data.remote.OpenMeteoForecastApi
import com.example.data.remote.OpenMeteoGeocodingApi
import com.example.data.remote.RainViewerApi
import com.example.data.repository.OpenMeteoWeatherRepository
import com.example.data.repository.WeatherRepository
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    // Every type Moshi sees uses @JsonClass(generateAdapter = true), so no reflective
    // KotlinJsonAdapterFactory is needed -- omitting it keeps kotlin-reflect out of the APK.
    val moshi: Moshi = Moshi.Builder().build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "atmos_http_cache"), 16L * 1024L * 1024L))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val forecastRetrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val aqiRetrofit = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val geocodingRetrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val rainViewerRetrofit = Retrofit.Builder()
        .baseUrl("https://api.rainviewer.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val nominatimRetrofit = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val database: AtmosDatabase = Room.databaseBuilder(
        context.applicationContext,
        AtmosDatabase::class.java,
        "atmos_weather.db"
    ).build()

    val preferencesManager = PreferencesManager(context.applicationContext)
    val locationTracker = LocationTracker(context.applicationContext)

    val weatherRepository: WeatherRepository = OpenMeteoWeatherRepository(
        forecastApi = forecastRetrofit.create(OpenMeteoForecastApi::class.java),
        airQualityApi = aqiRetrofit.create(OpenMeteoAirQualityApi::class.java),
        geocodingApi = geocodingRetrofit.create(OpenMeteoGeocodingApi::class.java),
        rainViewerApi = rainViewerRetrofit.create(RainViewerApi::class.java),
        nominatimApi = nominatimRetrofit.create(NominatimApi::class.java),
        weatherDao = database.weatherDao(),
        locationTracker = locationTracker,
        moshi = moshi
    )
}
