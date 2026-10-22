package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyapp.pomodoro.nativeapp.ui.theme.Charcoal
import com.studyapp.pomodoro.nativeapp.ui.theme.DisabledFill
import com.studyapp.pomodoro.nativeapp.ui.theme.InsetCream
import com.studyapp.pomodoro.nativeapp.ui.theme.MutedGrey

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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (enabled) Charcoal else MutedGrey,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                ),
            )
            Text(
                text = "$min-$max min",
                color = MutedGrey,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                ),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SoftIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Decrease $label by $step minutes",
                onClick = { onChange(value - step) },
                enabled = canDecrease,
                size = 36.dp,
                iconSize = 16.dp,
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(38.dp)
                    .softInset(InsetCream, 8.dp, 6.dp)
                    .semantics {
                        contentDescription = "$label: $value minutes"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = value.toString(),
                        color = if (enabled) Charcoal else MutedGrey,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    Text(
                        text = "min",
                        color = MutedGrey,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp,
                        ),
                    )
                }
            }
            SoftIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Increase $label by $step minutes",
                onClick = { onChange(value + step) },
                enabled = canIncrease,
                size = 36.dp,
                iconSize = 16.dp,
            )
        }
    }
}
