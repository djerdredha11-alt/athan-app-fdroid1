package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.NotificationMode

class PrayerAlarmReceiver : BroadcastReceiver {
    constructor() : super()

    companion object {
        const val CHANNEL_ID_ADHAN = "channel_adhan_v2"
        const val CHANNEL_ID_PRE_ALARM = "channel_pre_alarm_v2"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        createNotificationChannels(context)

        val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: "الصلاة"
        val prayerTypeStr = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE) ?: ""
        val notifModeStr = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_NOTIF_MODE) ?: NotificationMode.FULL_ADHAN.name
        val notifMode = try { NotificationMode.valueOf(notifModeStr) } catch (_: Exception) { NotificationMode.FULL_ADHAN }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (intent.action == PrayerAlarmScheduler.ACTION_PRE_ALARM) {
            val preMins = intent.getIntExtra("extra_pre_mins", 10)
            val title = context.getString(R.string.pre_adhan_title, prayerName)
            val body = context.getString(R.string.pre_adhan_body, preMins)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_PRE_ALARM)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)
                .build()

            notificationManager.notify(prayerTypeStr.hashCode() + 100, notification)
        } else if (intent.action == PrayerAlarmScheduler.ACTION_PRAYER_ALARM) {
            val title = context.getString(R.string.adhan_time_title, prayerName)
            val body = context.getString(R.string.adhan_time_body)

            // Intent to stop adhan
            val stopIntent = Intent(context, AdhanAudioService::class.java).apply {
                action = AdhanAudioService.ACTION_STOP
            }
            val stopPendingIntent = PendingIntent.getService(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notifBuilder = NotificationCompat.Builder(context, CHANNEL_ID_ADHAN)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)

            if (notifMode == NotificationMode.FULL_ADHAN) {
                notifBuilder.addAction(
                    android.R.drawable.ic_media_pause,
                    context.getString(R.string.action_stop_adhan),
                    stopPendingIntent
                )

                // Start audio service
                val audioIntent = Intent(context, AdhanAudioService::class.java).apply {
                    action = AdhanAudioService.ACTION_PLAY
                    putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME, prayerName)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(audioIntent)
                } else {
                    context.startService(audioIntent)
                }
            }

            notificationManager.notify(prayerTypeStr.hashCode(), notifBuilder.build())
        }

        // Re-schedule upcoming alarms
        PrayerAlarmScheduler.scheduleAllAlarms(context)
    }

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val adhanChannel = NotificationChannel(
                CHANNEL_ID_ADHAN,
                context.getString(R.string.notif_channel_adhan),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_adhan_desc)
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val preAlarmChannel = NotificationChannel(
                CHANNEL_ID_PRE_ALARM,
                context.getString(R.string.notif_channel_pre_alarm),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_pre_alarm_desc)
            }

            notificationManager.createNotificationChannel(adhanChannel)
            notificationManager.createNotificationChannel(preAlarmChannel)
        }
    }
}
