package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyapp.pomodoro.nativeapp.ui.theme.Ink
import com.studyapp.pomodoro.nativeapp.ui.theme.Muted

@Composable
fun Stepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val canDecrease = enabled && value > min
    val canIncrease = enabled && value < max

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "$min-$max min",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                ),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            SoftIconButton(
                icon = Icons.Outlined.Remove,
                contentDescription = "Decrease $label by $step minutes",
                onClick = { onChange(value - step) },
                enabled = canDecrease,
                size = 34.dp,
                iconSize = 15.dp,
            )
            Row(
                modifier = Modifier
                    .width(54.dp)
                    .clearAndSetSemantics {
                        contentDescription = "$label: $value minutes"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = value.toString(),
                    color = Ink,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "lnum",
                    ),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "min",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 0.4.sp,
                    ),
                )
            }
            SoftIconButton(
                icon = Icons.Outlined.Add,
                contentDescription = "Increase $label by $step minutes",
                onClick = { onChange(value + step) },
                enabled = canIncrease,
                size = 34.dp,
                iconSize = 15.dp,
            )
        }
    }
}
