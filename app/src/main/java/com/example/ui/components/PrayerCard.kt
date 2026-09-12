package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationMode
import com.example.data.model.PrayerType

@Composable
fun PrayerCard(
    prayerType: PrayerType,
    formattedTime: String,
    isNext: Boolean,
    notificationMode: NotificationMode,
    onToggleNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prayerIcon: ImageVector = when (prayerType) {
        PrayerType.FAJR -> Icons.Default.Brightness6
        PrayerType.SUNRISE -> Icons.Default.WbTwilight
        PrayerType.DHUHR -> Icons.Default.WbSunny
        PrayerType.ASR -> Icons.Default.Brightness5
        PrayerType.MAGHRIB -> Icons.Default.Brightness6
        PrayerType.ISHA -> Icons.Default.DarkMode
    }

    val cardBg by animateColorAsState(
        targetValue = if (isNext) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "card_bg"
    )

    val borderColor = if (isNext) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isNext) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("prayer_card_${prayerType.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Right side (RTL): Prayer Icon + Arabic Name + Next tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isNext) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = prayerIcon,
                        contentDescription = prayerType.arabicName,
                        tint = if (isNext) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = prayerType.arabicName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isNext) {
                        Text(
                            text = "الصلاة القادمة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Left side (RTL): Time + Notification Toggle Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onToggleNotification,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("btn_notif_${prayerType.name.lowercase()}")
                ) {
                    val notifIcon = when (notificationMode) {
                        NotificationMode.FULL_ADHAN -> Icons.Default.NotificationsActive
                        NotificationMode.NOTIFICATION_ONLY -> Icons.Default.Notifications
                        NotificationMode.SILENT -> Icons.Default.NotificationsOff
                    }
                    val notifTint = when (notificationMode) {
                        NotificationMode.FULL_ADHAN -> MaterialTheme.colorScheme.primary
                        NotificationMode.NOTIFICATION_ONLY -> MaterialTheme.colorScheme.secondary
                        NotificationMode.SILENT -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    }
                    Icon(
                        imageVector = notifIcon,
                        contentDescription = notificationMode.arabicTitle,
                        tint = notifTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
