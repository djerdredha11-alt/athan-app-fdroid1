package com.example.data.db

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CalculationMethod
import com.example.data.model.JuristicMethod
import com.example.data.model.LocationData
import com.example.data.model.NotificationMode
import com.example.data.model.PrayerType
import com.example.data.model.UserSettings

class SettingsStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("adhan_user_preferences", Context.MODE_PRIVATE)

    fun loadSettings(): UserSettings {
        val calcMethodName = prefs.getString("calc_method", CalculationMethod.UMM_AL_QURA.name)
        val calcMethod = try {
            CalculationMethod.valueOf(calcMethodName ?: CalculationMethod.UMM_AL_QURA.name)
        } catch (_: Exception) {
            CalculationMethod.UMM_AL_QURA
        }

        val juristicName = prefs.getString("juristic_method", JuristicMethod.STANDARD.name)
        val juristicMethod = try {
            JuristicMethod.valueOf(juristicName ?: JuristicMethod.STANDARD.name)
        } catch (_: Exception) {
            JuristicMethod.STANDARD
        }

        val is24h = prefs.getBoolean("is_24h", false)
        val preAlarmMins = prefs.getInt("pre_alarm_mins", 10)
        val volume = prefs.getFloat("audio_volume", 0.85f)
        val customAudioUri = prefs.getString("custom_audio_uri", null)
        val useGps = prefs.getBoolean("use_gps", true)
        val useOnlineApi = prefs.getBoolean("use_online_api", false)

        val cityName = prefs.getString("loc_city", "مكة المكرمة") ?: "مكة المكرمة"
        val countryName = prefs.getString("loc_country", "المملكة العربية السعودية") ?: "المملكة العربية السعودية"
        val latitude = prefs.getFloat("loc_lat", 21.4225f).toDouble()
        val longitude = prefs.getFloat("loc_lon", 39.8262f).toDouble()

        val location = LocationData(
            cityName = cityName,
            countryName = countryName,
            latitude = latitude,
            longitude = longitude,
            isGps = useGps
        )

        // Minute adjustments
        val minuteAdjustments = mutableMapOf<PrayerType, Int>()
        for (prayer in PrayerType.entries) {
            val adj = prefs.getInt("adj_${prayer.name}", 0)
            if (adj != 0) {
                minuteAdjustments[prayer] = adj
            }
        }

        // Notification modes
        val notifModes = mutableMapOf<PrayerType, NotificationMode>()
        for (prayer in PrayerType.entries) {
            val defaultMode = if (prayer == PrayerType.SUNRISE) NotificationMode.NOTIFICATION_ONLY else NotificationMode.FULL_ADHAN
            val modeStr = prefs.getString("notif_${prayer.name}", defaultMode.name)
            val mode = try {
                NotificationMode.valueOf(modeStr ?: defaultMode.name)
            } catch (_: Exception) {
                defaultMode
            }
            notifModes[prayer] = mode
        }

        return UserSettings(
            calculationMethod = calcMethod,
            juristicMethod = juristicMethod,
            is24HourFormat = is24h,
            preAlarmMinutes = preAlarmMins,
            volume = volume,
            customAudioUri = customAudioUri,
            useGps = useGps,
            selectedLocation = location,
            minuteAdjustments = minuteAdjustments,
            prayerNotifications = notifModes,
            useOnlineApi = useOnlineApi
        )
    }

    fun saveSettings(settings: UserSettings) {
        prefs.edit().apply {
            putString("calc_method", settings.calculationMethod.name)
            putString("juristic_method", settings.juristicMethod.name)
            putBoolean("is_24h", settings.is24HourFormat)
            putInt("pre_alarm_mins", settings.preAlarmMinutes)
            putFloat("audio_volume", settings.volume)
            putString("custom_audio_uri", settings.customAudioUri)
            putBoolean("use_gps", settings.useGps)
            putBoolean("use_online_api", settings.useOnlineApi)

            putString("loc_city", settings.selectedLocation.cityName)
            putString("loc_country", settings.selectedLocation.countryName)
            putFloat("loc_lat", settings.selectedLocation.latitude.toFloat())
            putFloat("loc_lon", settings.selectedLocation.longitude.toFloat())

            for (prayer in PrayerType.entries) {
                putInt("adj_${prayer.name}", settings.minuteAdjustments[prayer] ?: 0)
                putString("notif_${prayer.name}", (settings.prayerNotifications[prayer] ?: NotificationMode.FULL_ADHAN).name)
            }
            apply()
        }
    }
}
