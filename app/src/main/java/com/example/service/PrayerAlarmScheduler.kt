package com.example.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.calculation.AstronomicalCalculator
import com.example.data.db.SettingsStorage
import com.example.data.model.NotificationMode
import com.example.data.model.PrayerType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object PrayerAlarmScheduler {

    const val ACTION_PRAYER_ALARM = "com.aistudio.adhan.ACTION_PRAYER_ALARM"
    const val ACTION_PRE_ALARM = "com.aistudio.adhan.ACTION_PRE_ALARM"

    const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
    const val EXTRA_NOTIF_MODE = "extra_notif_mode"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val settings = SettingsStorage(context).loadSettings()

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val todayTimes = AstronomicalCalculator.calculate(today, settings.selectedLocation, settings)
        val tomorrowTimes = AstronomicalCalculator.calculate(tomorrow, settings.selectedLocation, settings)

        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now()

        val prayersToSchedule = listOf(
            Triple(today, todayTimes, 0),
            Triple(tomorrow, tomorrowTimes, 100)
        )

        for ((date, times, requestCodeBase) in prayersToSchedule) {
            for (prayer in PrayerType.entries) {
                val notifMode = settings.prayerNotifications[prayer] ?: NotificationMode.FULL_ADHAN
                if (notifMode == NotificationMode.SILENT) continue

                val prayerTime = times.getTimeFor(prayer)
                val prayerDateTime = LocalDateTime.of(date, prayerTime)

                // 1. Schedule Pre-alarm if enabled
                if (settings.preAlarmMinutes > 0 && prayer.isSalat) {
                    val preDateTime = prayerDateTime.minusMinutes(settings.preAlarmMinutes.toLong())
                    if (preDateTime.isAfter(now)) {
                        val triggerMillis = preDateTime.atZone(zoneId).toInstant().toEpochMilli()
                        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                            action = ACTION_PRE_ALARM
                            putExtra(EXTRA_PRAYER_NAME, prayer.arabicName)
                            putExtra(EXTRA_PRAYER_TYPE, prayer.name)
                            putExtra("extra_pre_mins", settings.preAlarmMinutes)
                        }
                        val reqCode = requestCodeBase + prayer.order + 50
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            reqCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setExactAlarm(alarmManager, triggerMillis, pendingIntent)
                    }
                }

                // 2. Schedule exact prayer adhan
                if (prayerDateTime.isAfter(now)) {
                    val triggerMillis = prayerDateTime.atZone(zoneId).toInstant().toEpochMilli()
                    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = ACTION_PRAYER_ALARM
                        putExtra(EXTRA_PRAYER_NAME, prayer.arabicName)
                        putExtra(EXTRA_PRAYER_TYPE, prayer.name)
                        putExtra(EXTRA_NOTIF_MODE, notifMode.name)
                    }
                    val reqCode = requestCodeBase + prayer.order
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(alarmManager, triggerMillis, pendingIntent)
                }
            }
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }
}
