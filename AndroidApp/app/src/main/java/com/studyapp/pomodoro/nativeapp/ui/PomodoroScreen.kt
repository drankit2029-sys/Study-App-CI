package com.studyapp.pomodoro.nativeapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyapp.pomodoro.nativeapp.Phase
import com.studyapp.pomodoro.nativeapp.PomodoroUiState
import com.studyapp.pomodoro.nativeapp.SessionsPerCycle
import com.studyapp.pomodoro.nativeapp.SettingBounds
import com.studyapp.pomodoro.nativeapp.formatClock
import com.studyapp.pomodoro.nativeapp.heading
import com.studyapp.pomodoro.nativeapp.ui.components.BotanicalBranch
import com.studyapp.pomodoro.nativeapp.ui.components.CompletionDialog
import com.studyapp.pomodoro.nativeapp.ui.components.ModeTabs
import com.studyapp.pomodoro.nativeapp.ui.components.ProgressRing
import com.studyapp.pomodoro.nativeapp.ui.components.SoftButton
import com.studyapp.pomodoro.nativeapp.ui.components.SoftButtonVariant
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
            .clipToBounds()
            .background(Ivory),
        contentAlignment = Alignment.TopCenter,
    ) {
        BotanicalBranch(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 52.dp, y = (-18).dp)
                .size(132.dp),
        )
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 18.dp, bottom = 36.dp),
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
            RhythmCard(state)
            Spacer(Modifier.height(16.dp))
            SettingsCard(state = state, onUpdateSetting = onUpdateSetting)
            Spacer(Modifier.height(26.dp))
            Footer()
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
    Column(modifier = Modifier.fillMaxWidth()) {
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
) {
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
            enabled = !state.isRunning,
            onSelect = onSelectPhase,
        )
        Spacer(Modifier.height(18.dp))
        TimerFace(state)
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

@Composable
private fun TimerFace(state: PomodoroUiState) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val faceSize = minOf(maxWidth, 258.dp)
        val clockSize = if (faceSize < 224.dp) 42.sp else 54.sp
        val clockLineHeight = if (faceSize < 224.dp) 44.sp else 56.sp
        ProgressRing(
            progress = state.progress,
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
private fun RhythmCard(state: PomodoroUiState) {
    val goalCount = minOf(state.completedFocus, SessionsPerCycle)
    val goalProgress = goalCount.toFloat() / SessionsPerCycle
    val animatedProgress by animateFloatAsState(
        targetValue = goalProgress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "goalProgress",
    )
    val caption = when {
        state.completedFocus == 0 -> "A GENTLE START"
        state.completedFocus < SessionsPerCycle -> "ALMOST THERE"
        else -> "CYCLE COMPLETE"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .softRaised(Paper, 24.dp, 7.dp)
            .padding(20.dp),
    ) {
        SectionLabel(index = "02", title = "TODAY'S RHYTHM")
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatBlock(
                value = state.completedFocus.toString(),
                label = "SESSIONS",
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            StatBlock(
                value = state.focusedMinutes.toString(),
                label = "MINUTES",
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            StatBlock(
                value = "$goalCount/$SessionsPerCycle",
                label = "GOAL",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(18.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
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
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = caption,
                color = Muted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                ),
            )
            Text(
                text = "$goalCount OF $SessionsPerCycle",
                color = Forest,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                ),
            )
        }
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String,
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
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "lnum",
            ),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
            ),
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(42.dp)
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
