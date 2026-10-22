package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studyapp.pomodoro.nativeapp.ui.theme.InsetCream
import com.studyapp.pomodoro.nativeapp.ui.theme.InsetShadowDark
import com.studyapp.pomodoro.nativeapp.ui.theme.InsetShadowLight
import com.studyapp.pomodoro.nativeapp.ui.theme.RaisedCream
import com.studyapp.pomodoro.nativeapp.ui.theme.ShadowDark

fun Modifier.softRaised(
    color: Color = RaisedCream,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val highlight = if (color == RaisedCream) Color.White.copy(alpha = 0.45f) else Color.Transparent
    return shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = ShadowDark.copy(alpha = 0.45f),
        spotColor = ShadowDark,
    )
        .background(color, shape)
        .border(1.dp, highlight, shape)
}

fun Modifier.softInset(
    color: Color = InsetCream,
    cornerRadius: Dp = 8.dp,
    depth: Dp = 10.dp,
    strength: Float = 1f,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return clip(shape).drawBehind {
        drawRect(color)
        val corner = cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
        val edge = depth.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
        val dark = InsetShadowDark.copy(alpha = InsetShadowDark.alpha * strength)
        val light = InsetShadowLight.copy(alpha = InsetShadowLight.alpha * strength)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(dark, Color.Transparent),
                startY = 0f,
                endY = edge,
            ),
            size = Size(size.width, edge),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(dark, Color.Transparent),
                startX = 0f,
                endX = edge,
            ),
            size = Size(edge, size.height),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, light),
                startY = size.height - edge,
                endY = size.height,
            ),
            topLeft = Offset(0f, size.height - edge),
            size = Size(size.width, edge),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, light),
                startX = size.width - edge,
                endX = size.width,
            ),
            topLeft = Offset(size.width - edge, 0f),
            size = Size(edge, size.height),
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent, dark),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
