package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.AtmosApplication
import com.example.data.repository.WeatherResult
import com.example.widget.WeatherWidgetUpdater
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WeatherSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AtmosApplication ?: return Result.failure()
        val container = app.container
        val prefs = container.preferencesManager.preferencesFlow.first()

        container.weatherRepository.ensureDefaultLocationsSeeded()
        val saved = container.weatherRepository.observeSavedLocations().first()
        val activeLoc = saved.firstOrNull { it.id == prefs.selectedLocationId }
            ?: saved.firstOrNull()
            ?: return Result.success()

        val result = if (activeLoc.isCurrentLocation && container.locationTracker.hasLocationPermission()) {
            container.weatherRepository.refreshFromCurrentGpsLocation()
                ?: container.weatherRepository.getLocationWeather(activeLoc, forceRefresh = true)
        } else {
            container.weatherRepository.getLocationWeather(activeLoc, forceRefresh = true)
        }

        return when (result) {
            is WeatherResult.Success -> {
                WeatherWidgetUpdater.updateAllWidgets(applicationContext, result.snapshot, prefs)
                WeatherNotificationHelper.evaluateAndNotify(applicationContext, result.snapshot, prefs)
                Result.success()
            }
            is WeatherResult.Error -> {
                if (result.cachedFallback != null) {
                    WeatherWidgetUpdater.updateAllWidgets(applicationContext, result.cachedFallback, prefs)
                }
                if (result.isRateLimited) Result.retry() else Result.success()
            }
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "atmos_periodic_weather_sync"

        private fun getWorkManager(context: Context): WorkManager {
            if (!WorkManager.isInitialized()) {
                val config = (context.applicationContext as? androidx.work.Configuration.Provider)
                    ?.workManagerConfiguration
                    ?: androidx.work.Configuration.Builder().build()
                runCatching { WorkManager.initialize(context.applicationContext, config) }
            }
            return WorkManager.getInstance(context.applicationContext)
        }

        fun schedulePeriodic(context: Context, intervalHours: Int) {
            val hours = intervalHours.coerceAtLeast(1).toLong()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(hours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            runCatching {
                getWorkManager(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
        }

        fun triggerImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<WeatherSyncWorker>().build()
            runCatching {
                getWorkManager(context).enqueue(request)
            }
        }
    }
}
