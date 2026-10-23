package com.studyapp.decaycharge.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.StoneLight
import com.studyapp.decaycharge.ui.theme.White
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun BatteryCell(
    progress: Float,
    title: String,
    zone: Zone,
    modifier: Modifier = Modifier,
    subtle: Boolean = false,
) {
    val target = (progress / 100f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900),
        label = "batteryLevel",
    )
    val wave by rememberInfiniteTransition(label = "batteryWave").animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )
    val levelColor = zoneColor(zone)
    val zoneLabel = when (zone) {
        Zone.RED -> "Red Zone"
        Zone.AMBER -> "Amber Zone"
        Zone.GREEN -> "Green Zone"
        Zone.LOCKED -> "Locked"
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "$title, $zoneLabel, battery at ${progress.toInt()} percent"
                },
        ) {
            val housingWidth = size.width * 0.72f
            val housingHeight = size.height * 0.88f
            val housingLeft = (size.width - housingWidth) / 2f
            val housingTop = size.height * 0.09f
            val terminalWidth = housingWidth * 0.32f
            val terminalHeight = size.height * 0.035f
            val corner = housingWidth * 0.15f

            drawRoundRect(
                color = if (subtle) Color.Transparent else White.copy(alpha = 0.56f),
                topLeft = Offset(housingLeft, housingTop),
                size = Size(housingWidth, housingHeight),
                cornerRadius = CornerRadius(corner, corner),
            )
            drawRoundRect(
                color = levelColor.copy(alpha = 0.17f),
                topLeft = Offset(housingLeft + 3.dp.toPx(), housingTop + 3.dp.toPx()),
                size = Size(housingWidth - 6.dp.toPx(), housingHeight - 6.dp.toPx()),
                cornerRadius = CornerRadius(corner * 0.88f, corner * 0.88f),
            )
            drawRoundRect(
                color = levelColor.copy(alpha = 0.55f),
                topLeft = Offset(size.width / 2f - terminalWidth / 2f, housingTop - terminalHeight),
                size = Size(terminalWidth, terminalHeight + 2.dp.toPx()),
                cornerRadius = CornerRadius(terminalHeight, terminalHeight),
            )
            drawRoundRect(
                color = levelColor.copy(alpha = 0.42f),
                topLeft = Offset(housingLeft, housingTop),
                size = Size(housingWidth, housingHeight),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = 1.4.dp.toPx()),
            )

            val innerLeft = housingLeft + 7.dp.toPx()
            val innerTop = housingTop + 7.dp.toPx()
            val innerWidth = housingWidth - 14.dp.toPx()
            val innerHeight = housingHeight - 14.dp.toPx()
            val innerCorner = (corner - 3.dp.toPx()).coerceAtLeast(2.dp.toPx())
            val liquidTop = innerTop + innerHeight * (1f - animated)
            val clip = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = innerLeft,
                        top = innerTop,
                        right = innerLeft + innerWidth,
                        bottom = innerTop + innerHeight,
                        cornerRadius = CornerRadius(innerCorner, innerCorner),
                    ),
                )
            }

            if (animated > 0.005f) {
                clipPath(clip) {
                    val waveHeight = (innerHeight * 0.035f).coerceAtLeast(2.dp.toPx())
                    val wave = Path().apply {
                        moveTo(innerLeft - 12.dp.toPx(), liquidTop)
                        var x = innerLeft - 12.dp.toPx()
                        while (x <= innerLeft + innerWidth + 12.dp.toPx()) {
                            val y = liquidTop + sin(
                                ((x - innerLeft) / innerWidth * 2.2f * PI + wave).toFloat(),
                            ) * waveHeight
                            lineTo(x, y)
                            x += 4.dp.toPx()
                        }
                        lineTo(innerLeft + innerWidth + 12.dp.toPx(), innerTop + innerHeight)
                        lineTo(innerLeft - 12.dp.toPx(), innerTop + innerHeight)
                        close()
                    }
                    drawPath(
                        path = wave,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                levelColor.copy(alpha = 0.92f),
                                levelColor.copy(alpha = 0.72f),
                            ),
                            startY = liquidTop,
                            endY = innerTop + innerHeight,
                        ),
                    )

                    val bubbleColor = White.copy(alpha = 0.42f)
                    repeat(5) { index ->
                        val fraction = (index + 1) / 6f
                        val bubbleCenter = Offset(
                            x = innerLeft + innerWidth * fraction,
                            y = innerTop + innerHeight * (1f - animated * (0.22f + index * 0.12f)),
                        )
                        drawCircle(
                            color = bubbleColor,
                            radius = (1.2f + index * 0.38f).dp.toPx(),
                            center = bubbleCenter,
                        )
                    }
                    drawLine(
                        color = White.copy(alpha = 0.45f),
                        start = Offset(innerLeft + innerWidth * 0.26f, liquidTop + innerHeight * 0.12f),
                        end = Offset(innerLeft + innerWidth * 0.26f, innerTop + innerHeight * 0.82f),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            drawRoundRect(
                color = Forest.copy(alpha = 0.10f),
                topLeft = Offset(housingLeft + housingWidth * 0.18f, housingTop + housingHeight * 0.92f),
                size = Size(housingWidth * 0.64f, 2.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

@Composable
fun BatteryPlate(
    progress: Float,
    title: String,
    zone: Zone,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .background(Paper, RoundedCornerShape(30.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        BatteryCell(
            progress = progress,
            title = title,
            zone = zone,
            modifier = Modifier.fillMaxSize(),
            subtle = false,
        )
        content()
    }
}
