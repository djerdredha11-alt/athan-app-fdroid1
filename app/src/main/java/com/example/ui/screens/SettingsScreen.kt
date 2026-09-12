package com.example.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CalculationMethod
import com.example.data.model.JuristicMethod
import com.example.data.model.LocationData
import com.example.data.model.PrayerType
import com.example.service.LocationHelper
import com.example.ui.viewmodel.PrayerViewModel
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: PrayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val context = LocalContext.current

    // Dialog states
    var showMethodDialog by remember { mutableStateOf(false) }
    var showJuristicDialog by remember { mutableStateOf(false) }
    var showCitiesDialog by remember { mutableStateOf(false) }
    var showCustomCoordDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAdjustmentsDialog by remember { mutableStateOf(false) }

    // Audio test preview state
    var isPlayingTestAudio by remember { mutableStateOf(false) }
    var testPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            testPlayer?.stop()
            testPlayer?.release()
            testPlayer = null
        }
    }

    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomAudioUri(uri.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Location & Coordinates
        SettingsCard(title = "الموقع الجغرافي", icon = Icons.Default.LocationCity) {
            Text(
                text = "الموقع المحدد حالياً: ${settings.selectedLocation.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format(Locale.US, "خط العرض: %.4f • خط الطول: %.4f", settings.selectedLocation.latitude, settings.selectedLocation.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = { viewModel.requestGpsLocation() },
                    modifier = Modifier.weight(1f).testTag("btn_gps_location")
                ) {
                    Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحديد تلقائي")
                }

                OutlinedButton(
                    onClick = { showCitiesDialog = true },
                    modifier = Modifier.weight(1f).testTag("btn_city_list")
                ) {
                    Text("قائمة المدن")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = { showCustomCoordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.EditLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إدخال إحداثيات مخصصة (Lat/Lon)")
            }
        }

        // Section 2: Calculation & Juristic Methods
        SettingsCard(title = "طريقة الحساب والمذهب الفقهي", icon = Icons.Default.Scale) {
            SettingRowClickable(
                label = "طريقة الحساب الفلكي",
                value = settings.calculationMethod.arabicName,
                onClick = { showMethodDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingRowClickable(
                label = "المذهب الفقهي (صلاة العصر)",
                value = settings.juristicMethod.arabicName,
                onClick = { showJuristicDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingRowClickable(
                label = "تعديل دقائق المواقيت يدويًا",
                value = "ضبط الفجر، الظهر، العصر...",
                onClick = { showAdjustmentsDialog = true }
            )
        }

        // Section 3: Audio & Reminders
        SettingsCard(title = "الأذان والتنبيهات", icon = Icons.Default.Audiotrack) {
            // Audio source
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "صوت الأذان",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (settings.customAudioUri == null) "أذان 4002 (الافتراضي)" else "ملف صوتي مخصص من الجهاز",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    if (settings.customAudioUri != null) {
                        OutlinedButton(
                            onClick = { viewModel.setCustomAudioUri(null) },
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text("استعادة")
                        }
                    }
                    Button(
                        onClick = { audioFilePicker.launch("audio/*") }
                    ) {
                        Text("اختيار ملف")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Play / Stop Test Adhan
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "معاينة صوت الأذان",
                    style = MaterialTheme.typography.bodyMedium
                )
                FilledTonalButton(
                    onClick = {
                        if (isPlayingTestAudio) {
                            testPlayer?.stop()
                            testPlayer?.release()
                            testPlayer = null
                            isPlayingTestAudio = false
                        } else {
                            try {
                                val player = MediaPlayer().apply {
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_ALARM)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .build()
                                    )
                                    setVolume(settings.volume, settings.volume)
                                    if (settings.customAudioUri != null) {
                                        setDataSource(context, Uri.parse(settings.customAudioUri))
                                    } else {
                                        val afd = context.resources.openRawResourceFd(R.raw.adhan_4002)
                                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                        afd.close()
                                    }
                                    setOnCompletionListener {
                                        isPlayingTestAudio = false
                                    }
                                    prepare()
                                    start()
                                }
                                testPlayer = player
                                isPlayingTestAudio = true
                            } catch (e: Exception) {
                                isPlayingTestAudio = false
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlayingTestAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPlayingTestAudio) "إيقاف المعاينة" else "تجربة الأذان")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Volume Slider
            Text(
                text = "مستوى صوت الأذان: ${(settings.volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = settings.volume,
                onValueChange = { viewModel.updateVolume(it) },
                valueRange = 0.1f..1.0f,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Pre-alarm selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "تنبيه قبل الأذان",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (settings.preAlarmMinutes > 0) "تنبيه قبل ${settings.preAlarmMinutes} دقيقة" else "معطل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0, 5, 10, 15).forEach { mins ->
                        val isSelected = settings.preAlarmMinutes == mins
                        FilledTonalButton(
                            onClick = { viewModel.updatePreAlarmMinutes(mins) },
                            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = if (mins == 0) "إيقاف" else "$mins د",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Preferences & Offline/Online Engine
        SettingsCard(title = "التفضيلات والمصدر", icon = Icons.Default.Cloud) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تنسيق الوقت 24 ساعة",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (settings.is24HourFormat) "23:59" else "11:59 م",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.is24HourFormat,
                    onCheckedChange = { viewModel.updateTimeFormat(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "جلب المواقيت عبر الإنترنت (Aladhan API)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (settings.useOnlineApi) "مفعل (مع رجوع تلقائي للحساب الفلكي المحلي)" else "معطل (يعمل بحساب فلكي محلي دون إنترنت)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.useOnlineApi,
                    onCheckedChange = { viewModel.toggleOnlineApi(it) }
                )
            }
        }

        // Section 5: Open Source & Privacy Policy (F-Droid compliance)
        SettingsCard(title = "الخصوصية والمصدر المفتوح (F-Droid)", icon = Icons.Default.Lock) {
            Text(
                text = "هذا التطبيق مجاني، مفتوح المصدر وخالٍ تماماً من أي إعلانات أو متتبعات أو كود تجاري.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("سياسة الخصوصية")
                }

                OutlinedButton(
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("حول التطبيق والرخصة")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Dialog 1: Calculation Method Selector
    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = { Text("اختر طريقة الحساب الفلكي") },
            text = {
                LazyColumn {
                    items(CalculationMethod.entries) { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateCalculationMethod(method)
                                    showMethodDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = settings.calculationMethod == method,
                                onClick = {
                                    viewModel.updateCalculationMethod(method)
                                    showMethodDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = method.arabicName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMethodDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Dialog 2: Juristic Method
    if (showJuristicDialog) {
        AlertDialog(
            onDismissRequest = { showJuristicDialog = false },
            title = { Text("اختر المذهب الفقهي لحساب صلاة العصر") },
            text = {
                Column {
                    JuristicMethod.entries.forEach { method ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateJuristicMethod(method)
                                    showJuristicDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = settings.juristicMethod == method,
                                onClick = {
                                    viewModel.updateJuristicMethod(method)
                                    showJuristicDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = method.arabicName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showJuristicDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Dialog 3: Preset Cities Selector
    if (showCitiesDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredCities = remember(searchQuery) {
            if (searchQuery.isBlank()) LocationHelper.PRESET_CITIES
            else LocationHelper.PRESET_CITIES.filter {
                it.cityName.contains(searchQuery, ignoreCase = true) ||
                it.countryName.contains(searchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showCitiesDialog = false },
            title = { Text("اختر المدينة") },
            text = {
                Column(modifier = Modifier.height(350.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث عن مدينة...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn {
                        items(filteredCities) { city ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectManualCity(city)
                                        showCitiesDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = city.cityName, fontWeight = FontWeight.Bold)
                                    Text(text = city.countryName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCitiesDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog 4: Custom Coordinates
    if (showCustomCoordDialog) {
        var cityName by remember { mutableStateOf(settings.selectedLocation.cityName) }
        var countryName by remember { mutableStateOf(settings.selectedLocation.countryName) }
        var latText by remember { mutableStateOf(settings.selectedLocation.latitude.toString()) }
        var lonText by remember { mutableStateOf(settings.selectedLocation.longitude.toString()) }

        AlertDialog(
            onDismissRequest = { showCustomCoordDialog = false },
            title = { Text("إدخال إحداثيات مخصصة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cityName,
                        onValueChange = { cityName = it },
                        label = { Text("اسم المدينة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = countryName,
                        onValueChange = { countryName = it },
                        label = { Text("اسم الدولة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("خط العرض (Latitude)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("خط الطول (Longitude)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val lat = latText.toDoubleOrNull() ?: settings.selectedLocation.latitude
                    val lon = lonText.toDoubleOrNull() ?: settings.selectedLocation.longitude
                    viewModel.setCustomCoordinates(cityName, countryName, lat, lon)
                    showCustomCoordDialog = false
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomCoordDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Dialog 5: Minute Adjustments Dialog
    if (showAdjustmentsDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustmentsDialog = false },
            title = { Text("تعديل دقائق المواقيت يدويًا") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PrayerType.entries) { prayer ->
                        val currentAdj = settings.minuteAdjustments[prayer] ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = prayer.arabicName, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.setMinuteAdjustment(prayer, -1) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "تقليل دقيقة")
                                }
                                Text(
                                    text = if (currentAdj > 0) "+$currentAdj د" else "$currentAdj د",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(onClick = { viewModel.setMinuteAdjustment(prayer, +1) }) {
                                    Icon(Icons.Default.Add, contentDescription = "زيادة دقيقة")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAdjustmentsDialog = false }) {
                    Text("تم")
                }
            }
        )
    }

    // Dialog 6: Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("سياسة الخصوصية الصارمة") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = """
                        • خصوصية تامة 100%:
                        لا يقوم هذا التطبيق بجمع، تخزين، أو مشاركة أي بيانات شخصية أو معلومات تعريفية عن المستخدم.
                        
                        • بدون حساب وبدون تسجيل دخول:
                        لا يتطلب التطبيق أي بريد إلكتروني، رقم هاتف، أو اسم مستخدم.
                        
                        • بدون تتبع أو إعلانات:
                        لا يحتوي التطبيق على Google Analytics أو Firebase Analytics أو أي شبكة إعلانية أو متتبع من أي طرف ثالث.
                        
                        • الموقع الجغرافي:
                        يتم استخدام إحداثيات الموقع حصريًا على جهازك لحساب أوقات الصلاة الدقيقة وتحديد اتجاه القبلة، ولا يتم إرسال موقعك إلى أي خادم خارجي.
                        
                        • متوافق مع متطلبات متجر F-Droid للبرمجيات الحرة والمفتوحة المصدر.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("فهمت ذلك")
                }
            }
        )
    }

    // Dialog 7: About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("عن تطبيق أذان") },
            text = {
                Column {
                    Text(
                        text = "تطبيق أذان إسلامي حديث، خفيف، وسريع، مبني بنظام أندرويد الحديث (Jetpack Compose) وموجه لمستخدمي المصادر المفتوحة ومتجر F-Droid.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الرخصة: مفتوح المصدر بالكامل تحت رخصة MIT / GPLv3.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingRowClickable(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
