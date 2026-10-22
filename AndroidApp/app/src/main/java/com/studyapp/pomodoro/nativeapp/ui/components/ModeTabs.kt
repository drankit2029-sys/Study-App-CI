package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyapp.pomodoro.nativeapp.Phase
import com.studyapp.pomodoro.nativeapp.shortLabel
import com.studyapp.pomodoro.nativeapp.ui.theme.DeepTeal
import com.studyapp.pomodoro.nativeapp.ui.theme.InsetCream
import com.studyapp.pomodoro.nativeapp.ui.theme.MutedGrey
import com.studyapp.pomodoro.nativeapp.ui.theme.RaisedCream

private val modes = listOf(Phase.Focus, Phase.ShortBreak, Phase.LongBreak)

@Composable
fun ModeTabs(
    activePhase: Phase,
    enabled: Boolean,
    onSelect: (Phase) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .softRaised(RaisedCream, 24.dp, 4.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        modes.forEach { phase ->
            val active = phase == activePhase
            val interactionSource = remember { MutableInteractionSource() }
            val haptic = LocalHapticFeedback.current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .alpha(if (enabled) 1f else 0.6f)
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (active) {
                            Modifier.softInset(InsetCream, 20.dp, 7.dp)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Tab,
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(phase)
                    }
                    .semantics { selected = active }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = phase.shortLabel,
                    color = if (active) DeepTeal else MutedGrey,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (active) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .width(24.dp)
                            .height(2.dp)
                            .background(DeepTeal, RoundedCornerShape(1.dp)),
                    )
                }
            }
        }
    }
}
