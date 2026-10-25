package com.studyapp.pomodoro.nativeapp.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import com.studyapp.pomodoro.nativeapp.DEFAULT_ALARM_SOUND
import com.studyapp.pomodoro.nativeapp.Phase
import com.studyapp.pomodoro.nativeapp.PomodoroUiState
import com.studyapp.pomodoro.nativeapp.SILENT_ALARM_SOUND
import com.studyapp.pomodoro.nativeapp.SessionsPerCycle
import com.studyapp.pomodoro.nativeapp.SettingBounds
import com.studyapp.pomodoro.nativeapp.formatClock
import com.studyapp.pomodoro.nativeapp.heading
import com.studyapp.pomodoro.nativeapp.ui.components.AlarmSettingsDialog
import com.studyapp.pomodoro.nativeapp.ui.components.BotanicalBranch
import com.studyapp.pomodoro.nativeapp.ui.components.CompletionDialog
import com.studyapp.pomodoro.nativeapp.ui.components.ModeTabs
import com.studyapp.pomodoro.nativeapp.ui.components.NativeAdCard
import com.studyapp.pomodoro.nativeapp.ui.components.ProgressRing
import com.studyapp.pomodoro.nativeapp.ui.components.SoftButton
import com.studyapp.pomodoro.nativeapp.ui.components.SoftButtonVariant
import com.studyapp.pomodoro.nativeapp.ui.components.SoftIconButton
import com.studyapp.pomodoro.nativeapp.ui.components.Stepper
import com.studyapp.pomodoro.nativeapp.ui.components.softRaised
import com.studyapp.pomodoro.nativeapp.ui.theme.Border
import com.studyapp.pomodoro.nativeapp.ui.theme.Forest
import com.studyapp.pomodoro.nativeapp.ui.theme.Ink
import com.studyapp.pomodoro.nativeapp.ui.theme.Ivory
import com.studyapp.pomodoro.nativeapp.ui.theme.Mint
import com.studyapp.pomodoro.nativeapp.ui.theme.Muted
import com.studyapp.pomodoro.nativeapp.ui.theme.Paper
import com.studyapp.pomodoro.nativeapp.ui.theme.Stone
import com.studyapp.pomodoro.nativeapp.ui.theme.StoneLight

