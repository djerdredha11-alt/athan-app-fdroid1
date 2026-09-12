package com.example.data.provider

import com.example.data.calculation.AstronomicalCalculator
import com.example.data.model.CalculationMethod
import com.example.data.model.DayPrayerTimes
import com.example.data.model.JuristicMethod
import com.example.data.model.LocationData
import com.example.data.model.PrayerType
import com.example.data.model.UserSettings
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AladhanTimings(
    @Json(name = "Fajr") val fajr: String,
    @Json(name = "Sunrise") val sunrise: String,
    @Json(name = "Dhuhr") val dhuhr: String,
    @Json(name = "Asr") val asr: String,
    @Json(name = "Maghrib") val maghrib: String,
    @Json(name = "Isha") val isha: String
)

@JsonClass(generateAdapter = true)
data class AladhanHijriMonth(
    @Json(name = "ar") val ar: String?
)

@JsonClass(generateAdapter = true)
data class AladhanHijri(
    @Json(name = "date") val date: String?,
    @Json(name = "day") val day: String?,
    @Json(name = "year") val year: String?,
    @Json(name = "month") val month: AladhanHijriMonth?
)

@JsonClass(generateAdapter = true)
data class AladhanDate(
    @Json(name = "hijri") val hijri: AladhanHijri?
)

@JsonClass(generateAdapter = true)
data class AladhanData(
    @Json(name = "timings") val timings: AladhanTimings,
    @Json(name = "date") val date: AladhanDate?
)

@JsonClass(generateAdapter = true)
data class AladhanResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: AladhanData?
)

interface AladhanApiService {
    @GET("v1/timings/{date}")
    suspend fun getTimings(
        @Path("date") dateFormatted: String, // DD-MM-YYYY
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int,
        @Query("school") school: Int
    ): AladhanResponse
}

class AladhanApiProvider(
    private val fallbackProvider: OfflineAstronomicalProvider = OfflineAstronomicalProvider()
) : PrayerTimesProvider {

    override val providerId: String = "aladhan_api"
    override val providerNameArabic: String = "مواقيت معتمدة عبر الإنترنت (Aladhan API)"

    private val apiService: AladhanApiService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AladhanApiService::class.java)
    }

    override suspend fun getPrayerTimes(
        date: LocalDate,
        location: LocationData,
        settings: UserSettings
    ): Result<DayPrayerTimes> = withContext(Dispatchers.IO) {
        try {
            val dateStr = String.format(Locale.US, "%02d-%02d-%04d", date.dayOfMonth, date.monthValue, date.year)
            val apiMethodId = mapMethodToAladhanId(settings.calculationMethod)
            val schoolId = if (settings.juristicMethod == JuristicMethod.HANAFI) 1 else 0

            val response = apiService.getTimings(
                dateFormatted = dateStr,
                latitude = location.latitude,
                longitude = location.longitude,
                method = apiMethodId,
                school = schoolId
            )

            val timings = response.data?.timings ?: throw IllegalStateException("Empty data from Aladhan API")

            fun parseTime(timeString: String, type: PrayerType): LocalTime {
                // Aladhan strings can be "05:12" or "05:12 (EEST)"
                val clean = timeString.split(" ").firstOrNull() ?: timeString
                val parts = clean.split(":")
                var hour = parts[0].toInt()
                var minute = parts[1].toInt()

                val offset = settings.minuteAdjustments[type] ?: 0
                val totalMins = hour * 60 + minute + offset
                val fixedMins = ((totalMins % 1440) + 1440) % 1440
                return LocalTime.of(fixedMins / 60, fixedMins % 60)
            }

            val hijriAr = response.data.date?.hijri
            val hijriFormatted = if (hijriAr?.day != null && hijriAr.month?.ar != null && hijriAr.year != null) {
                "${hijriAr.day} ${hijriAr.month.ar} ${hijriAr.year} هـ"
            } else {
                AstronomicalCalculator.calculateHijriDate(date)
            }

            val gregorianFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))

            val result = DayPrayerTimes(
                date = date,
                hijriDate = hijriFormatted,
                gregorianDate = date.format(gregorianFormatter),
                location = location,
                fajr = parseTime(timings.fajr, PrayerType.FAJR),
                sunrise = parseTime(timings.sunrise, PrayerType.SUNRISE),
                dhuhr = parseTime(timings.dhuhr, PrayerType.DHUHR),
                asr = parseTime(timings.asr, PrayerType.ASR),
                maghrib = parseTime(timings.maghrib, PrayerType.MAGHRIB),
                isha = parseTime(timings.isha, PrayerType.ISHA),
                calculationMethod = settings.calculationMethod,
                juristicMethod = settings.juristicMethod,
                sourceDescription = "مواقيت Aladhan API • ${settings.calculationMethod.arabicName}"
            )
            Result.success(result)
        } catch (e: Exception) {
            // Graceful fallback to offline astronomical calculation
            fallbackProvider.getPrayerTimes(date, location, settings).map { offlineTimes ->
                offlineTimes.copy(
                    sourceDescription = "${offlineTimes.sourceDescription} (بدون إنترنت - بديل فلكي)"
                )
            }
        }
    }

    private fun mapMethodToAladhanId(method: CalculationMethod): Int = when (method) {
        CalculationMethod.MWL -> 3
        CalculationMethod.UMM_AL_QURA -> 4
        CalculationMethod.EGYPTIAN -> 5
        CalculationMethod.KARACHI -> 1
        CalculationMethod.ISNA -> 2
        CalculationMethod.DUBAI -> 16
        CalculationMethod.KUWAIT -> 9
        CalculationMethod.QATAR -> 10
        CalculationMethod.TEHRAN -> 7
    }
}
