package com.example.data.model

import java.time.LocalDate
import java.time.LocalTime

enum class PrayerType(val arabicName: String, val englishName: String, val order: Int) {
    FAJR("الفجر", "Fajr", 0),
    SUNRISE("الشروق", "Sunrise", 1),
    DHUHR("الظهر", "Dhuhr", 2),
    ASR("العصر", "Asr", 3),
    MAGHRIB("المغرب", "Maghrib", 4),
    ISHA("العشاء", "Isha", 5);

    val isSalat: Boolean
        get() = this != SUNRISE
}

enum class NotificationMode(val arabicTitle: String) {
    FULL_ADHAN("أذان كامل"),
    NOTIFICATION_ONLY("تنبيه فقط"),
    SILENT("صامت")
}

enum class CalculationMethod(
    val arabicName: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaIntervalMins: Int = 0 // > 0 if fixed minutes after Maghrib (e.g. Umm Al-Qura)
) {
    UMM_AL_QURA("أم القرى - مكة المكرمة", 18.5, 0.0, ishaIntervalMins = 90),
    MWL("رابطة العالم الإسلامي (MWL)", 18.0, 17.0),
    EGYPTIAN("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0),
    ISNA("الجمعية الإسلامية لأمريكا الشمالية (ISNA)", 15.0, 15.0),
    DUBAI("دبي / دولة الإمارات", 18.2, 18.2),
    KUWAIT("وزارة الأوقاف والشؤون الإسلامية - الكويت", 18.0, 17.5),
    QATAR("وزارة الأوقاف والشؤون الإسلامية - قطر", 18.0, 0.0, ishaIntervalMins = 90),
    TEHRAN("معهد الجيوفيزياء - جامعة طهران", 17.7, 14.0)
}

enum class JuristicMethod(val arabicName: String, val shadowFactor: Double) {
    STANDARD("جمهور الفقهاء (الشافعي، المالكي، الحنبلي)", 1.0),
    HANAFI("المذهب الحنفي (مثلان)", 2.0)
}

data class LocationData(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val isGps: Boolean = false
) {
    val displayName: String
        get() = if (countryName.isBlank()) cityName else "$cityName، $countryName"
}

data class DayPrayerTimes(
    val date: LocalDate,
    val hijriDate: String,
    val gregorianDate: String,
    val location: LocationData,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val calculationMethod: CalculationMethod,
    val juristicMethod: JuristicMethod,
    val sourceDescription: String
) {
    fun getTimeFor(prayer: PrayerType): LocalTime = when (prayer) {
        PrayerType.FAJR -> fajr
        PrayerType.SUNRISE -> sunrise
        PrayerType.DHUHR -> dhuhr
        PrayerType.ASR -> asr
        PrayerType.MAGHRIB -> maghrib
        PrayerType.ISHA -> isha
    }
}

data class PrayerCountdown(
    val currentPrayer: PrayerType?,
    val nextPrayer: PrayerType,
    val remainingMillis: Long,
    val totalPrayerWindowMillis: Long,
    val progress: Float
)

data class UserSettings(
    val calculationMethod: CalculationMethod = CalculationMethod.UMM_AL_QURA,
    val juristicMethod: JuristicMethod = JuristicMethod.STANDARD,
    val is24HourFormat: Boolean = false,
    val preAlarmMinutes: Int = 10,
    val volume: Float = 0.85f,
    val customAudioUri: String? = null,
    val useGps: Boolean = true,
    val selectedLocation: LocationData = LocationData(
        cityName = "مكة المكرمة",
        countryName = "المملكة العربية السعودية",
        latitude = 21.4225,
        longitude = 39.8262,
        isGps = false
    ),
    val minuteAdjustments: Map<PrayerType, Int> = emptyMap(),
    val prayerNotifications: Map<PrayerType, NotificationMode> = mapOf(
        PrayerType.FAJR to NotificationMode.FULL_ADHAN,
        PrayerType.SUNRISE to NotificationMode.NOTIFICATION_ONLY,
        PrayerType.DHUHR to NotificationMode.FULL_ADHAN,
        PrayerType.ASR to NotificationMode.FULL_ADHAN,
        PrayerType.MAGHRIB to NotificationMode.FULL_ADHAN,
        PrayerType.ISHA to NotificationMode.FULL_ADHAN
    ),
    val useOnlineApi: Boolean = false
)