@Composable
fun PomodoroScreen(
    state: PomodoroUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (Phase) -> Unit,
    onUpdateSetting: (Phase, Int) -> Unit,
    onResetDaily: () -> Unit,
    onUpdateAlarmEnabled: (Boolean) -> Unit,
    onUpdateAlarmSound: (String) -> Unit,
    onUpdateAlarmVibration: (Boolean) -> Unit,
    onUpdateAlarmDuration: (Int) -> Unit,
    onPreviewAlarm: () -> Unit,
    onStopAlarm: () -> Unit,
    onDismissCompletion: () -> Unit,
    onStartNext: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var showAlarmSettings by rememberSaveable { mutableStateOf(false) }
    val ringtonePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                val pickedUri = IntentCompat.getParcelableExtra(
                    data,
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
                onUpdateAlarmSound(pickedUri?.toString() ?: SILENT_ALARM_SOUND)
            }
        }
    }
    SideEffect { view.keepScreenOn = state.isRunning }
    DisposableEffect(Unit) {
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(state.completion) {
        if (state.completion != null) {
            showAlarmSettings = false
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val openAlarmSettings = { showAlarmSettings = true }
    val openRingtonePicker = {
        val existingUri = when (state.alarm.sound) {
            DEFAULT_ALARM_SOUND -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            SILENT_ALARM_SOUND -> null
            else -> runCatching { Uri.parse(state.alarm.sound) }.getOrNull()
        }
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_ALARM,
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pomodoro alarm")
        }
        ringtonePicker.launch(intent)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Ivory),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isLandscape = maxWidth > maxHeight
        val isTablet = minOf(maxWidth, maxHeight) >= 600.dp
        val isCompactPortrait = !isLandscape && maxWidth < 390.dp
        val isCompactLandscape = isLandscape && maxWidth < 760.dp && maxHeight < 440.dp
        val useTabletLandscape = isTablet && isLandscape && maxHeight >= 680.dp
        val useTwoPane = isLandscape && !useTabletLandscape &&
            maxWidth >= 600.dp && maxHeight >= 320.dp
        val uiScale = when {
            isTablet -> 1.15f
            isCompactLandscape -> 0.84f
            isCompactPortrait -> 0.86f
            else -> 1f
        }
        val screenDensity = LocalDensity.current

        CompositionLocalProvider(
            LocalDensity provides Density(
                density = screenDensity.density * uiScale,
                fontScale = screenDensity.fontScale,
            ),
        ) {
            BotanicalBranch(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = if (isLandscape) 44.dp else 52.dp, y = (-18).dp)
                    .size(if (isLandscape) 112.dp else 132.dp),
            )
            when {
                useTabletLandscape -> TabletLandscapeContent(
                    state = state,
                    onToggle = onToggle,
                    onReset = onReset,
                    onSelectPhase = onSelectPhase,
                    onUpdateSetting = onUpdateSetting,
                    onResetDaily = onResetDaily,
                    onOpenAlarmSettings = openAlarmSettings,
                )
                useTwoPane -> LandscapeContent(
                    state = state,
                    onToggle = onToggle,
                    onReset = onReset,
                    onSelectPhase = onSelectPhase,
                    onUpdateSetting = onUpdateSetting,
                    onResetDaily = onResetDaily,
                    onOpenAlarmSettings = openAlarmSettings,
                )
                else -> PortraitContent(
                    state = state,
                    onToggle = onToggle,
                    onReset = onReset,
                    onSelectPhase = onSelectPhase,
                    onUpdateSetting = onUpdateSetting,
                    onResetDaily = onResetDaily,
                    onOpenAlarmSettings = openAlarmSettings,
                    maxContentWidth = if (isTablet) 720.dp else 560.dp,
                    timerFaceSize = if (isTablet) 360.dp else 258.dp,
                )
            }

            if (showAlarmSettings && state.completion == null) {
                AlarmSettingsDialog(
                    settings = state.alarm,
                    onDismiss = {
                        onStopAlarm()
                        showAlarmSettings = false
                    },
                    onEnabledChange = onUpdateAlarmEnabled,
                    onSoundClick = openRingtonePicker,
                    onVibrationChange = onUpdateAlarmVibration,
                    onDurationChange = onUpdateAlarmDuration,
                    onPreview = onPreviewAlarm,
                )
            }

            state.completion?.let { completion ->
                CompletionDialog(
                    completion = completion,
                    settings = state.settings,
                    onDismiss = onDismissCompletion,
                    onStartNext = onStartNext,
                )
            }
        }
    }
}

@Composable
private fun PortraitContent(
    state: PomodoroUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (Phase) -> Unit,
    onUpdateSetting: (Phase, Int) -> Unit,
    onResetDaily: () -> Unit,
    onOpenAlarmSettings: () -> Unit,
    maxContentWidth: Dp,
    timerFaceSize: Dp,
) {
    Column(
        modifier = Modifier
            .widthIn(max = maxContentWidth)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 18.dp, bottom = 36.dp),
    ) {
        Header(onOpenAlarmSettings = onOpenAlarmSettings)
        Spacer(Modifier.height(24.dp))
        TimerCard(
            state = state,
            onToggle = onToggle,
            onReset = onReset,
            onSelectPhase = onSelectPhase,
            faceSize = timerFaceSize,
        )
        Spacer(Modifier.height(16.dp))
        NativeAdCard()
        Spacer(Modifier.height(16.dp))
        RhythmCard(state = state, onResetDaily = onResetDaily)
        Spacer(Modifier.height(16.dp))
        SettingsCard(state = state, onUpdateSetting = onUpdateSetting)
        Spacer(Modifier.height(26.dp))
        Footer()
    }
}

