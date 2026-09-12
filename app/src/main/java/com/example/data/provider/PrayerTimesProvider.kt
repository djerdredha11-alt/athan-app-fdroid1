package com.example.data.provider

import com.example.data.model.DayPrayerTimes
import com.example.data.model.LocationData
import com.example.data.model.UserSettings
import java.time.LocalDate

interface PrayerTimesProvider {
    val providerId: String
    val providerNameArabic: String

    suspend fun getPrayerTimes(
        date: LocalDate,
        location: LocationData,
        settings: UserSettings
    ): Result<DayPrayerTimes>
}
