package com.studyapp.decaycharge.ui.screens

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyapp.decaycharge.ui.SettingsUiState
import com.studyapp.decaycharge.ui.SettingsViewModel
import com.studyapp.decaycharge.ui.components.CalmButton
import com.studyapp.decaycharge.ui.components.CalmButtonVariant
import com.studyapp.decaycharge.ui.components.CalmCard
import com.studyapp.decaycharge.ui.components.CalmIconButton
import com.studyapp.decaycharge.ui.components.Eyebrow
import com.studyapp.decaycharge.ui.components.FilterPill
import com.studyapp.decaycharge.ui.components.Hairline
import com.studyapp.decaycharge.ui.components.SectionLabel
import com.studyapp.decaycharge.ui.theme.Border
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.Ice
import com.studyapp.decaycharge.ui.theme.IceSoft
import com.studyapp.decaycharge.ui.theme.Ink
import com.studyapp.decaycharge.ui.theme.Ivory
import com.studyapp.decaycharge.ui.theme.Muted
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.Parchment
import com.studyapp.decaycharge.ui.theme.ZoneRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::restoreBackup)
    }

    fun requestDndAccess() {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    fun requestFullScreenAccess() {
        if (Build.VERSION.SDK_INT >= 34) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    BackHandler { onBack() }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.preferences.examDateEpochMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(viewModel::setExamDate)
                        showDatePicker = false
                    },
                ) {
                    Text("Set exam", color = Forest)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Muted)
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ivory)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalmIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                filled = false,
                tint = Ink,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Eyebrow("PREFERENCES")
                Text(
                    text = "Settings",
                    color = Ink,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ExamCard(state, onChangeDate = { showDatePicker = true }, onClear = { viewModel.setExamDate(null) })
            FocusCard(
                state = state,
                onDnd = {
                    viewModel.setDndEnabled(it)
                    if (it && !context.getSystemService(NotificationManager::class.java)
                            .isNotificationPolicyAccessGranted) {
                        requestDndAccess()
                    }
                },
                onStrictFocus = viewModel::setStrictFocus,
                onKeepScreen = viewModel::setKeepScreenOn,
                onFullScreen = {
                    viewModel.setFullScreenAlerts(it)
                    val manager = context.getSystemService(NotificationManager::class.java)
                    if (it && Build.VERSION.SDK_INT >= 34 && !manager.canUseFullScreenIntent()) {
                        requestFullScreenAccess()
                    }
                },
                onGrantDnd = ::requestDndAccess,
            )
            DigestCard(
                state = state,
                onEnabled = viewModel::setDigestEnabled,
                onTime = viewModel::setDigestTime,
            )
            FreezeCard(state = state, onEnd = viewModel::endFreeze)
            BackupCard(
                state = state,
                onExport = { createBackup.launch("decaycharge-backup.json") },
                onImport = { openBackup.launch(arrayOf("application/json", "text/plain", "*/*")) },
            )
            state.message?.let { message ->
                Text(
                    text = message,
                    color = if (message.startsWith("Restore failed") || message.startsWith("Backup failed")) {
                        ZoneRed
                    } else {
                        Forest
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Hairline(color = Border)
            Text(
                text = "DECAYCHARGE 0.6.0 · LOCAL-FIRST",
                color = Muted,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    letterSpacing = 1.6.sp,
                ),
            )
        }
    }
}