@Composable
private fun LandscapeContent(
    state: PomodoroUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (Phase) -> Unit,
    onUpdateSetting: (Phase, Int) -> Unit,
    onResetDaily: () -> Unit,
    onOpenAlarmSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 1280.dp)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        LandscapeHeader(onOpenAlarmSettings = onOpenAlarmSettings)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                TimerCard(
                    state = state,
                    onToggle = onToggle,
                    onReset = onReset,
                    onSelectPhase = onSelectPhase,
                    landscape = true,
                    faceSize = 280.dp,
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                RhythmCard(
                    state = state,
                    compact = true,
                    onResetDaily = onResetDaily,
                )
                Spacer(Modifier.height(10.dp))
                LandscapeSettingsCard(state = state, onUpdateSetting = onUpdateSetting)
                Spacer(Modifier.height(10.dp))
                NativeAdCard()
            }
        }
    }
}

@Composable
private fun TabletLandscapeContent(
    state: PomodoroUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (Phase) -> Unit,
    onUpdateSetting: (Phase, Int) -> Unit,
    onResetDaily: () -> Unit,
    onOpenAlarmSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 1480.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        LandscapeHeader(onOpenAlarmSettings = onOpenAlarmSettings)
        Spacer(Modifier.height(12.dp))
        TimerCard(
            state = state,
            onToggle = onToggle,
            onReset = onReset,
            onSelectPhase = onSelectPhase,
            landscape = true,
            faceSize = 320.dp,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RhythmCard(
                state = state,
                onResetDaily = onResetDaily,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            LandscapeSettingsCard(
                state = state,
                onUpdateSetting = onUpdateSetting,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            NativeAdCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun LandscapeHeader(onOpenAlarmSettings: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val showTagline = maxWidth >= 800.dp
        val titleSize = if (maxWidth < 780.dp) 25.sp else 30.sp
        val titleLineHeight = if (maxWidth < 780.dp) 27.sp else 32.sp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "STUDY / RHYTHM",
                color = Forest,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 3.2.sp,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Pomodoro",
                color = Ink,
                maxLines = 1,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = titleSize,
                    lineHeight = titleLineHeight,
                ),
            )
            Spacer(Modifier.weight(1f))
            if (showTagline) {
                Text(
                    text = "A softer way to protect your focus.",
                    color = Muted,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 240.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    ),
                )
                Spacer(Modifier.width(12.dp))
            }
            SoftIconButton(
                icon = Icons.Outlined.Alarm,
                contentDescription = "Alarm settings",
                onClick = onOpenAlarmSettings,
                size = 42.dp,
                iconSize = 18.dp,
            )
        }
    }
}

@Composable
private fun Header(onOpenAlarmSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STUDY / RHYTHM",
                    color = Forest,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.6.sp,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Pomodoro",
                    color = Ink,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 46.sp,
                        lineHeight = 48.sp,
                    ),
                )
                Text(
                    text = "A softer way to protect your focus.",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.width(12.dp))
            SoftIconButton(
                icon = Icons.Outlined.Alarm,
                contentDescription = "Alarm settings",
                onClick = onOpenAlarmSettings,
                size = 44.dp,
                iconSize = 19.dp,
            )
        }
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(Stone),
        )
    }
}

