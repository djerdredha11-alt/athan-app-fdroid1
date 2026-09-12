package com.example.data.provider

import com.example.data.calculation.AstronomicalCalculator
import com.example.data.model.DayPrayerTimes
import com.example.data.model.LocationData
import com.example.data.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class OfflineAstronomicalProvider : PrayerTimesProvider {
    override val providerId: String = "offline_astronomical"
    override val providerNameArabic: String = "الحساب الفلكي الدقيق (محلي/دون إنترنت)"

    override suspend fun getPrayerTimes(
        date: LocalDate,
        location: LocationData,
        settings: UserSettings
    ): Result<DayPrayerTimes> = withContext(Dispatchers.Default) {
        try {
            val times = AstronomicalCalculator.calculate(date, location, settings)
            Result.success(times)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
