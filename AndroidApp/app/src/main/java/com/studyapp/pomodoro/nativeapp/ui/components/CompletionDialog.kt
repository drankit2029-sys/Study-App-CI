package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.studyapp.pomodoro.nativeapp.CompletionInfo
import com.studyapp.pomodoro.nativeapp.Phase
import com.studyapp.pomodoro.nativeapp.SessionSettings
import com.studyapp.pomodoro.nativeapp.ui.theme.Charcoal
import com.studyapp.pomodoro.nativeapp.ui.theme.DeepTeal
import com.studyapp.pomodoro.nativeapp.ui.theme.MutedGrey
import com.studyapp.pomodoro.nativeapp.ui.theme.RaisedCream
import com.studyapp.pomodoro.nativeapp.ui.theme.TealTint

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .softRaised(RaisedCream, 20.dp, 20.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .softRaised(RaisedCream, 999.dp, 6.dp)
                        .background(TealTint, RoundedCornerShape(999.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = DeepTeal,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    color = Charcoal,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    color = MutedGrey,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    ),
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SoftButton(
                        label = "Later",
                        onClick = onDismiss,
                        variant = SoftButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    SoftButton(
                        label = confirmLabel,
                        onClick = onStartNext,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