@Composable
private fun TimerCard(
    state: PomodoroUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (Phase) -> Unit,
    landscape: Boolean = false,
    faceSize: Dp = 258.dp,
) {
    if (landscape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .softRaised(Paper, 24.dp, 8.dp)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerFace(
                state = state,
                modifier = Modifier.weight(1.08f),
                maxFaceSize = faceSize,
            )
            Column(modifier = Modifier.weight(0.92f)) {
                SectionLabel(index = "01", title = "FOCUS TIMER")
                Spacer(Modifier.height(6.dp))
                ModeTabs(
                    activePhase = state.phase,
                    enabled = !state.isRunning && !state.isMidSession,
                    onSelect = onSelectPhase,
                )
                Spacer(Modifier.height(8.dp))
                SoftButton(
                    label = state.actionLabel,
                    onClick = onToggle,
                    leadingIcon = if (state.isRunning) Icons.Rounded.Pause else null,
                    trailingIcon = if (state.isRunning) null else Icons.AutoMirrored.Rounded.ArrowForward,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                SoftButton(
                    label = "Reset timer",
                    onClick = onReset,
                    leadingIcon = Icons.Rounded.Refresh,
                    variant = SoftButtonVariant.Secondary,
                    enabled = state.canReset,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                StatusRow(state)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .softRaised(Paper, 26.dp, 9.dp)
                .padding(20.dp),
        ) {
            SectionLabel(index = "01", title = "FOCUS TIMER")
            Spacer(Modifier.height(16.dp))
            ModeTabs(
                activePhase = state.phase,
                enabled = !state.isRunning && !state.isMidSession,
                onSelect = onSelectPhase,
            )
            Spacer(Modifier.height(18.dp))
            TimerFace(state, maxFaceSize = faceSize)
            Spacer(Modifier.height(18.dp))
            SoftButton(
                label = state.actionLabel,
                onClick = onToggle,
                leadingIcon = if (state.isRunning) Icons.Rounded.Pause else null,
                trailingIcon = if (state.isRunning) null else Icons.AutoMirrored.Rounded.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            SoftButton(
                label = "Reset timer",
                onClick = onReset,
                leadingIcon = Icons.Rounded.Refresh,
                variant = SoftButtonVariant.Secondary,
                enabled = state.canReset,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            StatusRow(state)
        }
    }
}

@Composable
private fun TimerFace(
    state: PomodoroUiState,
    modifier: Modifier = Modifier,
    maxFaceSize: Dp = 258.dp,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val faceSize = minOf(maxWidth, maxFaceSize)
        val clockSize = when {
            faceSize < 200.dp -> 34.sp
            faceSize < 224.dp -> 42.sp
            else -> 54.sp
        }
        val clockLineHeight = when {
            faceSize < 200.dp -> 36.sp
            faceSize < 224.dp -> 44.sp
            else -> 56.sp
        }
        val ringProgress by animateFloatAsState(
            targetValue = state.progress,
            animationSpec = tween(durationMillis = 250, easing = LinearEasing),
            label = "timerRingProgress",
        )
        ProgressRing(
            progress = ringProgress,
            size = faceSize,
            strokeWidth = 8.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.phase.heading.uppercase(),
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 2.0.sp,
                        lineHeight = 14.sp,
                    ),
                )
                Text(
                    text = formatClock(state.remaining),
                    color = Ink,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clearAndSetSemantics {
                            contentDescription = "${formatClock(state.remaining)} remaining"
                        },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = clockSize,
                        lineHeight = clockLineHeight,
                        fontWeight = FontWeight.Light,
                        fontFeatureSettings = "lnum, tnum",
                    ),
                )
                Text(
                    text = state.sessionLabel,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                SessionDots(state)
            }
        }
    }
}

@Composable
private fun SessionDots(state: PomodoroUiState) {
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = "${state.filledDots} of $SessionsPerCycle sessions complete"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(SessionsPerCycle) { index ->
            val filled = index < state.filledDots
            val current = state.phase == Phase.Focus && index == state.filledDots
            val dotModifier = when {
                filled -> Modifier.background(Forest, CircleShape)
                current -> Modifier
                    .background(Mint, CircleShape)
                    .border(1.dp, Forest, CircleShape)
                else -> Modifier.background(StoneLight, CircleShape)
            }
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .then(dotModifier),
            )
        }
    }
}

@Composable
private fun StatusRow(state: PomodoroUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val statusColor: Color = when {
            state.isRunning -> Forest
            state.isMidSession -> Mint
            else -> Stone
        }
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(statusColor, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = state.statusText,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
        )
    }
}

