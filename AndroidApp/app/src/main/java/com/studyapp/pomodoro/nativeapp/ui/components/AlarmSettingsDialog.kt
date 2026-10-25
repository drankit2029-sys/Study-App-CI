package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.studyapp.pomodoro.nativeapp.AlarmDurationOptions
import com.studyapp.pomodoro.nativeapp.AlarmSettings
import com.studyapp.pomodoro.nativeapp.PomodoroAlarmPlayer
import com.studyapp.pomodoro.nativeapp.ui.theme.Border
import com.studyapp.pomodoro.nativeapp.ui.theme.Forest
import com.studyapp.pomodoro.nativeapp.ui.theme.Ink
import com.studyapp.pomodoro.nativeapp.ui.theme.Ivory
import com.studyapp.pomodoro.nativeapp.ui.theme.Mint
import com.studyapp.pomodoro.nativeapp.ui.theme.Muted
import com.studyapp.pomodoro.nativeapp.ui.theme.Paper

@Composable
fun AlarmSettingsDialog(
    settings: AlarmSettings,
    onDismiss: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSoundClick: () -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onDurationChange: (Int) -> Unit,
    onPreview: () -> Unit,
) {
    val context = LocalContext.current
    val soundTitle = remember(settings.sound) {
        PomodoroAlarmPlayer.alarmSoundTitle(context, settings.sound)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val shortLandscape = maxWidth > maxHeight && maxHeight < 560.dp
            if (shortLandscape) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .heightIn(max = maxHeight)
                        .softRaised(Paper, 26.dp, 18.dp)
                        .padding(14.dp),
                ) {
                    AlarmDialogHeading(onDismiss = onDismiss, dense = true)
                    Spacer(Modifier.height(6.dp))
                    AlarmDialogDescription(dense = true)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Border, thickness = 1.dp)
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AlarmToggleRow(
                                title = "Ring when a session ends",
                                subtitle = "Play an alarm and show the completion card.",
                                checked = settings.enabled,
                                onCheckedChange = onEnabledChange,
                                dense = true,
                            )
                            HorizontalDivider(color = Border, thickness = 1.dp)
                            AlarmSoundRow(
                                soundTitle = soundTitle,
                                onClick = onSoundClick,
                                dense = true,
                            )
                            HorizontalDivider(color = Border, thickness = 1.dp)
                            AlarmToggleRow(
                                title = "Vibrate",
                                subtitle = "Use your phone's alarm vibration pattern.",
                                checked = settings.vibration,
                                onCheckedChange = onVibrationChange,
                                dense = true,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Spacer(Modifier.height(6.dp))
                            AlarmDurationSection(
                                durationSeconds = settings.durationSeconds,
                                onDurationChange = onDurationChange,
                                dense = true,
                            )
                            Spacer(Modifier.height(10.dp))
                            AlarmPreviewButton(onPreview = onPreview)
                            Spacer(Modifier.height(8.dp))
                            AlarmFootnote(dense = true)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .heightIn(max = maxHeight)
                        .softRaised(Paper, 26.dp, 18.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                ) {
                    AlarmDialogHeading(onDismiss = onDismiss)
                    Spacer(Modifier.height(12.dp))
                    AlarmDialogDescription()
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Border, thickness = 1.dp)
                    AlarmToggleRow(
                        title = "Ring when a session ends",
                        subtitle = "Play an alarm and show the completion card.",
                        checked = settings.enabled,
                        onCheckedChange = onEnabledChange,
                    )
                    HorizontalDivider(color = Border, thickness = 1.dp)
                    AlarmSoundRow(
                        soundTitle = soundTitle,
                        onClick = onSoundClick,
                    )
                    HorizontalDivider(color = Border, thickness = 1.dp)
                    AlarmToggleRow(
                        title = "Vibrate",
                        subtitle = "Use your phone's alarm vibration pattern.",
                        checked = settings.vibration,
                        onCheckedChange = onVibrationChange,
                    )
                    HorizontalDivider(color = Border, thickness = 1.dp)
                    Spacer(Modifier.height(14.dp))
                    AlarmDurationSection(
                        durationSeconds = settings.durationSeconds,
                        onDurationChange = onDurationChange,
                    )
                    Spacer(Modifier.height(16.dp))
                    AlarmPreviewButton(onPreview = onPreview)
                    Spacer(Modifier.height(12.dp))
                    AlarmFootnote()
                }
            }
        }
    }
}

