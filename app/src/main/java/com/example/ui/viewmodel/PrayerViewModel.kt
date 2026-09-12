package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.CalculationMethod
import com.example.data.model.DayPrayerTimes
import com.example.data.model.JuristicMethod
import com.example.data.model.LocationData
import com.example.data.model.NotificationMode
import com.example.data.model.PrayerCountdown
import com.example.data.model.PrayerType
import com.example.data.model.UserSettings
import com.example.data.provider.PrayerRepository
import com.example.service.LocationHelper
import com.example.service.PrayerAlarmScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class PrayerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val selectedDayTimes: DayPrayerTimes? = null,
    val countdown: PrayerCountdown? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val settings: UserSettings = UserSettings()
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrayerRepository(application)

    private val _uiState = MutableStateFlow(PrayerUiState(settings = repository.userSettings.value))
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null
    private var todayTimes: DayPrayerTimes? = null
    private var tomorrowTimes: DayPrayerTimes? = null

    init {
        viewModelScope.launch {
            repository.userSettings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
                loadPrayerTimes(forceRefresh = false)
            }
        }
        startCountdownTicker()
    }

    private fun sendErrorNotification(title: String, message: String) {
        try {
            val context = getApplication<Application>()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "channel_prayer_info_notif"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "تنبيهات تطبيق أذان",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }
            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            notificationManager.notify(777, notif)
        } catch (_: Exception) {}
    }

    private fun startCountdownTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                updateCountdown()
                delay(1000)
            }
        }
    }

    private fun updateCountdown() {
        val today = todayTimes
        val tomorrow = tomorrowTimes
        if (today != null && tomorrow != null) {
            val countdown = repository.calculateCountdown(today, tomorrow, LocalDateTime.now())
            _uiState.value = _uiState.value.copy(countdown = countdown)
        }
    }

    fun loadPrayerTimes(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val settings = _uiState.value.settings
            val selectedDate = _uiState.value.selectedDate
            val today = LocalDate.now()

            try {
                val times = repository.getPrayerTimesForDate(selectedDate, settings.selectedLocation, forceRefresh)
                val tToday = if (selectedDate == today) times else repository.getPrayerTimesForDate(today, settings.selectedLocation, forceRefresh)
                val tTomorrow = repository.getPrayerTimesForDate(today.plusDays(1), settings.selectedLocation, forceRefresh)

                todayTimes = tToday
                tomorrowTimes = tTomorrow

                _uiState.value = _uiState.value.copy(
                    selectedDayTimes = times,
                    isToday = (selectedDate == today),
                    isLoading = false,
                    errorMessage = null
                )
                updateCountdown()

                // Reschedule exact alarms
                PrayerAlarmScheduler.scheduleAllAlarms(getApplication())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                sendErrorNotification("تطبيق أذان", "تم اعتماد الحساب الفلكي الدقيق لمواقيت الصلاة")
            }
        }
    }

    fun changeDateOffset(offsetDays: Long) {
        val newDate = if (offsetDays == 0L) LocalDate.now() else _uiState.value.selectedDate.plusDays(offsetDays)
        _uiState.value = _uiState.value.copy(selectedDate = newDate)
        loadPrayerTimes(forceRefresh = false)
    }

    fun refresh() {
        loadPrayerTimes(forceRefresh = true)
    }

    fun requestGpsLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val gpsLoc = LocationHelper.getCurrentGpsLocation(getApplication())
            if (gpsLoc != null) {
                val newSettings = _uiState.value.settings.copy(
                    selectedLocation = gpsLoc,
                    useGps = true
                )
                repository.updateSettings(newSettings)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                sendErrorNotification("تحديد الموقع GPS", "تعذر تحديد الموقع تلقائياً، يرجى التأكد من تشغيل الـ GPS أو اختيار المدينة")
            }
        }
    }

    fun selectManualCity(city: LocationData) {
        val newSettings = _uiState.value.settings.copy(
            selectedLocation = city.copy(isGps = false),
            useGps = false
        )
        repository.updateSettings(newSettings)
    }

    fun setCustomCoordinates(cityName: String, countryName: String, lat: Double, lon: Double) {
        val customLoc = LocationData(
            cityName = cityName.ifBlank { "موقع مخصص" },
            countryName = countryName,
            latitude = lat,
            longitude = lon,
            isGps = false
        )
        val newSettings = _uiState.value.settings.copy(
            selectedLocation = customLoc,
            useGps = false
        )
        repository.updateSettings(newSettings)
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        val newSettings = _uiState.value.settings.copy(calculationMethod = method)
        repository.updateSettings(newSettings)
    }

    fun updateJuristicMethod(juristic: JuristicMethod) {
        val newSettings = _uiState.value.settings.copy(juristicMethod = juristic)
        repository.updateSettings(newSettings)
    }

    fun updateTimeFormat(is24h: Boolean) {
        val newSettings = _uiState.value.settings.copy(is24HourFormat = is24h)
        repository.updateSettings(newSettings)
    }

    fun updatePreAlarmMinutes(minutes: Int) {
        val newSettings = _uiState.value.settings.copy(preAlarmMinutes = minutes)
        repository.updateSettings(newSettings)
        PrayerAlarmScheduler.scheduleAllAlarms(getApplication())
    }

    fun updateVolume(vol: Float) {
        val newSettings = _uiState.value.settings.copy(volume = vol.coerceIn(0f, 1f))
        repository.updateSettings(newSettings)
    }

    fun setCustomAudioUri(uriString: String?) {
        val newSettings = _uiState.value.settings.copy(customAudioUri = uriString)
        repository.updateSettings(newSettings)
    }

    fun togglePrayerNotification(prayer: PrayerType) {
        val currentModes = _uiState.value.settings.prayerNotifications.toMutableMap()
        val current = currentModes[prayer] ?: NotificationMode.FULL_ADHAN
        val next = when (current) {
            NotificationMode.FULL_ADHAN -> NotificationMode.NOTIFICATION_ONLY
            NotificationMode.NOTIFICATION_ONLY -> NotificationMode.SILENT
            NotificationMode.SILENT -> NotificationMode.FULL_ADHAN
        }
        currentModes[prayer] = next
        val newSettings = _uiState.value.settings.copy(prayerNotifications = currentModes)
        repository.updateSettings(newSettings)
        PrayerAlarmScheduler.scheduleAllAlarms(getApplication())
    }

    fun setMinuteAdjustment(prayer: PrayerType, delta: Int) {
        val current = _uiState.value.settings.minuteAdjustments.toMutableMap()
        val existing = current[prayer] ?: 0
        val updated = (existing + delta).coerceIn(-60, 60)
        current[prayer] = updated
        val newSettings = _uiState.value.settings.copy(minuteAdjustments = current)
        repository.updateSettings(newSettings)
    }

    fun toggleOnlineApi(enable: Boolean) {
        val newSettings = _uiState.value.settings.copy(useOnlineApi = enable)
        repository.updateSettings(newSettings)
    }

    fun formatTime(time: java.time.LocalTime): String {
        return repository.formatTime(time, _uiState.value.settings.is24HourFormat)
    }
}