@Composable
private fun RhythmCard(
    state: PomodoroUiState,
    onResetDaily: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val goalCount = minOf(state.completedFocus, SessionsPerCycle)
    val goalProgress = goalCount.toFloat() / SessionsPerCycle
    val animatedProgress by animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "goalProgress",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .softRaised(Paper, if (compact) 22.dp else 24.dp, if (compact) 6.dp else 7.dp)
            .padding(if (compact) 14.dp else 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(index = "02", title = "TODAY'S RHYTHM")
            ResetTodayButton(
                enabled = state.canResetDaily,
                compact = compact,
                onClick = onResetDaily,
            )
        }
        Spacer(Modifier.height(if (compact) 10.dp else 18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatBlock(
                value = state.completedFocus.toString(),
                label = "SESSIONS",
                compact = compact,
                modifier = Modifier.weight(1f),
            )
            StatDivider(compact = compact)
            StatBlock(
                value = state.focusedMinutes.toString(),
                label = "MINUTES",
                compact = compact,
                modifier = Modifier.weight(1f),
            )
            StatDivider(compact = compact)
            StatBlock(
                value = "$goalCount/$SessionsPerCycle",
                label = "GOAL",
                compact = compact,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(if (compact) 10.dp else 18.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 6.dp else 7.dp),
        ) {
            val radius = size.height / 2f
            drawRoundRect(
                color = StoneLight,
                cornerRadius = CornerRadius(radius, radius),
            )
            if (animatedProgress > 0f) {
                drawRoundRect(
                    color = Forest,
                    size = Size(size.width * animatedProgress, size.height),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
        }
        Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "AUTO RESET · 12:00 AM",
                color = Muted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 9.sp else 10.sp,
                    letterSpacing = 1.2.sp,
                ),
            )
            Text(
                text = "$goalCount OF $SessionsPerCycle",
                color = Forest,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 10.sp else 11.sp,
                    letterSpacing = 1.4.sp,
                ),
            )
        }
    }
}

@Composable
private fun ResetTodayButton(
    enabled: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (enabled) Forest else Muted.copy(alpha = 0.45f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = "Reset today's sessions" }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(if (compact) 12.dp else 13.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "RESET TODAY",
            color = contentColor,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (compact) 8.sp else 9.sp,
                letterSpacing = 1.0.sp,
            ),
        )
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = Ink,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = if (compact) 28.sp else 34.sp,
                lineHeight = if (compact) 30.sp else 36.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "lnum",
            ),
        )
        Spacer(Modifier.height(if (compact) 2.dp else 3.dp))
        Text(
            text = label,
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (compact) 10.sp else 11.sp,
                letterSpacing = 1.5.sp,
            ),
        )
    }
}

@Composable
private fun StatDivider(compact: Boolean = false) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(if (compact) 34.dp else 42.dp)
            .background(Border),
    )
}

@Composable
private fun SettingsCard(
    state: PomodoroUiState,
    onUpdateSetting: (Phase, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softRaised(Paper, 24.dp, 7.dp)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(index = "03", title = "SESSION LENGTHS")
            Text(
                text = "MINUTES",
                color = Muted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Stepper(
            label = "Focus",
            value = state.settings.focus,
            min = SettingBounds.getValue(Phase.Focus).min,
            max = SettingBounds.getValue(Phase.Focus).max,
            step = SettingBounds.getValue(Phase.Focus).step,
            onChange = { onUpdateSetting(Phase.Focus, it) },
            enabled = !state.isRunning,
        )
        HorizontalDivider(color = Border, thickness = 1.dp)
        Stepper(
            label = "Short break",
            value = state.settings.short,
            min = SettingBounds.getValue(Phase.ShortBreak).min,
            max = SettingBounds.getValue(Phase.ShortBreak).max,
            step = SettingBounds.getValue(Phase.ShortBreak).step,
            onChange = { onUpdateSetting(Phase.ShortBreak, it) },
            enabled = !state.isRunning,
        )
        HorizontalDivider(color = Border, thickness = 1.dp)
        Stepper(
            label = "Long break",
            value = state.settings.long,
            min = SettingBounds.getValue(Phase.LongBreak).min,
            max = SettingBounds.getValue(Phase.LongBreak).max,
            step = SettingBounds.getValue(Phase.LongBreak).step,
            onChange = { onUpdateSetting(Phase.LongBreak, it) },
            enabled = !state.isRunning,
        )
        if (state.isRunning) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pause the timer to adjust your rhythm.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                ),
            )
        }
    }
}

