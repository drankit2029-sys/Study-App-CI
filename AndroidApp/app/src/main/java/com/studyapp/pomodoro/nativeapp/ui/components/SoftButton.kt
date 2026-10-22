package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studyapp.pomodoro.nativeapp.ui.theme.DeepTeal
import com.studyapp.pomodoro.nativeapp.ui.theme.DeepTealPressed
import com.studyapp.pomodoro.nativeapp.ui.theme.DisabledFill
import com.studyapp.pomodoro.nativeapp.ui.theme.DisabledText
import com.studyapp.pomodoro.nativeapp.ui.theme.OnTeal
import com.studyapp.pomodoro.nativeapp.ui.theme.PressedCream
import com.studyapp.pomodoro.nativeapp.ui.theme.RaisedCream

enum class SoftButtonVariant {
    Primary,
    Secondary,
}

@Composable
fun SoftButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: SoftButtonVariant = SoftButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val primary = variant == SoftButtonVariant.Primary
    val shape = RoundedCornerShape(27.dp)
    val contentColor = when {
        !enabled -> DisabledText
        primary -> OnTeal
        else -> DeepTeal
    }
    val surfaceModifier = when {
        !enabled -> Modifier.background(DisabledFill, shape)
        pressed && primary -> Modifier.softInset(DeepTealPressed, 27.dp, 7.dp)
        pressed -> Modifier.softInset(PressedCream, 27.dp, 7.dp)
        primary -> Modifier.softRaised(DeepTeal, 27.dp, 8.dp)
        else -> Modifier.softRaised(RaisedCream, 27.dp, 6.dp)
    }

    Row(
        modifier = modifier
            .height(54.dp)
            .then(surfaceModifier)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun SoftIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 54.dp,
    iconSize: Dp = 20.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(999.dp)
    val iconColor: Color = if (enabled) DeepTeal else DisabledText
    val surfaceModifier = when {
        !enabled -> Modifier.background(DisabledFill, shape)
        pressed -> Modifier.softInset(PressedCream, 999.dp, 7.dp)
        else -> Modifier.softRaised(RaisedCream, 999.dp, 6.dp)
    }

    Row(
        modifier = modifier
            .size(size)
            .then(surfaceModifier)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(iconSize),
        )
    }
}
