package com.studyapp.pomodoro.nativeapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.studyapp.pomodoro.nativeapp.ui.theme.Forest
import com.studyapp.pomodoro.nativeapp.ui.theme.Mint

@Composable
fun BotanicalBranch(
    modifier: Modifier = Modifier,
    color: Color = Forest,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stem = Path().apply {
            moveTo(w * 0.98f, h * 0.02f)
            cubicTo(w * 0.84f, h * 0.06f, w * 0.72f, h * 0.18f, w * 0.64f, h * 0.32f)
            cubicTo(w * 0.56f, h * 0.46f, w * 0.42f, h * 0.55f, w * 0.27f, h * 0.60f)
            cubicTo(w * 0.18f, h * 0.63f, w * 0.10f, h * 0.64f, w * 0.03f, h * 0.62f)
        }
        drawPath(
            path = stem,
            color = color.copy(alpha = 0.42f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawLeaf(Offset(w * 0.87f, h * 0.10f), -38f, w * 0.21f, w * 0.075f, color.copy(alpha = 0.24f))
        drawLeaf(Offset(w * 0.78f, h * 0.16f), -72f, w * 0.18f, w * 0.065f, Mint.copy(alpha = 0.58f))
        drawLeaf(Offset(w * 0.72f, h * 0.24f), -12f, w * 0.19f, w * 0.070f, color.copy(alpha = 0.22f))
        drawLeaf(Offset(w * 0.66f, h * 0.31f), -60f, w * 0.17f, w * 0.060f, Mint.copy(alpha = 0.55f))
        drawLeaf(Offset(w * 0.59f, h * 0.40f), 24f, w * 0.18f, w * 0.065f, color.copy(alpha = 0.20f))
        drawLeaf(Offset(w * 0.51f, h * 0.48f), -44f, w * 0.16f, w * 0.058f, Mint.copy(alpha = 0.52f))
        drawLeaf(Offset(w * 0.41f, h * 0.54f), 32f, w * 0.17f, w * 0.060f, color.copy(alpha = 0.18f))
        drawLeaf(Offset(w * 0.31f, h * 0.58f), -28f, w * 0.15f, w * 0.055f, Mint.copy(alpha = 0.48f))
        drawLeaf(Offset(w * 0.21f, h * 0.61f), 38f, w * 0.14f, w * 0.052f, color.copy(alpha = 0.16f))
    }
}

private fun DrawScope.drawLeaf(
    center: Offset,
    angle: Float,
    length: Float,
    width: Float,
    color: Color,
) {
    rotate(degrees = angle, pivot = center) {
        drawOval(
            color = color,
            topLeft = Offset(center.x - length / 2f, center.y - width / 2f),
            size = Size(length, width),
        )
        drawLine(
            color = color.copy(alpha = (color.alpha * 0.75f).coerceAtMost(1f)),
            start = Offset(center.x - length * 0.42f, center.y),
            end = Offset(center.x + length * 0.42f, center.y),
            strokeWidth = 0.6.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