@Composable
private fun LandscapeSettingsCard(
    state: PomodoroUiState,
    onUpdateSetting: (Phase, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !state.isRunning
    Column(
        modifier = modifier
            .fillMaxWidth()
            .softRaised(Paper, 22.dp, 6.dp)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(index = "03", title = "SESSION LENGTHS")
            Text(
                text = "MINUTES",
                color = Muted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 280.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CompactStepper(
                            label = "Focus",
                            value = state.settings.focus,
                            min = SettingBounds.getValue(Phase.Focus).min,
                            max = SettingBounds.getValue(Phase.Focus).max,
                            step = SettingBounds.getValue(Phase.Focus).step,
                            enabled = enabled,
                            onChange = { onUpdateSetting(Phase.Focus, it) },
                            modifier = Modifier.weight(1f),
                        )
                        CompactStepper(
                            label = "Short",
                            value = state.settings.short,
                            min = SettingBounds.getValue(Phase.ShortBreak).min,
                            max = SettingBounds.getValue(Phase.ShortBreak).max,
                            step = SettingBounds.getValue(Phase.ShortBreak).step,
                            enabled = enabled,
                            onChange = { onUpdateSetting(Phase.ShortBreak, it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CompactStepper(
                            label = "Long",
                            value = state.settings.long,
                            min = SettingBounds.getValue(Phase.LongBreak).min,
                            max = SettingBounds.getValue(Phase.LongBreak).max,
                            step = SettingBounds.getValue(Phase.LongBreak).step,
                            enabled = enabled,
                            onChange = { onUpdateSetting(Phase.LongBreak, it) },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CompactStepper(
                        label = "Focus",
                        value = state.settings.focus,
                        min = SettingBounds.getValue(Phase.Focus).min,
                        max = SettingBounds.getValue(Phase.Focus).max,
                        step = SettingBounds.getValue(Phase.Focus).step,
                        enabled = enabled,
                        onChange = { onUpdateSetting(Phase.Focus, it) },
                        modifier = Modifier.weight(1f),
                    )
                    CompactStepper(
                        label = "Short",
                        value = state.settings.short,
                        min = SettingBounds.getValue(Phase.ShortBreak).min,
                        max = SettingBounds.getValue(Phase.ShortBreak).max,
                        step = SettingBounds.getValue(Phase.ShortBreak).step,
                        enabled = enabled,
                        onChange = { onUpdateSetting(Phase.ShortBreak, it) },
                        modifier = Modifier.weight(1f),
                    )
                    CompactStepper(
                        label = "Long",
                        value = state.settings.long,
                        min = SettingBounds.getValue(Phase.LongBreak).min,
                        max = SettingBounds.getValue(Phase.LongBreak).max,
                        step = SettingBounds.getValue(Phase.LongBreak).step,
                        enabled = enabled,
                        onChange = { onUpdateSetting(Phase.LongBreak, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (state.isRunning) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Pause the timer to adjust your rhythm.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                ),
            )
        }
    }
}

@Composable
private fun CompactStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.alpha(if (enabled) 1f else 0.5f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = "$min-$max min",
            color = Muted,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.2.sp,
            ),
        )
        Spacer(Modifier.height(5.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            SoftIconButton(
                icon = Icons.Outlined.Remove,
                contentDescription = "Decrease $label by $step minutes",
                onClick = { onChange(value - step) },
                enabled = enabled && value > min,
                size = 32.dp,
                iconSize = 13.dp,
            )
            Text(
                text = value.toString(),
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 22.dp),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "lnum",
                ),
            )
            SoftIconButton(
                icon = Icons.Outlined.Add,
                contentDescription = "Increase $label by $step minutes",
                onClick = { onChange(value + step) },
                enabled = enabled && value < max,
                size = 32.dp,
                iconSize = 13.dp,
            )
        }
    }
}

@Composable
private fun SectionLabel(index: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = index,
            color = Forest,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
            ),
        )
        Text(
            text = title,
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
            ),
        )
    }
}

@Composable
private fun Footer() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(1.dp)
                .background(Stone),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "A SOFTER STUDY RHYTHM",
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 2.0.sp,
            ),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(1.dp)
                .background(Stone),
        )
    }
}
