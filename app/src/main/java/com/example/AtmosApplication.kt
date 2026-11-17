package com.example

import android.app.Application
import androidx.work.Configuration
import com.example.di.AppContainer
import com.example.worker.WeatherNotificationHelper
import com.example.worker.WeatherSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AtmosApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WeatherNotificationHelper.ensureChannels(this)

        appScope.launch {
            runCatching {
                container.weatherRepository.ensureDefaultLocationsSeeded()
                val prefs = container.preferencesManager.preferencesFlow.first()
                WeatherSyncWorker.schedulePeriodic(this@AtmosApplication, prefs.syncIntervalHours)
            }
        }
    }
}
