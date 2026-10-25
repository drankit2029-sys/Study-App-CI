package com.studyapp.pomodoro.nativeapp.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.studyapp.pomodoro.nativeapp.CompletionInfo
import com.studyapp.pomodoro.nativeapp.Phase
import com.studyapp.pomodoro.nativeapp.SessionSettings
import com.studyapp.pomodoro.nativeapp.ui.theme.Forest
import com.studyapp.pomodoro.nativeapp.ui.theme.Ink
import com.studyapp.pomodoro.nativeapp.ui.theme.MintLight
import com.studyapp.pomodoro.nativeapp.ui.theme.Muted
import com.studyapp.pomodoro.nativeapp.ui.theme.Paper
import com.studyapp.pomodoro.nativeapp.ui.theme.Stone

@Composable
fun CompletionDialog(
    completion: CompletionInfo,
    settings: SessionSettings,
    onDismiss: () -> Unit,
    onStartNext: () -> Unit,
) {
    val title = when {
        completion.finished == Phase.Focus && completion.next == Phase.LongBreak -> "Cycle complete"
        completion.finished == Phase.Focus -> "Focus complete"
        else -> "Break complete"
    }
    val message = when {
        completion.finished == Phase.Focus && completion.next == Phase.LongBreak ->
            "Four focus sessions complete. A ${settings.long}-minute long break is ready - step away and recharge."
        completion.finished == Phase.Focus ->
            "Session ${completion.completedFocus} done. A ${settings.short}-minute short break is ready."
        else ->
            "Break finished. Your next ${settings.focus}-minute focus session is ready when you are."
    }
    val confirmLabel = if (completion.next == Phase.Focus) "Start focus" else "Start break"

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
                        .widthIn(max = 560.dp)
                        .heightIn(max = maxHeight)
                        .softRaised(Paper, 26.dp, 20.dp)
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.75f),
                            RoundedCornerShape(26.dp),
                        )
                        .padding(18.dp),
                ) {
                    CompletionHeading(title = title, onDismiss = onDismiss)
                    Spacer(Modifier.height(10.dp))
                    CompletionMessage(message = message)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SoftButton(
                            label = confirmLabel,
                            onClick = onStartNext,
                            trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                            modifier = Modifier.weight(1f),
                        )
                        SoftButton(
                            label = "Later",
                            onClick = onDismiss,
                            variant = SoftButtonVariant.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    CompletionFooter()
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 370.dp)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .softRaised(Paper, 26.dp, 20.dp)
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.75f),
                            RoundedCornerShape(26.dp),
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                ) {
                    CompletionHeading(title = title, onDismiss = onDismiss)
                    Spacer(Modifier.height(18.dp))
                    CompletionMessage(message = message)
                    Spacer(Modifier.height(24.dp))
                    SoftButton(
                        label = confirmLabel,
                        onClick = onStartNext,
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    SoftButton(
                        label = "Later",
                        onClick = onDismiss,
                        variant = SoftButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    CompletionFooter()
                }
            }
        }
    }
}

@Composable
private fun CompletionHeading(
    title: String,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val closeInteraction = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(MintLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Forest,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Ink,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                )
                Text(
                    text = "You're ready to go.",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = closeInteraction,
                    indication = LocalIndication.current,
                    role = Role.Button,
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Close dialog",
                tint = Muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CompletionMessage(message: String) {
    Text(
        text = message,
        color = Muted,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.sp,
            lineHeight = 20.sp,
        ),
    )
}

@Composable
private fun CompletionFooter() {
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
        Spacer(Modifier.width(8.dp))
        Text(
            text = "A GENTLE PAUSE",
            color = Muted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(1.dp)
                .background(Stone),
        )
    }
}