@Composable
private fun AlarmDialogHeading(
    onDismiss: () -> Unit,
    dense: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (dense) 36.dp else 46.dp)
                .background(Mint, RoundedCornerShape(if (dense) 13.dp else 16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Alarm,
                contentDescription = null,
                tint = Forest,
                modifier = Modifier.size(if (dense) 18.dp else 22.dp),
            )
        }
        Spacer(Modifier.width(if (dense) 10.dp else 14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RING & RESET",
                color = Forest,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (dense) 9.sp else 10.sp,
                    letterSpacing = 1.6.sp,
                ),
            )
            Text(
                text = "Alarm settings",
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = if (dense) 26.sp else 31.sp,
                    lineHeight = if (dense) 28.sp else 33.sp,
                ),
            )
        }
        Spacer(Modifier.width(8.dp))
        SoftIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = "Close alarm settings",
            onClick = onDismiss,
            size = if (dense) 36.dp else 42.dp,
            iconSize = if (dense) 15.dp else 17.dp,
        )
    }
}

@Composable
private fun AlarmDialogDescription(dense: Boolean = false) {
    Text(
        text = if (dense) {
            "How the phone calls you back when a session ends."
        } else {
            "Choose how the phone calls you back when a session ends."
        },
        color = Muted,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = if (dense) 11.sp else 12.sp,
            lineHeight = if (dense) 15.sp else 18.sp,
        ),
    )
}

@Composable
private fun AlarmSoundRow(
    soundTitle: String,
    onClick: () -> Unit,
    dense: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = if (dense) 6.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Alarm sound",
                color = Ink,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (dense) 13.sp else 14.sp,
                ),
            )
            Spacer(Modifier.height(if (dense) 1.dp else 3.dp))
            Text(
                text = soundTitle,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (dense) 10.sp else 11.sp,
                    lineHeight = if (dense) 13.sp else 15.sp,
                ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "CHOOSE",
            color = Forest,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
            ),
        )
    }
}

@Composable
private fun AlarmDurationSection(
    durationSeconds: Int,
    onDurationChange: (Int) -> Unit,
    dense: Boolean = false,
) {
    Text(
        text = "RING FOR",
        color = Muted,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.5.sp,
        ),
    )
    Spacer(Modifier.height(if (dense) 6.dp else 8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AlarmDurationOptions.forEach { seconds ->
            DurationChip(
                seconds = seconds,
                selected = durationSeconds == seconds,
                onClick = { onDurationChange(seconds) },
                dense = dense,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AlarmPreviewButton(onPreview: () -> Unit) {
    SoftButton(
        label = "Preview alarm",
        onClick = onPreview,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AlarmFootnote(dense: Boolean = false) {
    Text(
        text = if (dense) {
            "Uses alarm volume and respects Do Not Disturb."
        } else {
            "Uses your phone's alarm volume and respects Do Not Disturb. You can silence a ringing alarm from the completion card or notification."
        },
        color = Muted,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 9.sp,
            lineHeight = if (dense) 12.sp else 14.sp,
        ),
    )
}

@Composable
private fun AlarmToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    dense: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (dense) 5.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Ink,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (dense) 13.sp else 14.sp,
                ),
            )
            Spacer(Modifier.height(if (dense) 1.dp else 3.dp))
            Text(
                text = subtitle,
                color = Muted,
                maxLines = if (dense) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = if (dense) 12.sp else 14.sp,
                ),
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ivory,
                checkedTrackColor = Forest,
            ),
        )
    }
}

@Composable
private fun DurationChip(
    seconds: Int,
    selected: Boolean,
    onClick: () -> Unit,
    dense: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) Forest else Paper
    val content = if (selected) Ivory else Muted
    Box(
        modifier = modifier
            .heightIn(min = if (dense) 36.dp else 42.dp)
            .background(background, RoundedCornerShape(15.dp))
            .border(1.dp, if (selected) Forest else Border, RoundedCornerShape(15.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${seconds}s",
            color = content,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
