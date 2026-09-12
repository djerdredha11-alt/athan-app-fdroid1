package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("أذان", appName)
  }

  @Test
  fun `astronomical calculator calculates valid prayer times`() {
    val location = com.example.data.model.LocationData(
        cityName = "مكة المكرمة",
        countryName = "المملكة العربية السعودية",
        latitude = 21.4225,
        longitude = 39.8262
    )
    val settings = com.example.data.model.UserSettings(
        calculationMethod = com.example.data.model.CalculationMethod.UMM_AL_QURA,
        selectedLocation = location
    )
    val times = com.example.data.calculation.AstronomicalCalculator.calculate(
        java.time.LocalDate.of(2026, 9, 12),
        location,
        settings
    )

    // Fajr is before sunrise, sunrise before dhuhr, dhuhr before asr, asr before maghrib, maghrib before isha
    assert(times.fajr.isBefore(times.sunrise))
    assert(times.sunrise.isBefore(times.dhuhr))
    assert(times.dhuhr.isBefore(times.asr))
    assert(times.asr.isBefore(times.maghrib))
    assert(times.maghrib.isBefore(times.isha))
  }

  @Test
  fun `qibla calculation points accurately towards Kaaba`() {
    // From Cairo (approx Lat 30.0444, Lon 31.2357), Qibla bearing is approx 136 degrees
    val info = com.example.data.calculation.AstronomicalCalculator.calculateQibla(30.0444, 31.2357)
    assert(info.qiblaBearing in 130f..142f)
    assert(info.distanceKm > 1000.0 && info.distanceKm < 1500.0)
  }
}
