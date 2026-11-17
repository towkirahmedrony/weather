package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.AtmosApplication
import com.example.MainActivity
import com.example.R
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherCondition
import com.example.domain.model.WeatherSnapshot
import com.example.domain.util.UnitFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WeatherWidgetUpdater {

    fun updateAllWidgets(
        context: Context,
        snapshot: WeatherSnapshot,
        prefs: UserPreferences
    ) {
        val manager = AppWidgetManager.getInstance(context)

        val currentIds = manager.getAppWidgetIds(
            ComponentName(context, CurrentWeatherWidgetProvider::class.java)
        )
        currentIds.forEach { id ->
            val views = buildCurrentViews(context, snapshot, prefs)
            manager.updateAppWidget(id, views)
        }

        val forecastIds = manager.getAppWidgetIds(
            ComponentName(context, ForecastWeatherWidgetProvider::class.java)
        )
        forecastIds.forEach { id ->
            val views = buildForecastViews(context, snapshot, prefs)
            manager.updateAppWidget(id, views)
        }
    }

    private fun launchAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildCurrentViews(
        context: Context,
        snapshot: WeatherSnapshot,
        prefs: UserPreferences
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_current_weather)
        views.setOnClickPendingIntent(R.id.widget_current_root, launchAppIntent(context))
        views.setTextViewText(R.id.widget_city, snapshot.location.name)
        views.setTextViewText(
            R.id.widget_temp,
            UnitFormatter.formatTemp(snapshot.current.tempC, prefs.temperatureUnit)
        )
        views.setTextViewText(
            R.id.widget_condition,
            UnitFormatter.conditionLabel(snapshot.current.condition, context)
        )
        val feels = UnitFormatter.formatTemp(snapshot.current.feelsLikeC, prefs.temperatureUnit)
        val hi = UnitFormatter.formatTemp(snapshot.current.todayHighC, prefs.temperatureUnit)
        val lo = UnitFormatter.formatTemp(snapshot.current.todayLowC, prefs.temperatureUnit)
        views.setTextViewText(
            R.id.widget_high_low,
            "${context.getString(R.string.widget_feels_like_fmt, feels)} · ${context.getString(R.string.widget_high_low_fmt, hi, lo)}"
        )
        views.setTextViewText(
            R.id.widget_emoji_icon,
            conditionGlyph(snapshot.current.condition, snapshot.current.isDay)
        )
        return views
    }

    private fun buildForecastViews(
        context: Context,
        snapshot: WeatherSnapshot,
        prefs: UserPreferences
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_forecast_weather)
        views.setOnClickPendingIntent(R.id.widget_forecast_root, launchAppIntent(context))
        views.setTextViewText(R.id.widget_f_city, snapshot.location.name)
        views.setTextViewText(
            R.id.widget_f_temp,
            UnitFormatter.formatTemp(snapshot.current.tempC, prefs.temperatureUnit)
        )
        views.setTextViewText(
            R.id.widget_f_condition,
            UnitFormatter.conditionLabel(snapshot.current.condition, context)
        )
        views.setTextViewText(
            R.id.widget_f_updated,
            UnitFormatter.formatUpdatedAgo(snapshot.lastUpdatedEpochMillis, context)
        )

        val slots = snapshot.hourly.take(4)
        val timeIds = listOf(R.id.slot1_time, R.id.slot2_time, R.id.slot3_time, R.id.slot4_time)
        val iconIds = listOf(R.id.slot1_icon, R.id.slot2_icon, R.id.slot3_icon, R.id.slot4_icon)
        val tempIds = listOf(R.id.slot1_temp, R.id.slot2_temp, R.id.slot3_temp, R.id.slot4_temp)

        for (i in 0 until 4) {
            val h = slots.getOrNull(i)
            if (h != null) {
                val label = if (i == 0) context.getString(R.string.hourly_now)
                else UnitFormatter.formatHourLabel(h.isoTime, prefs.language)
                views.setTextViewText(timeIds[i], label)
                views.setTextViewText(iconIds[i], conditionGlyph(h.condition, h.isDay))
                views.setTextViewText(
                    tempIds[i],
                    UnitFormatter.formatTemp(h.tempC, prefs.temperatureUnit)
                )
            }
        }
        return views
    }

    private fun conditionGlyph(condition: WeatherCondition, isDay: Boolean): String = when (condition) {
        WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR -> if (isDay) "☀" else "☾"
        WeatherCondition.PARTLY_CLOUDY -> if (isDay) "⛅" else "☁"
        WeatherCondition.OVERCAST -> "☁"
        WeatherCondition.FOG -> "🌫"
        WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> "🌧"
        WeatherCondition.THUNDERSTORM -> "⛈"
        WeatherCondition.SNOW -> "❄"
        WeatherCondition.SQUALL -> "💨"
    }

    fun refreshFromCacheOrNetwork(context: Context) {
        val app = context.applicationContext as? AtmosApplication ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = app.container.preferencesManager.preferencesFlow.first()
            val snapshot = app.container.weatherRepository.getCachedWeather(prefs.selectedLocationId)
            if (snapshot != null) {
                updateAllWidgets(context, snapshot, prefs)
            }
        }
    }
}

class CurrentWeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WeatherWidgetUpdater.refreshFromCacheOrNetwork(context)
    }
}

class ForecastWeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WeatherWidgetUpdater.refreshFromCacheOrNetwork(context)
    }
}