@Composable
private fun ExamCard(
    state: SettingsUiState,
    onChangeDate: () -> Unit,
    onClear: () -> Unit,
) {
    CalmCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevation = 5.dp) {
        Column(modifier = Modifier.padding(18.dp)) {
            SectionLabel(index = "01", title = "EXAM COUNTDOWN")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(IceSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = Ice,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.daysUntilExam == null) {
                            "No exam date set"
                        } else {
                            "${state.daysUntilExam} days remaining"
                        },
                        color = Ink,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                    )
                    Text(
                        text = if (state.daysUntilExam != null && state.daysUntilExam <= 30) {
                            "Crunch scaling is lowering effective stability for high-yield chapters."
                        } else {
                            "Within 30 days, high-yield topics enter review windows earlier."
                        },
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalmButton(
                    label = if (state.preferences.examDateEpochMs == null) "Set exam date" else "Change date",
                    onClick = onChangeDate,
                    variant = CalmButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                if (state.preferences.examDateEpochMs != null) {
                    CalmButton(
                        label = "Clear",
                        onClick = onClear,
                        variant = CalmButtonVariant.Quiet,
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusCard(
    state: SettingsUiState,
    onDnd: (Boolean) -> Unit,
    onStrictFocus: (Boolean) -> Unit,
    onKeepScreen: (Boolean) -> Unit,
    onFullScreen: (Boolean) -> Unit,
    onGrantDnd: () -> Unit,
) {
    val prefs = state.preferences
    CalmCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevation = 5.dp) {
        Column(modifier = Modifier.padding(18.dp)) {
            SectionLabel(index = "02", title = "FOCUS & TIMER")
            Spacer(Modifier.height(8.dp))
            SettingToggle(
                title = "Do Not Disturb while charging",
                subtitle = "Switch to priority interruptions and restore afterwards.",
                checked = prefs.dndEnabled,
                onCheckedChange = onDnd,
            )
            if (prefs.dndEnabled) {
                Text(
                    text = "If DND access is missing, Android asks you to grant it.",
                    color = Muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onGrantDnd)
                        .padding(start = 2.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                )
            }
            SettingDivider()
            SettingToggle(
                title = "Strict focus guard",
                subtitle = "Warn after 15s away, then pause after 30s.",
                checked = prefs.strictFocusEnabled,
                onCheckedChange = onStrictFocus,
            )
            SettingDivider()
            SettingToggle(
                title = "Keep charger screen awake",
                subtitle = "Only while the charger is visible.",
                checked = prefs.keepScreenOn,
                onCheckedChange = onKeepScreen,
            )
            SettingDivider()
            SettingToggle(
                title = "Full-screen completion alert",
                subtitle = "Wake the screen when a charge completes, if allowed.",
                checked = prefs.fullScreenAlertsEnabled,
                onCheckedChange = onFullScreen,
            )
        }
    }
}

@Composable
private fun DigestCard(
    state: SettingsUiState,
    onEnabled: (Boolean) -> Unit,
    onTime: (Int, Int) -> Unit,
) {
    val prefs = state.preferences
    CalmCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevation = 4.dp) {
        Column(modifier = Modifier.padding(18.dp)) {
            SectionLabel(index = "03", title = "MORNING DIGEST")
            SettingToggle(
                title = "One daily triage reminder",
                subtitle = "Never one notification per fading chapter.",
                checked = prefs.digestEnabled,
                onCheckedChange = onEnabled,
            )
            if (prefs.digestEnabled) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "DELIVERY TIME",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, letterSpacing = 1.3.sp),
                )
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(7, 8, 9, 18, 20).forEach { hour ->
                        FilterPill(
                            label = "%02d:00".format(hour),
                            selected = prefs.digestHour == hour && prefs.digestMinute == 0,
                            onClick = { onTime(hour, 0) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FreezeCard(state: SettingsUiState, onEnd: () -> Unit) {
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        color = if (state.activeFreeze != null) IceSoft else Paper,
        cornerRadius = 24.dp,
        elevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            SectionLabel(index = "04", title = "CRYO-FREEZE")
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.AcUnit,
                    contentDescription = null,
                    tint = Ice,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (state.activeFreeze == null) {
                        "Batteries are decaying normally."
                    } else {
                        "All battery decay is paused."
                    },
                    color = Ink,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                )
            }
            if (state.activeFreeze != null) {
                Spacer(Modifier.height(12.dp))
                CalmButton(
                    label = "End Cryo-Freeze",
                    onClick = onEnd,
                    variant = CalmButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = "Start or extend a freeze from the snowflake on Triage.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                )
            }
        }
    }
}

@Composable
private fun BackupCard(
    state: SettingsUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    CalmCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, elevation = 4.dp) {
        Column(modifier = Modifier.padding(18.dp)) {
            SectionLabel(index = "05", title = "BACKUP & RESTORE")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Full JSON backup of subjects, chapters, sessions, freezes, and preferences. Android Auto Backup is also enabled.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
            )
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalmButton(
                    label = "Export",
                    onClick = onExport,
                    leadingIcon = Icons.Rounded.Download,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                )
                CalmButton(
                    label = "Restore",
                    onClick = onImport,
                    leadingIcon = Icons.Rounded.Upload,
                    enabled = !state.busy,
                    variant = CalmButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Ink,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
            )
            Text(
                text = subtitle,
                color = Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 14.sp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingDivider() {
    Spacer(Modifier.height(2.dp))
    Hairline(color = Border)
    Spacer(Modifier.height(2.dp))
}
