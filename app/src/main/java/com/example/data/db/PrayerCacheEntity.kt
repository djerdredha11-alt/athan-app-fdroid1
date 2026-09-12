package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_prayer_times")
data class PrayerCacheEntity(
    @PrimaryKey
    val cacheKey: String, // e.g. "2026-09-12_21.42_39.82"
    val dateIso: String,
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val fajrTime: String,
    val sunriseTime: String,
    val dhuhrTime: String,
    val asrTime: String,
    val maghribTime: String,
    val ishaTime: String,
    val hijriDate: String,
    val gregorianDate: String,
    val calculationMethod: String,
    val juristicMethod: String,
    val sourceDescription: String,
    val cachedTimestamp: Long
)
