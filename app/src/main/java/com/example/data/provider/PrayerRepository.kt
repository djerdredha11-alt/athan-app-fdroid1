package com.example.data.provider

import android.content.Context
import com.example.data.calculation.AstronomicalCalculator
import com.example.data.db.AppDatabase
import com.example.data.db.PrayerCacheEntity
import com.example.data.db.SettingsStorage
import com.example.data.model.DayPrayerTimes
import com.example.data.model.LocationData
import com.example.data.model.PrayerCountdown
import com.example.data.model.PrayerType
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerRepository(private val context: Context) {

    private val settingsStorage = SettingsStorage(context)
    private val database = AppDatabase.getInstance(context)
    private val prayerDao = database.prayerDao()

    private val offlineProvider = OfflineAstronomicalProvider()
    private val apiProvider = AladhanApiProvider(offlineProvider)

    private val _userSettings = MutableStateFlow(settingsStorage.loadSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    suspend fun getPrayerTimesForDate(
        date: LocalDate,
        location: LocationData = _userSettings.value.selectedLocation,
        forceRefresh: Boolean = false
    ): DayPrayerTimes {
        val currentSettings = _userSettings.value
        val cacheKey = "${date}_${location.latitude.toInt()}_${location.longitude.toInt()}_${currentSettings.calculationMethod}_${currentSettings.juristicMethod}"

        if (!forceRefresh) {
            val cached = prayerDao.getCached(cacheKey)
            if (cached != null) {
                return parseCached(cached, location, currentSettings)
            }
        }

        val provider = if (currentSettings.useOnlineApi) apiProvider else offlineProvider
        val result = provider.getPrayerTimes(date, location, currentSettings)
        val prayerTimes = result.getOrElse {
            offlineProvider.getPrayerTimes(date, location, currentSettings).getOrThrow()
        }

        // Cache in Room
        try {
            val entity = PrayerCacheEntity(
                cacheKey = cacheKey,
                dateIso = date.toString(),
                cityName = location.cityName,
                countryName = location.countryName,
                latitude = location.latitude,
                longitude = location.longitude,
                fajrTime = prayerTimes.fajr.toString(),
                sunriseTime = prayerTimes.sunrise.toString(),
                dhuhrTime = prayerTimes.dhuhr.toString(),
                asrTime = prayerTimes.asr.toString(),
                maghribTime = prayerTimes.maghrib.toString(),
                ishaTime = prayerTimes.isha.toString(),
                hijriDate = prayerTimes.hijriDate,
                gregorianDate = prayerTimes.gregorianDate,
                calculationMethod = prayerTimes.calculationMethod.name,
                juristicMethod = prayerTimes.juristicMethod.name,
                sourceDescription = prayerTimes.sourceDescription,
                cachedTimestamp = System.currentTimeMillis()
            )
            prayerDao.insert(entity)
        } catch (_: Exception) {
            // DB cache error non-critical
        }

        return prayerTimes
    }

    private fun parseCached(
        cached: PrayerCacheEntity,
        location: LocationData,
        settings: UserSettings
    ): DayPrayerTimes {
        val date = LocalDate.parse(cached.dateIso)
        return DayPrayerTimes(
            date = date,
            hijriDate = cached.hijriDate,
            gregorianDate = cached.gregorianDate,
            location = location,
            fajr = LocalTime.parse(cached.fajrTime),
            sunrise = LocalTime.parse(cached.sunriseTime),
            dhuhr = LocalTime.parse(cached.dhuhrTime),
            asr = LocalTime.parse(cached.asrTime),
            maghrib = LocalTime.parse(cached.maghribTime),
            isha = LocalTime.parse(cached.ishaTime),
            calculationMethod = settings.calculationMethod,
            juristicMethod = settings.juristicMethod,
            sourceDescription = cached.sourceDescription
        )
    }

    fun updateSettings(newSettings: UserSettings) {
        settingsStorage.saveSettings(newSettings)
        _userSettings.value = newSettings
    }

    /**
     * Compute current prayer and next prayer countdown accurately
     */
    fun calculateCountdown(
        todayTimes: DayPrayerTimes,
        tomorrowTimes: DayPrayerTimes,
        now: LocalDateTime = LocalDateTime.now()
    ): PrayerCountdown {
        val today = todayTimes.date
        val tomorrow = tomorrowTimes.date

        val prayersToday = listOf(
            PrayerType.FAJR to LocalDateTime.of(today, todayTimes.fajr),
            PrayerType.SUNRISE to LocalDateTime.of(today, todayTimes.sunrise),
            PrayerType.DHUHR to LocalDateTime.of(today, todayTimes.dhuhr),
            PrayerType.ASR to LocalDateTime.of(today, todayTimes.asr),
            PrayerType.MAGHRIB to LocalDateTime.of(today, todayTimes.maghrib),
            PrayerType.ISHA to LocalDateTime.of(today, todayTimes.isha)
        )

        val fajrTomorrow = LocalDateTime.of(tomorrow, tomorrowTimes.fajr)

        var currentPrayer: PrayerType? = null
        var nextPrayer: PrayerType = PrayerType.FAJR
        var currentPrayerTime: LocalDateTime = prayersToday.last().second
        var nextPrayerTime: LocalDateTime = fajrTomorrow

        if (now.isBefore(prayersToday[0].second)) {
            // Before Fajr today
            currentPrayer = null
            nextPrayer = PrayerType.FAJR
            currentPrayerTime = prayersToday[0].second.minusHours(4) // approximate prior window
            nextPrayerTime = prayersToday[0].second
        } else {
            for (i in 0 until prayersToday.size) {
                val (prayer, time) = prayersToday[i]
                if (!now.isBefore(time)) {
                    currentPrayer = prayer
                    currentPrayerTime = time
                    if (i < prayersToday.size - 1) {
                        nextPrayer = prayersToday[i + 1].first
                        nextPrayerTime = prayersToday[i + 1].second
                    } else {
                        // After Isha today, next is Fajr tomorrow
                        nextPrayer = PrayerType.FAJR
                        nextPrayerTime = fajrTomorrow
                    }
                }
            }
        }

        val totalWindowMillis = Duration.between(currentPrayerTime, nextPrayerTime).toMillis().coerceAtLeast(1000L)
        val remainingMillis = Duration.between(now, nextPrayerTime).toMillis().coerceAtLeast(0L)
        val elapsedMillis = totalWindowMillis - remainingMillis
        val progress = (elapsedMillis.toFloat() / totalWindowMillis.toFloat()).coerceIn(0f, 1f)

        return PrayerCountdown(
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer,
            remainingMillis = remainingMillis,
            totalPrayerWindowMillis = totalWindowMillis,
            progress = progress
        )
    }

    fun formatTime(time: LocalTime, is24h: Boolean): String {
        return if (is24h) {
            time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
        } else {
            val hour12 = if (time.hour % 12 == 0) 12 else time.hour % 12
            val period = if (time.hour < 12) "ص" else "م"
            String.format(Locale.US, "%d:%02d %s", hour12, time.minute, period)
        }
    }
}
