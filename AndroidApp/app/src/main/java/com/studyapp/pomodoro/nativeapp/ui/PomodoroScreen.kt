package com.studyapp.pomodoro.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyapp.pomodoro.nativeapp.PomodoroUiState
import com.studyapp.pomodoro.nativeapp.Phase
import com.studyapp.pomodoro.nativeapp.SessionsPerCycle
import com.studyapp.pomodoro.nativeapp.SettingBounds
import com.studyapp.pomodoro.nativeapp.formatClock
import com.studyapp.pomodoro.nativeapp.heading
import com.studyapp.pomodoro.nativeapp.ui.components.CompletionDialog
import com.studyapp.pomodoro.nativeapp.ui.components.ModeTabs
import com.studyapp.pomodoro.nativeapp.ui.components.ProgressRing
import com.studyapp.pomodoro.nativeapp.ui.components.SoftButton
import com.studyapp.pomodoro.nativeapp.ui.components.SoftIconButton
import com.studyapp.pomodoro.nativeapp.ui.components.StatCard
import com.studyapp.pomodoro.nativeapp.ui.components.Stepper
import com.studyapp.pomodoro.nativeapp.ui.components.softInset
import com.studyapp.pomodoro.nativeapp.ui.components.softRaised
import com.studyapp.pomodoro.nativeapp.ui.theme.BaseCream
import com.studyapp.pomodoro.nativeapp.ui.theme.BorderCream
import com.studyapp.pomodoro.nativeapp.ui.theme.Charcoal
import com.studyapp.pomodoro.nativeapp.ui.theme.DeepTeal
import com.studyapp.pomodoro.nativeapp.ui.theme.DisabledFill
import com.studyapp.pomodoro.nativeapp.ui.theme.DotIdle
import com.studyapp.pomodoro.nativeapp.ui.theme.FocusGlow
import com.studyapp.pomodoro.nativeapp.ui.theme.InsetCream
import com.studyapp.pomodoro.nativeapp.ui.theme.MutedGrey
import com.studyapp.pomodoro.nativeapp.ui.theme.OnTeal
import com.studyapp.pomodoro.nativeapp.ui.theme.RaisedCream
import com.studyapp.pomodoro.nativeapp.ui.theme.TealTint
import com.studyapp.pomodoro.nativeapp.ui.theme.WarmAmber

@Composable
fun PomodoroScreen(
    state: PomodoroUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (Phase) -> Unit,
    onUpdateSetting: (Phase, Int) -> Unit,
    onDismissCompletion: () -> Unit,
    onStartNext: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    SideEffect { view.keepScreenOn = state.isRunning }
    DisposableEffect(Unit) {
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(state.completion) {
        if (state.completion != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaseCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 40.dp),
        ) {
            Header()
            Spacer(Modifier.height(24.dp))
            TimerCard(
                state = state,
                onToggle = onToggle,
                onReset = onReset,
                onSelectPhase = onSelectPhase,
            )
            Spacer(Modifier.height(16.dp))
            StatsRow(state)
            Spacer(Modifier.height(16.dp))
            SettingsCard(state = state, onUpdateSetting = onUpdateSetting)
        }
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

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .softInset(InsetCream, 14.dp, 6.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = DeepTeal,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "STUDY RHYTHM",
                    color = DeepTeal,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        lineHeight = 12.sp,
                    ),
                )
            }
            Text(
                text = "Pomodoro",
                color = Charcoal,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "A softer way to protect your focus.",
                color = MutedGrey,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                ),
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(56.dp)
                .softRaised(RaisedCream, 999.dp, 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = DeepTeal,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun TimerCard(
    state: PomodoroUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onSelectPhase: (Phase) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softRaised(RaisedCream, 24.dp, 10.dp)
            .padding(16.dp),
    ) {
        ModeTabs(
            activePhase = state.phase,
            enabled = !state.isRunning,
            onSelect = onSelectPhase,
        )
        Spacer(Modifier.height(20.dp))
        TimerRing(state)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SoftButton(
                label = state.actionLabel,
                onClick = onToggle,
                icon = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                modifier = Modifier.weight(1f),
            )
            SoftIconButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Reset timer",
                onClick = onReset,
                enabled = state.canReset,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val statusColor = when {
                state.isRunning -> DeepTeal
                state.remaining < state.totalSeconds -> WarmAmber
                else -> DisabledFill
            }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(statusColor, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = state.statusText,
                color = MutedGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 260.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                ),
            )
        }
    }
}

@Composable
private fun TimerRing(state: PomodoroUiState) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val wellSize = minOf(maxWidth, 280.dp)
        val ringSize = wellSize - 28.dp
        val clockSize = if (ringSize < 230.dp) 44.sp else 52.sp
        val clockLineHeight = if (ringSize < 230.dp) 50.sp else 58.sp
        Box(
            modifier = Modifier
                .size(wellSize)
                .softInset(InsetCream, 999.dp, 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProgressRing(progress = state.progress, size = ringSize) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.phase.heading.uppercase(),
                        color = MutedGrey,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                            lineHeight = 14.sp,
                        ),
                    )
                    Text(
                        text = formatClock(state.remaining),
                        color = Charcoal,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .semantics {
                                contentDescription = "${formatClock(state.remaining)} remaining"
                            },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = clockSize,
                            lineHeight = clockLineHeight,
                            letterSpacing = (-1).sp,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    Text(
                        text = state.sessionLabel,
                        color = MutedGrey,
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
}

@Composable
private fun SessionDots(state: PomodoroUiState) {
    Row(
        modifier = Modifier.semantics {
            contentDescription = "${state.filledDots} of $SessionsPerCycle sessions complete"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(SessionsPerCycle) { index ->
            val filled = index < state.filledDots
            val current = state.phase == Phase.Focus && index == state.filledDots
            val dotModifier = when {
                filled -> Modifier.background(DeepTeal, CircleShape)
                current -> Modifier.border(2.dp, FocusGlow, CircleShape)
                else -> Modifier.background(DotIdle, CircleShape)
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .then(dotModifier),
            )
        }
    }
}

@Composable
private fun StatsRow(state: PomodoroUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(
            icon = Icons.Rounded.CheckCircle,
            label = "SESSIONS",
            value = state.completedFocus.toString(),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Rounded.Schedule,
            label = "MINUTES",
            value = state.focusedMinutes.toString(),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            icon = Icons.Rounded.Flag,
            label = "GOAL",
            value = "${minOf(state.completedFocus, SessionsPerCycle)}/$SessionsPerCycle",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SettingsCard(
    state: PomodoroUiState,
    onUpdateSetting: (Phase, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softRaised(RaisedCream, 24.dp, 8.dp)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(TealTint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "Session lengths",
                    color = Charcoal,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .background(DeepTeal, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "MINUTES",
                    color = OnTeal,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        lineHeight = 10.sp,
                    ),
                )
            }
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
        HorizontalDivider(color = BorderCream, thickness = 1.dp)
        Stepper(
            label = "Short break",
            value = state.settings.short,
            min = SettingBounds.getValue(Phase.ShortBreak).min,
            max = SettingBounds.getValue(Phase.ShortBreak).max,
            step = SettingBounds.getValue(Phase.ShortBreak).step,
            onChange = { onUpdateSetting(Phase.ShortBreak, it) },
            enabled = !state.isRunning,
        )
        HorizontalDivider(color = BorderCream, thickness = 1.dp)
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
                text = "Pause the timer to adjust session lengths.",
                color = MutedGrey,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                ),
            )
        }
    }
}
