package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.example.data.model.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    val PRESET_CITIES = listOf(
        // === الجزائر (Wilayas & Cities of Algeria) ===
        LocationData("الجزائر العاصمة", "الجزائر", 36.7538, 3.0588),
        LocationData("وهران", "الجزائر", 35.6987, -0.6349),
        LocationData("قسنطينة", "الجزائر", 36.3650, 6.6147),
        LocationData("عنابة", "الجزائر", 36.9000, 7.7667),
        LocationData("سطيف", "الجزائر", 36.1898, 5.4108),
        LocationData("باتنة", "الجزائر", 35.5559, 6.1741),
        LocationData("البليدة", "الجزائر", 36.4700, 2.8300),
        LocationData("تلمسان", "الجزائر", 34.8783, -1.3150),
        LocationData("الشلف", "الجزائر", 36.1652, 1.3345),
        LocationData("بجاية", "الجزائر", 36.7559, 5.0843),
        LocationData("بسكرة", "الجزائر", 34.8504, 5.7281),
        LocationData("بشار", "الجزائر", 31.6167, -2.2167),
        LocationData("البويرة", "الجزائر", 36.3749, 3.9020),
        LocationData("تمنراست", "الجزائر", 22.7850, 5.5228),
        LocationData("تبسة", "الجزائر", 35.4042, 8.1242),
        LocationData("تيارت", "الجزائر", 35.3710, 1.3170),
        LocationData("تيزي وزو", "الجزائر", 36.7118, 4.0459),
        LocationData("الجلفة", "الجزائر", 34.6728, 3.2630),
        LocationData("جيجل", "الجزائر", 36.8206, 5.7667),
        LocationData("سكيكدة", "الجزائر", 36.8762, 6.9092),
        LocationData("سيدي بلعباس", "الجزائر", 35.1899, -0.6309),
        LocationData("قالمة", "الجزائر", 36.4621, 7.4261),
        LocationData("المدية", "الجزائر", 36.2642, 2.7539),
        LocationData("مستغانم", "الجزائر", 35.9312, 0.0892),
        LocationData("المسيلة", "الجزائر", 35.7058, 4.5419),
        LocationData("معسكر", "الجزائر", 35.3950, 0.1403),
        LocationData("ورقلة", "الجزائر", 31.9493, 5.3250),
        LocationData("البيض", "الجزائر", 33.6833, 1.0167),
        LocationData("إليزي", "الجزائر", 26.4833, 8.4667),
        LocationData("برج بوعريريج", "الجزائر", 36.0732, 4.7611),
        LocationData("بومرداس", "الجزائر", 36.7664, 3.4772),
        LocationData("الطارف", "الجزائر", 36.7672, 8.3139),
        LocationData("تندوف", "الجزائر", 27.6761, -8.1478),
        LocationData("تيسمسيلت", "الجزائر", 35.6072, 1.8108),
        LocationData("الوادي", "الجزائر", 33.3683, 6.8674),
        LocationData("خنشلة", "الجزائر", 35.4358, 7.1433),
        LocationData("سوق أهراس", "الجزائر", 36.2864, 7.9511),
        LocationData("تيبازة", "الجزائر", 36.5925, 2.4475),
        LocationData("ميلة", "الجزائر", 36.4503, 6.2644),
        LocationData("عين الدفلى", "الجزائر", 36.2641, 1.9679),
        LocationData("النعامة", "الجزائر", 33.2667, -0.3167),
        LocationData("عين تموشنت", "الجزائر", 35.2975, -1.1403),
        LocationData("غرداية", "الجزائر", 32.4909, 3.6736),
        LocationData("غليزان", "الجزائر", 35.7372, 0.5558),
        LocationData("الأغواط", "الجزائر", 33.8000, 2.8651),
        LocationData("أدرار", "الجزائر", 27.8743, -0.2939),
        LocationData("تقرت", "الجزائر", 33.1053, 6.0581),
        LocationData("جانت", "الجزائر", 24.5550, 9.4850),
        LocationData("تيميمون", "الجزائر", 29.2639, 0.2310),

        // === المملكة العربية السعودية ===
        LocationData("مكة المكرمة", "المملكة العربية السعودية", 21.4225, 39.8262),
        LocationData("المدينة المنورة", "المملكة العربية السعودية", 24.4672, 39.6111),
        LocationData("الرياض", "المملكة العربية السعودية", 24.7136, 46.6753),
        LocationData("جدة", "المملكة العربية السعودية", 21.5433, 39.1728),
        LocationData("الدمام", "المملكة العربية السعودية", 26.4207, 50.0888),
        LocationData("الطائف", "المملكة العربية السعودية", 21.2854, 40.4222),
        LocationData("تبوك", "المملكة العربية السعودية", 28.3835, 36.5662),
        LocationData("بريدة", "المملكة العربية السعودية", 26.3260, 43.9750),
        LocationData("أبها", "المملكة العربية السعودية", 18.2164, 42.5053),
        LocationData("خميس مشيط", "المملكة العربية السعودية", 18.3000, 42.7333),
        LocationData("حائل", "المملكة العربية السعودية", 27.5114, 41.7208),
        LocationData("نجران", "المملكة العربية السعودية", 17.4924, 44.1277),
        LocationData("جازان", "المملكة العربية السعودية", 16.8892, 42.5511),
        LocationData("ينبع", "المملكة العربية السعودية", 24.0895, 38.0637),
        LocationData("الجبيل", "المملكة العربية السعودية", 27.0112, 49.6582),
        LocationData("الهفوف", "المملكة العربية السعودية", 25.3647, 49.5878),
        LocationData("الخرج", "المملكة العربية السعودية", 24.1554, 47.3119),

        // === مصر ===
        LocationData("القاهرة", "مصر", 30.0444, 31.2357),
        LocationData("الإسكندرية", "مصر", 31.2001, 29.9187),
        LocationData("الجيزة", "مصر", 30.0131, 31.2089),
        LocationData("بورسعيد", "مصر", 31.2653, 32.3019),
        LocationData("السويس", "مصر", 29.9668, 32.5498),
        LocationData("طنطا", "مصر", 30.7865, 31.0004),
        LocationData("المنصورة", "مصر", 31.0409, 31.3785),
        LocationData("أسيوط", "مصر", 27.1783, 31.1859),
        LocationData("الإسماعيلية", "مصر", 30.5965, 32.2715),
        LocationData("الفيوم", "مصر", 29.3084, 30.8428),
        LocationData("الزقازيق", "مصر", 30.5877, 31.5020),
        LocationData("دمياط", "مصر", 31.4175, 31.8144),
        LocationData("أسوان", "مصر", 24.0889, 32.8998),
        LocationData("الأقصر", "مصر", 25.6872, 32.6396),
        LocationData("شرم الشيخ", "مصر", 27.9158, 34.3299),
        LocationData("الغردقة", "مصر", 27.2579, 33.8116),

        // === المغرب ===
        LocationData("الرباط", "المغرب", 34.0209, -6.8416),
        LocationData("الدار البيضاء", "المغرب", 33.5731, -7.5898),
        LocationData("فاس", "المغرب", 34.0331, -5.0003),
        LocationData("مراكش", "المغرب", 31.6295, -7.9811),
        LocationData("طنجة", "المغرب", 35.7595, -5.8340),
        LocationData("أكادير", "المغرب", 30.4278, -9.5981),
        LocationData("مكناس", "المغرب", 33.8938, -5.5473),
        LocationData("وجدة", "المغرب", 34.6867, -1.9114),
        LocationData("القنيطرة", "المغرب", 34.2610, -6.5802),
        LocationData("تطوان", "المغرب", 35.5889, -5.3626),
        LocationData("العيون", "المغرب", 27.1536, -13.2033),
        LocationData("آسفي", "المغرب", 32.2994, -9.2372),

        // === تونس ===
        LocationData("تونس العاصمة", "تونس", 36.8065, 10.1815),
        LocationData("صفاقس", "تونس", 34.7406, 10.7603),
        LocationData("سوسة", "تونس", 35.8256, 10.6369),
        LocationData("القيروان", "تونس", 35.6781, 10.0963),
        LocationData("بنزرت", "تونس", 37.2744, 9.8739),
        LocationData("قابس", "تونس", 33.8815, 10.0982),
        LocationData("مدنين", "تونس", 33.3549, 10.4959),
        LocationData("نابل", "تونس", 36.4561, 10.7376),
        LocationData("المنستير", "تونس", 35.7779, 10.8262),

        // === فلسطين والأردن ===
        LocationData("القدس الشريف", "فلسطين", 31.7683, 35.2137),
        LocationData("غزة", "فلسطين", 31.5017, 34.4668),
        LocationData("رام الله", "فلسطين", 31.9038, 35.2034),
        LocationData("الخليل", "فلسطين", 31.5326, 35.0998),
        LocationData("نابلس", "فلسطين", 32.2211, 35.2544),
        LocationData("جنين", "فلسطين", 32.4594, 35.3009),
        LocationData("عمّان", "الأردن", 31.9454, 35.9284),
        LocationData("إربد", "الأردن", 32.5568, 35.8469),
        LocationData("الزرقاء", "الأردن", 32.0728, 36.0880),
        LocationData("العقبة", "الأردن", 29.5321, 35.0063),

        // === دول الخليج العربي ===
        LocationData("أبوظبي", "الإمارات العربية المتحدة", 24.4539, 54.3773),
        LocationData("دبي", "الإمارات العربية المتحدة", 25.2048, 55.2708),
        LocationData("الشارقة", "الإمارات العربية المتحدة", 25.3463, 55.4209),
        LocationData("عجمان", "الإمارات العربية المتحدة", 25.4052, 55.5136),
        LocationData("رأس الخيمة", "الإمارات العربية المتحدة", 25.7895, 55.9432),
        LocationData("الفجيرة", "الإمارات العربية المتحدة", 25.1288, 56.3265),
        LocationData("مدينة الكويت", "الكويت", 29.3759, 47.9774),
        LocationData("الأحمدي", "الكويت", 29.0769, 48.0839),
        LocationData("الدوحة", "قطر", 25.2854, 51.5310),
        LocationData("الريان", "قطر", 25.2919, 51.4244),
        LocationData("المنامة", "البحرين", 26.2285, 50.5860),
        LocationData("المحرق", "البحرين", 26.2572, 50.6119),
        LocationData("مسقط", "عُمان", 23.5880, 58.3829),
        LocationData("صلالة", "عُمان", 17.0151, 54.0924),
        LocationData("صحار", "عُمان", 24.3644, 56.7468),

        // === العراق وبلاد الشام ===
        LocationData("بغداد", "العراق", 33.3152, 44.3661),
        LocationData("البصرة", "العراق", 30.5085, 47.7804),
        LocationData("الموصل", "العراق", 36.3400, 43.1300),
        LocationData("أربيل", "العراق", 36.1911, 44.0091),
        LocationData("النجف الأشرف", "العراق", 32.0259, 44.3463),
        LocationData("كربلاء المقدسة", "العراق", 32.6160, 44.0249),
        LocationData("كركوك", "العراق", 35.4681, 44.3922),
        LocationData("السليمانية", "العراق", 35.5570, 45.4359),
        LocationData("دمشق", "سوريا", 33.5138, 36.2765),
        LocationData("حلب", "سوريا", 36.2021, 37.1343),
        LocationData("حمص", "سوريا", 34.7324, 36.7137),
        LocationData("اللاذقية", "سوريا", 35.5317, 35.7900),
        LocationData("بيروت", "لبنان", 33.8938, 35.5018),
        LocationData("طرابلس", "لبنان", 34.4367, 35.8497),
        LocationData("صيدا", "لبنان", 33.5631, 35.3689),

        // === اليمن وليبيا والسودان وموريتانيا ===
        LocationData("صنعاء", "اليمن", 15.3694, 44.1910),
        LocationData("عدن", "اليمن", 12.7797, 45.0367),
        LocationData("تعز", "اليمن", 13.5795, 44.0209),
        LocationData("الحديدة", "اليمن", 14.7978, 42.9545),
        LocationData("المكلا", "اليمن", 14.5425, 49.1242),
        LocationData("طرابلس", "ليبيا", 32.8872, 13.1913),
        LocationData("بنغازي", "ليبيا", 32.1167, 20.0667),
        LocationData("مصراتة", "ليبيا", 32.3754, 15.0925),
        LocationData("البيضاء", "ليبيا", 32.7628, 21.7551),
        LocationData("الخرطوم", "السودان", 15.5007, 32.5599),
        LocationData("أم درمان", "السودان", 15.6505, 32.4800),
        LocationData("بورتسودان", "السودان", 19.6175, 37.2164),
        LocationData("نواكشوط", "موريتانيا", 18.0735, -15.9582),
        LocationData("نواذيبو", "موريتانيا", 20.9310, -17.0347),
        LocationData("مقديشو", "الصومال", 2.0469, 45.3182),
        LocationData("جيبوتي", "جيبوتي", 11.5721, 43.1456),

        // === تركيا والدول الإسلامية ===
        LocationData("إسطنبول", "تركيا", 41.0082, 28.9784),
        LocationData("أنقرة", "تركيا", 39.9334, 32.8597),
        LocationData("بورصة", "تركيا", 40.1885, 29.0610),
        LocationData("قونية", "تركيا", 37.8746, 32.4932),
        LocationData("إسلام آباد", "باكستان", 33.6844, 73.0479),
        LocationData("كراتشي", "باكستان", 24.8607, 67.0011),
        LocationData("لاهور", "باكستان", 31.5204, 74.3587),
        LocationData("دكا", "بنغلاديش", 23.8103, 90.4125),
        LocationData("جاكرتا", "إندونيسيا", -6.2088, 106.8456),
        LocationData("سورابايا", "إندونيسيا", -7.2575, 112.7521),
        LocationData("كوالالمبور", "ماليزيا", 3.1390, 101.6869),
        LocationData("طهران", "إيران", 35.6892, 51.3890),
        LocationData("طشقند", "أوزبكستان", 41.2995, 69.2401),
        LocationData("سمرقند", "أوزبكستان", 39.6542, 66.9597),
        LocationData("باكو", "أذربيجان", 40.4093, 49.8671),
        LocationData("سراييفو", "البوسنة والهرسك", 43.8563, 18.4131),

        // === مدن عالمية بارزة ===
        LocationData("لندن", "المملكة المتحدة", 51.5074, -0.1278),
        LocationData("مانشستر", "المملكة المتحدة", 53.4808, -2.2426),
        LocationData("باريس", "فرنسا", 48.8566, 2.3522),
        LocationData("مرسيليا", "فرنسا", 43.2965, 5.3698),
        LocationData("برلين", "ألمانيا", 52.5200, 13.4050),
        LocationData("فرانكفورت", "ألمانيا", 50.1109, 8.6821),
        LocationData("مدريد", "إسبانيا", 40.4168, -3.7038),
        LocationData("روما", "إيطاليا", 41.9028, 12.4964),
        LocationData("فيينا", "النمسا", 48.2082, 16.3738),
        LocationData("أمستردام", "هولندا", 52.3676, 4.9041),
        LocationData("بروكسل", "بلجيكا", 50.8503, 4.3517),
        LocationData("نيويورك", "الولايات المتحدة", 40.7128, -74.0060),
        LocationData("شيكاغو", "الولايات المتحدة", 41.8781, -87.6298),
        LocationData("لوس أنجلوس", "الولايات المتحدة", 34.0522, -118.2437),
        LocationData("تورونتو", "كندا", 43.6532, -79.3832),
        LocationData("مونتريال", "كندا", 45.5017, -73.5673),
        LocationData("سيدني", "أستراليا", -33.8688, 151.2093),
        LocationData("ملبورن", "أستراليا", -37.8136, 144.9631)
    )

    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gps = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
        val net = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }
        return gps || net
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(context: Context): LocationData? = withContext(Dispatchers.IO) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            return@withContext null
        }

        // Try last known location first
        var bestLocation: Location? = null
        if (isGpsEnabled) {
            val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) bestLocation = loc
        }
        if (isNetworkEnabled) {
            val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                bestLocation = loc
            }
        }

        if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 1000 * 60 * 30) {
            // Fresh enough (less than 30 mins old)
            return@withContext resolveLocationData(context, bestLocation)
        }

        // Otherwise request a single update
        try {
            val singleLoc = requestSingleLocationUpdate(locationManager) ?: bestLocation
            if (singleLoc != null) {
                resolveLocationData(context, singleLoc)
            } else {
                null
            }
        } catch (_: Exception) {
            bestLocation?.let { resolveLocationData(context, it) }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocationUpdate(locationManager: LocationManager): Location? =
        suspendCancellableCoroutine { continuation ->
            val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(null)
                }
            }

            try {
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                continuation.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    private fun resolveLocationData(context: Context, location: Location): LocationData {
        var cityName = "الموقع الحالي"
        var countryName = ""

        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("ar"))
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    cityName = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "موقعي"
                    countryName = addr.countryName ?: ""
                }
            }
        } catch (_: Exception) {
            // Offline geocoding fallback
        }

        return LocationData(
            cityName = cityName,
            countryName = countryName,
            latitude = location.latitude,
            longitude = location.longitude,
            isGps = true
        )
    }
}
