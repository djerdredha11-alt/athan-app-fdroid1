package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.NotificationMode
import com.example.data.model.PrayerType
import com.example.service.LocationHelper
import com.example.ui.components.CountdownHeroCard
import com.example.ui.components.DaySelectorBar
import com.example.ui.components.PrayerCard
import com.example.ui.viewmodel.PrayerViewModel

@Composable
fun HomeScreen(
    viewModel: PrayerViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showGpsEnableDialog by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            if (LocationHelper.isLocationServiceEnabled(context)) {
                viewModel.requestGpsLocation()
            } else {
                showGpsEnableDialog = true
            }
        }
    }

    // Immediately request permissions and GPS activation upon entering the app
    LaunchedEffect(Unit) {
        // 1. Notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Prompt for GPS immediately upon app launch
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            if (LocationHelper.isLocationServiceEnabled(context)) {
                viewModel.requestGpsLocation()
            } else {
                showGpsEnableDialog = true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Hero Countdown Card (Luxurious redesign)
            item {
                CountdownHeroCard(
                    dayTimes = uiState.selectedDayTimes,
                    countdown = if (uiState.isToday) uiState.countdown else null,
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.refresh() },
                    onLocationClick = onNavigateToSettings
                )
            }

            // 2. Day Stepper
            item {
                DaySelectorBar(
                    selectedDate = uiState.selectedDate,
                    isToday = uiState.isToday,
                    onPreviousDay = { viewModel.changeDateOffset(-1) },
                    onToday = { viewModel.changeDateOffset(0) },
                    onNextDay = { viewModel.changeDateOffset(1) }
                )
            }

            // 3. Prayer Cards
            val dayTimes = uiState.selectedDayTimes
            if (dayTimes != null) {
                val nextPrayer = if (uiState.isToday) uiState.countdown?.nextPrayer else null

                items(PrayerType.entries.size) { index ->
                    val prayer = PrayerType.entries[index]
                    val time = dayTimes.getTimeFor(prayer)
                    val formatted = viewModel.formatTime(time)
                    val isNext = (nextPrayer == prayer)
                    val notifMode = uiState.settings.prayerNotifications[prayer] ?: NotificationMode.FULL_ADHAN

                    PrayerCard(
                        prayerType = prayer,
                        formattedTime = formatted,
                        isNext = isNext,
                        notificationMode = notifMode,
                        onToggleNotification = { viewModel.togglePrayerNotification(prayer) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Prompt to enable GPS if disabled
    if (showGpsEnableDialog) {
        AlertDialog(
            onDismissRequest = { showGpsEnableDialog = false },
            icon = { Icon(Icons.Default.GpsFixed, contentDescription = null) },
            title = { Text("تفعيل خدمة الموقع (GPS)") },
            text = {
                Text("لضمان دقة مواقيت الصلاة واتجاه القبلة لموقعك الحالي، يرجى تشغيل خدمة الـ GPS في هاتفك.")
            },
            confirmButton = {
                Button(onClick = {
                    showGpsEnableDialog = false
                    try {
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }) {
                    Text("تشغيل الـ GPS")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGpsEnableDialog = false }) {
                    Text("لاحقاً")
                }
            }
        )
    }
}
