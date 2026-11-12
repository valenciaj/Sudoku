package com.valenciaj.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import com.valenciaj.sudoku.R
import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.DateFormat
import java.util.Date
import com.valenciaj.sudoku.core.Achievement
import com.valenciaj.sudoku.core.Stats

@Composable
fun AchievementsScreen(stats: Stats, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val unlocked = Achievement.entries.count { it.isUnlocked(stats) }
    val total = Achievement.entries.size
    Column(
        Modifier.widthIn(max = 960.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.achievements), onBack)
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = colors.surfaceContainerLow) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.achievements_count, unlocked, total), style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { unlocked.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    strokeCap = StrokeCap.Round,
                    trackColor = colors.surfaceContainerHighest,
                    drawStopIndicator = {},
                )
            }
        }
        AdaptiveList(Achievement.entries.sortedByDescending { it.isUnlocked(stats) }) { a ->
            val done = a.isUnlocked(stats)
            Surface(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = if (done) colors.surfaceContainerLowest else colors.surfaceContainerLow,
                border = if (done) BorderStroke(1.dp, colors.outlineVariant) else null,
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).background(if (done) brandGradient() else SolidColor(colors.surfaceContainerHighest), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            if (done) R.drawable.ic_emoji_events else R.drawable.ic_lock,
                            null,
                            tint = if (done) Color.White else colors.onSurfaceVariant,
                        )
                    }
                    Column(Modifier.padding(start = 14.dp).alpha(if (done) 1f else 0.6f)) {
                        Text(a.title(), style = MaterialTheme.typography.titleMedium)
                        Text(a.description(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    cloud: CloudState,
    cloudActions: CloudActions,
    onBack: () -> Unit,
) {
    Column(
        Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader(stringResource(R.string.settings), onBack)

        SettingsGroup(stringResource(R.string.theme)) {
            SegmentedPills(
                ThemeMode.entries.map { it.text() },
                settings.themeMode.ordinal,
                { onChange(settings.copy(themeMode = ThemeMode.entries[it])) },
            )
            Text(stringResource(R.string.color), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Accent.entries.forEach { accent ->
                    val selected = settings.accent == accent
                    val ring by animateColorAsState(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, label = "ring")
                    Box(
                        Modifier
                            .size(48.dp)
                            .border(3.dp, ring, CircleShape)
                            .padding(5.dp)
                            .background(accent.color, CircleShape)
                            .clickable { onChange(settings.copy(accent = accent)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) AppIcon(R.drawable.ic_check, accent.label, Modifier.size(20.dp), tint = Color.White)
                    }
                }
            }
        }

        SettingsGroup(stringResource(R.string.highlight)) {
            SwitchRow(stringResource(R.string.highlight_related), settings.highlightRelated) {
                onChange(settings.copy(highlightRelated = it))
            }
            SwitchRow(stringResource(R.string.highlight_same), settings.highlightSame) {
                onChange(settings.copy(highlightSame = it))
            }
        }

        SettingsGroup(stringResource(R.string.feedback)) {
            SwitchRow(stringResource(R.string.sound), settings.sound) { onChange(settings.copy(sound = it)) }
            SwitchRow(stringResource(R.string.vibration), settings.vibration) { onChange(settings.copy(vibration = it)) }
        }

        SettingsGroup(stringResource(R.string.accessibility)) {
            Text(stringResource(R.string.digit_size), style = MaterialTheme.typography.titleSmall)
            SegmentedPills(
                DigitSize.entries.map { it.text() },
                settings.digitSize.ordinal,
                { onChange(settings.copy(digitSize = DigitSize.entries[it])) },
            )
            SwitchRow(stringResource(R.string.color_blind), settings.colorBlind) { onChange(settings.copy(colorBlind = it)) }
        }

        SettingsGroup(stringResource(R.string.reminder)) { ReminderSection(settings, onChange) }

        SettingsGroup(stringResource(R.string.cloud)) { CloudSection(cloud, cloudActions) }
    }
}

@Composable
private fun DigitSize.text(): String = stringResource(
    when (this) {
        DigitSize.NORMAL -> R.string.size_normal
        DigitSize.LARGE -> R.string.size_large
        DigitSize.XLARGE -> R.string.size_xlarge
    },
)

/** Recordatorio diario: en Android 13+ hay que pedir permiso de notificaciones. */
@Composable
private fun ReminderSection(settings: Settings, onChange: (Settings) -> Unit) {
    // Sin registro de resultados (capturas de pantalla) no se puede pedir el permiso.
    val permission = if (LocalActivityResultRegistryOwner.current != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onChange(settings.copy(reminder = true))
        }
    } else {
        null
    }
    SwitchRow(stringResource(R.string.reminder_switch), settings.reminder) { on ->
        if (on && Build.VERSION.SDK_INT >= 33 && permission != null) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onChange(settings.copy(reminder = on))
        }
    }
    if (settings.reminder) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onChange(settings.copy(reminderHour = (settings.reminderHour + 23) % 24)) }) {
                Text("−")
            }
            Text(stringResource(R.string.reminder_hour, settings.reminderHour), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { onChange(settings.copy(reminderHour = (settings.reminderHour + 1) % 24)) }) {
                Text("+")
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Acciones de la copia en la nube. */
interface CloudActions {
    fun connectGitHub()
    fun cancelConnect()
    fun disconnectGitHub()
    fun syncNow()
}

@Composable
private fun CloudSection(cloud: CloudState, actions: CloudActions) {
    Text(stringResource(R.string.cloud_info), style = MaterialTheme.typography.bodyMedium)
    val uriHandler = LocalUriHandler.current
    when {
        !cloud.configured -> Text(stringResource(R.string.cloud_not_configured), style = MaterialTheme.typography.bodySmall)

        cloud.userCode != null -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.cloud_code))
                Text(cloud.userCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val uri = cloud.verificationUri ?: "https://github.com/login/device"
                Button(onClick = { uriHandler.openUri(uri) }) {
                    Text(stringResource(R.string.cloud_open, uri.removePrefix("https://")))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.cloud_waiting), Modifier.padding(start = 8.dp).weight(1f))
                    TextButton(onClick = actions::cancelConnect) { Text(stringResource(R.string.cancel)) }
                }
            }
        }

        cloud.login == null -> Button(onClick = actions::connectGitHub, enabled = !cloud.busy) {
            Text(stringResource(R.string.cloud_connect))
        }

        else -> {
            Text(stringResource(R.string.cloud_connected, cloud.login.orEmpty()), fontWeight = FontWeight.Bold)
            Text(
                when {
                    cloud.busy -> stringResource(R.string.cloud_syncing)
                    cloud.lastSync != null -> stringResource(
                        R.string.cloud_last_sync,
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(cloud.lastSync)),
                    )
                    else -> stringResource(R.string.cloud_never)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions::syncNow, enabled = !cloud.busy) { Text(stringResource(R.string.cloud_sync)) }
                OutlinedButton(onClick = actions::disconnectGitHub) { Text(stringResource(R.string.cloud_disconnect)) }
            }
        }
    }
    cloud.error?.let {
        Text(stringResource(R.string.cloud_error, it), color = MaterialTheme.colorScheme.error)
    }
}
