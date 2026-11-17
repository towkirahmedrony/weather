package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.AdvisorySeverity
import com.example.domain.model.AppLanguage
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherSnapshot
import com.example.domain.util.UnitFormatter

object WeatherNotificationHelper {

    const val CHANNEL_UPDATES = "atmos_weather_updates"
    const val CHANNEL_ALERTS = "atmos_weather_alerts"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES,
                context.getString(R.string.notif_channel_updates),
                NotificationManager.IMPORTANCE_LOW
            )
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.notif_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(updatesChannel)
            nm.createNotificationChannel(alertsChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun evaluateAndNotify(
        context: Context,
        snapshot: WeatherSnapshot,
        prefs: UserPreferences
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val isBn = prefs.language == AppLanguage.BANGLA
        val city = snapshot.location.name
        val tempStr = UnitFormatter.formatTemp(snapshot.current.tempC, prefs.temperatureUnit, true)
        val condStr = UnitFormatter.conditionLabel(snapshot.current.condition, context)

        val tapIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = NotificationManagerCompat.from(context)

        if (prefs.severeAlertsEnabled) {
            val bd = snapshot.bangladeshAdvisory
            if (bd.stormSeverity == AdvisorySeverity.SEVERE ||
                bd.stormSeverity == AdvisorySeverity.WARNING ||
                bd.heatSeverity == AdvisorySeverity.SEVERE ||
                bd.coastalWindSeverity == AdvisorySeverity.SEVERE
            ) {
                val title = if (isBn) "$city: আবহাওয়া সতর্কতা" else "$city: Weather Advisory"
                val body = buildString {
                    if (bd.stormSeverity >= AdvisorySeverity.WARNING) {
                        append(if (isBn) "বজ্রপাত ও ঝোড়ো হাওয়ার সম্ভাবনা। " else "Thunderstorm & lightning risk ahead. ")
                    }
                    if (bd.heatSeverity == AdvisorySeverity.SEVERE) {
                        append(if (isBn) "অত্যধিক তাপপ্রবাহ সতর্কতা। " else "Extreme heat index alert. ")
                    }
                    if (bd.coastalWindSeverity == AdvisorySeverity.SEVERE) {
                        append(if (isBn) "তীব্র দমকা হাওয়া (${bd.peakWindGustKmh.toInt()} km/h)।" else "Severe wind gusts (${bd.peakWindGustKmh.toInt()} km/h).")
                    }
                }.trim()

                val notif = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(tapIntent)
                    .setAutoCancel(true)
                    .build()
                runCatching { nm.notify(1002, notif) }
            }
        }

        if (prefs.rainAlertsEnabled) {
            val upcomingRainHour = snapshot.hourly.take(6).firstOrNull { it.precipProbPercent >= 60 }
            if (upcomingRainHour != null) {
                val hourStr = UnitFormatter.formatHourLabel(upcomingRainHour.isoTime, prefs.language)
                val title = if (isBn) "$city: বৃষ্টির সম্ভাবনা" else "$city: Rain Expected"
                val body = if (isBn) {
                    "$hourStr নাগাদ বৃষ্টির সম্ভাবনা ${upcomingRainHour.precipProbPercent}%।"
                } else {
                    "${upcomingRainHour.precipProbPercent}% chance of rain around $hourStr."
                }
                val notif = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(tapIntent)
                    .setAutoCancel(true)
                    .build()
                runCatching { nm.notify(1003, notif) }
            }
        }

        if (prefs.notificationsEnabled) {
            val hi = UnitFormatter.formatTemp(snapshot.current.todayHighC, prefs.temperatureUnit)
            val lo = UnitFormatter.formatTemp(snapshot.current.todayLowC, prefs.temperatureUnit)
            val title = "$city · $tempStr ($condStr)"
            val body = if (isBn) "সর্বোচ্চ $hi · সর্বনিম্ন $lo" else "High $hi · Low $lo"
            val notif = NotificationCompat.Builder(context, CHANNEL_UPDATES)
                .setSmallIcon(android.R.drawable.ic_menu_day)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(tapIntent)
                .setOnlyAlertOnce(true)
                .build()
            runCatching { nm.notify(1001, notif) }
        }
    }
}
