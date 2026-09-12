package com.example.data.calculation

import com.example.data.model.CalculationMethod
import com.example.data.model.DayPrayerTimes
import com.example.data.model.JuristicMethod
import com.example.data.model.LocationData
import com.example.data.model.PrayerType
import com.example.data.model.UserSettings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object AstronomicalCalculator {

    private const val RAD_TO_DEG = 180.0 / Math.PI
    private const val DEG_TO_RAD = Math.PI / 180.0

    private fun dsin(d: Double) = sin(d * DEG_TO_RAD)
    private fun dcos(d: Double) = cos(d * DEG_TO_RAD)
    private fun dtan(d: Double) = tan(d * DEG_TO_RAD)
    private fun darcsin(x: Double) = asin(x.coerceIn(-1.0, 1.0)) * RAD_TO_DEG
    private fun darccos(x: Double) = acos(x.coerceIn(-1.0, 1.0)) * RAD_TO_DEG
    private fun darctan(x: Double) = atan(x) * RAD_TO_DEG
    private fun darctan2(y: Double, x: Double) = atan2(y, x) * RAD_TO_DEG

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /**
     * Compute Sun coordinates: declination (degrees) and Equation of Time (hours)
     */
    private data class SunCoordinates(val declination: Double, val equationOfTime: Double)

    private fun sunCoordinates(julianDay: Double): SunCoordinates {
        val d = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))

        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(darctan2(dcos(e) * dsin(l), dcos(l))) / 15.0
        val decl = darcsin(dsin(e) * dsin(l))
        val eqt = q / 15.0 - ra
        return SunCoordinates(decl, eqt)
    }

    /**
     * Compute solar transit (Dhuhr) in fractional hours
     */
    private fun midDay(julianDay: Double, longitude: Double, timezone: Double): Double {
        val sun = sunCoordinates(julianDay)
        return fixHour(12.0 + timezone - longitude / 15.0 - sun.equationOfTime)
    }

    /**
     * Compute hour angle for a given sun angle altitude
     */
    private fun hourAngle(altitude: Double, latitude: Double, declination: Double): Double {
        val cosH = (dsin(altitude) - dsin(latitude) * dsin(declination)) /
                (dcos(latitude) * dcos(declination))
        return if (cosH < -1.0) 180.0 else if (cosH > 1.0) 0.0 else darccos(cosH)
    }

    /**
     * Compute Asr hour angle given juristic shadow factor (1.0 for Shafi'i, 2.0 for Hanafi)
     */
    private fun asrHourAngle(factor: Double, latitude: Double, declination: Double): Double {
        val delta = abs(latitude - declination)
        val altitude = darctan(1.0 / (factor + dtan(delta)))
        return hourAngle(altitude, latitude, declination)
    }

    fun calculate(
        date: LocalDate,
        location: LocationData,
        settings: UserSettings
    ): DayPrayerTimes {
        val zone = ZoneId.systemDefault()
        val zonedDateTime = date.atStartOfDay(zone)
        val timezoneOffsetHours = zone.rules.getOffset(zonedDateTime.toInstant()).totalSeconds / 3600.0

        val jd = julianDate(date.year, date.monthValue, date.dayOfMonth)
        val sun = sunCoordinates(jd)
        val dhuhrFraction = midDay(jd, location.longitude, timezoneOffsetHours)

        // Sunrise and Sunset (sun disk angle -0.8333)
        val sunAngle = -0.8333
        val sunriseHourAngle = hourAngle(sunAngle, location.latitude, sun.declination)
        val sunriseFraction = fixHour(dhuhrFraction - sunriseHourAngle / 15.0)
        val sunsetFraction = fixHour(dhuhrFraction + sunriseHourAngle / 15.0)

        // Fajr
        val method = settings.calculationMethod
        val fajrAngle = -method.fajrAngle
        var fajrHourAngle = hourAngle(fajrAngle, location.latitude, sun.declination)
        // High latitude fallback if twilight does not occur
        if (fajrHourAngle <= 0.0 || fajrHourAngle >= 180.0) {
            val nightLength = fixHour(sunriseFraction - sunsetFraction)
            fajrHourAngle = (nightLength * (method.fajrAngle / 60.0)) * 15.0
        }
        val fajrFraction = fixHour(dhuhrFraction - fajrHourAngle / 15.0)

        // Asr
        val asrHA = asrHourAngle(settings.juristicMethod.shadowFactor, location.latitude, sun.declination)
        val asrFraction = fixHour(dhuhrFraction + asrHA / 15.0)

        // Maghrib is normally at sunset + buffer (usually 1-2 min for safety)
        val maghribFraction = fixHour(sunsetFraction + 2.0 / 60.0)

        // Isha
        val ishaFraction = if (method.ishaIntervalMins > 0) {
            fixHour(maghribFraction + method.ishaIntervalMins / 60.0)
        } else {
            val ishaAngle = -method.ishaAngle
            var ishaHA = hourAngle(ishaAngle, location.latitude, sun.declination)
            if (ishaHA <= 0.0 || ishaHA >= 180.0) {
                val nightLength = fixHour(sunriseFraction - sunsetFraction)
                ishaHA = (nightLength * (method.ishaAngle / 60.0)) * 15.0
            }
            fixHour(dhuhrFraction + ishaHA / 15.0)
        }

        // Apply manual minute offsets
        fun applyOffset(fractionHours: Double, type: PrayerType): LocalTime {
            val offsetMins = settings.minuteAdjustments[type] ?: 0
            val totalSeconds = ((fractionHours * 3600.0).toLong() + offsetMins * 60)
            val fixedSeconds = ((totalSeconds % 86400) + 86400) % 86400
            val hours = (fixedSeconds / 3600).toInt()
            val minutes = ((fixedSeconds % 3600) / 60).toInt()
            val seconds = (fixedSeconds % 60).toInt()
            return LocalTime.of(hours, minutes, seconds)
        }

        val fajrTime = applyOffset(fajrFraction, PrayerType.FAJR)
        val sunriseTime = applyOffset(sunriseFraction, PrayerType.SUNRISE)
        val dhuhrTime = applyOffset(dhuhrFraction + 1.0 / 60.0, PrayerType.DHUHR) // +1 min after zenith
        val asrTime = applyOffset(asrFraction, PrayerType.ASR)
        val maghribTime = applyOffset(maghribFraction, PrayerType.MAGHRIB)
        val ishaTime = applyOffset(ishaFraction, PrayerType.ISHA)

        val hijriDateStr = calculateHijriDate(date)
        val gregorianFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))

        return DayPrayerTimes(
            date = date,
            hijriDate = hijriDateStr,
            gregorianDate = date.format(gregorianFormatter),
            location = location,
            fajr = fajrTime,
            sunrise = sunriseTime,
            dhuhr = dhuhrTime,
            asr = asrTime,
            maghrib = maghribTime,
            isha = ishaTime,
            calculationMethod = method,
            juristicMethod = settings.juristicMethod,
            sourceDescription = "${method.arabicName} • ${settings.juristicMethod.arabicName}"
        )
    }

    /**
     * Accurate Tabular / Umm Al-Qura compatible Hijri date converter
     */
    fun calculateHijriDate(date: LocalDate): String {
        var jd = julianDate(date.year, date.monthValue, date.dayOfMonth).toLong()
        val l = jd - 1948440 + 10632
        val n = ((l - 1) / 10631).toInt()
        val l2 = l - 10631 * n + 354
        val j = (((10985 - l2) / 5316).toInt()) * (((50 * l2) / 17719).toInt()) +
                ((l2 / 5670).toInt()) * (((43 * l2) / 15238).toInt())
        val l3 = l2 - (((30 - j) / 15).toInt()) * (((17719 * j) / 50).toInt()) -
                ((j / 16).toInt()) * (((15238 * j) / 43).toInt()) + 29
        val m = ((24 * l3) / 709).toInt()
        val day = l3 - ((709 * m) / 24).toInt()
        val year = 30 * n + j - 30

        val hijriMonths = arrayOf(
            "محرّم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوّال", "ذو القعدة", "ذو الحجة"
        )
        val monthName = if (m in 1..12) hijriMonths[m - 1] else "شهر $m"
        return "$day $monthName $year هـ"
    }

    /**
     * Computes the great-circle Qibla direction (azimuth in degrees from True North)
     * and distance to Kaaba in kilometers.
     * Kaaba: Lat 21.422487 N, Lon 39.826206 E
     */
    data class QiblaInfo(
        val qiblaBearing: Float, // 0 to 360 degrees
        val distanceKm: Double
    )

    fun calculateQibla(latitude: Double, longitude: Double): QiblaInfo {
        val kaabaLat = 21.422487
        val kaabaLon = 39.826206

        val phi1 = latitude * DEG_TO_RAD
        val phi2 = kaabaLat * DEG_TO_RAD
        val deltaLambda = (kaabaLon - longitude) * DEG_TO_RAD

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        var qibla = atan2(y, x) * RAD_TO_DEG
        qibla = (qibla + 360.0) % 360.0

        // Distance using Haversine formula
        val dLat = (kaabaLat - latitude) * DEG_TO_RAD
        val dLon = (kaabaLon - longitude) * DEG_TO_RAD
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(phi1) * cos(phi2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        val distance = 6371.0 * c

        return QiblaInfo(qibla.toFloat(), distance)
    }
}
