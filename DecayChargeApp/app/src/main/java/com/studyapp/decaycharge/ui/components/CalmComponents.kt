package com.studyapp.decaycharge.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.ui.theme.Border
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.ForestDeep
import com.studyapp.decaycharge.ui.theme.Ink
import com.studyapp.decaycharge.ui.theme.Ivory
import com.studyapp.decaycharge.ui.theme.Mint
import com.studyapp.decaycharge.ui.theme.Muted
import com.studyapp.decaycharge.ui.theme.MutedLight
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.Parchment
import com.studyapp.decaycharge.ui.theme.ShadowWarm
import com.studyapp.decaycharge.ui.theme.Stone
import com.studyapp.decaycharge.ui.theme.StoneLight
import com.studyapp.decaycharge.ui.theme.White
import com.studyapp.decaycharge.ui.theme.ZoneAmber
import com.studyapp.decaycharge.ui.theme.ZoneAmberSoft
import com.studyapp.decaycharge.ui.theme.ZoneGreen
import com.studyapp.decaycharge.ui.theme.ZoneGreenSoft
import com.studyapp.decaycharge.ui.theme.ZoneRed
import com.studyapp.decaycharge.ui.theme.ZoneRedSoft
import kotlin.math.cos
import kotlin.math.sin

fun Modifier.softRaised(
    color: Color = Paper,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp,
    highlight: Boolean = true,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    var result = shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = ShadowWarm.copy(alpha = 0.45f),
        spotColor = ShadowWarm,
    ).background(color, shape)
    if (highlight) {
        result = result.border(1.dp, White.copy(alpha = 0.72f), shape)
    }
    return result
}

fun Modifier.softInset(
    color: Color = StoneLight,
    cornerRadius: Dp = 8.dp,
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return clip(shape).background(color).border(1.dp, White.copy(alpha = 0.65f), shape)
}

@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = Stone,
) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(color),
    )
}

@Composable
fun SectionLabel(
    index: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index,
            color = Forest,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 1.2.sp,
            ),
        )
        Text(
            text = title,
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 1.6.sp,
            ),
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Forest,
) {
    Text(
        text = text,
        color = color,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 8.sp,
            letterSpacing = 2.3.sp,
        ),
    )
}

@Composable
fun CalmCard(
    modifier: Modifier = Modifier,
    color: Color = Paper,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.softRaised(color, cornerRadius, elevation),
        content = content,
    )
}

@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    tint: Color = Forest,
) {
    val shape = RoundedCornerShape(999.dp)
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) Forest else Paper, shape)
            .border(1.dp, if (selected) Forest else Border, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!selected && tint != Forest) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(tint, CircleShape),
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = label,
            color = if (selected) Ivory else Muted,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            ),
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString().padStart(2, '0'),
                color = if (selected) Mint else MutedLight,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    letterSpacing = 0.6.sp,
                ),
            )
        }
    }
}

@Composable
fun SubjectTag(
    shortName: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f), shape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Text(
            text = shortName,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 1.1.sp,
            ),
        )
    }
}

@Composable
fun ZonePill(
    zone: Zone,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(zoneSoftColor(zone), shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(zoneColor(zone), CircleShape),
        )
        Text(
            text = zoneLabel(zone),
            color = zoneColor(zone),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                letterSpacing = 1.0.sp,
            ),
        )
    }
}

@Composable
fun ZoneDot(
    zone: Zone,
    modifier: Modifier = Modifier,
    size: Dp = 7.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(zoneColor(zone), CircleShape),
    )
}

@Composable
fun zoneColor(zone: Zone): Color = when (zone) {
    Zone.RED -> ZoneRed
    Zone.AMBER -> ZoneAmber
    Zone.GREEN -> ZoneGreen
    Zone.LOCKED -> MutedLight
}

@Composable
fun zoneSoftColor(zone: Zone): Color = when (zone) {
    Zone.RED -> ZoneRedSoft
    Zone.AMBER -> ZoneAmberSoft
    Zone.GREEN -> ZoneGreenSoft
    Zone.LOCKED -> StoneLight
}

fun zoneLabel(zone: Zone): String = when (zone) {
    Zone.RED -> "RED"
    Zone.AMBER -> "AMBER"
    Zone.GREEN -> "GREEN"
    Zone.LOCKED -> "LOCKED"
}

@Composable
fun BatteryBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    track: Color = StoneLight,
    height: Dp = 6.dp,
    animate: Boolean = true,
) {
    val target = (progress / 100f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (animate) 700 else 0),
        label = "batteryBar",
    )
    Canvas(
        modifier = modifier
            .height(height)
            .semantics {
                contentDescription = "${progress.toInt()} percent charged"
            },
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = track, cornerRadius = radius)
        if (animated > 0f) {
            drawRoundRect(
                color = color,
                size = Size(size.width * animated, size.height),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
fun RingGauge(
    progress: Float,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 6.dp,
    trackColor: Color = StoneLight,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val clamped = progress.coerceIn(0f, 1f)
            if (clamped > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (clamped < 0.995f) {
                    val radius = diameter / 2f
                    val angle = Math.toRadians((-90f + 360f * clamped).toDouble())
                    drawCircle(
                        color = Mint,
                        radius = stroke * 0.56f,
                        center = Offset(
                            x = center.x + (cos(angle) * radius).toFloat(),
                            y = center.y + (sin(angle) * radius).toFloat(),
                        ),
                    )
                }
            }
        }
        content()
    }
}

enum class CalmButtonVariant {
    Primary,
    Secondary,
    Quiet,
}

@Composable
fun CalmButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    variant: CalmButtonVariant = CalmButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(27.dp)
    val contentColor = when {
        !enabled -> MutedLight
        variant == CalmButtonVariant.Primary -> Ivory
        else -> Ink
    }
    val surface = when {
        !enabled -> Modifier.background(StoneLight, shape)
        pressed && variant == CalmButtonVariant.Primary -> Modifier.background(ForestDeep, shape)
        pressed -> Modifier.background(StoneLight, shape)
        variant == CalmButtonVariant.Primary ->
            Modifier.softRaised(Forest, 27.dp, 6.dp, highlight = false)
        variant == CalmButtonVariant.Quiet -> Modifier.background(Parchment, shape)
        else -> Modifier.softRaised(Paper, 27.dp, 4.dp)
    }

    Row(
        modifier = modifier
            .height(52.dp)
            .then(surface)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (trailingIcon != null) {
            Spacer(Modifier.width(9.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
fun CalmIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 17.dp,
    tint: Color = Forest,
    filled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val shape = CircleShape
    val surface = when {
        !enabled -> Modifier.background(StoneLight, shape)
        pressed -> Modifier.background(StoneLight, shape)
        filled -> Modifier.softRaised(Paper, 999.dp, 4.dp)
        else -> Modifier.background(Color.Transparent, shape)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(surface)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else MutedLight,
            modifier = Modifier.size(iconSize),
        )
    }
}

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
            color = color.copy(alpha = 0.38f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
        )
        drawLeaf(Offset(w * 0.87f, h * 0.10f), -38f, w * 0.21f, w * 0.075f, color.copy(alpha = 0.22f))
        drawLeaf(Offset(w * 0.78f, h * 0.16f), -72f, w * 0.18f, w * 0.065f, Mint.copy(alpha = 0.56f))
        drawLeaf(Offset(w * 0.72f, h * 0.24f), -12f, w * 0.19f, w * 0.070f, color.copy(alpha = 0.20f))
        drawLeaf(Offset(w * 0.66f, h * 0.31f), -60f, w * 0.17f, w * 0.060f, Mint.copy(alpha = 0.52f))
        drawLeaf(Offset(w * 0.59f, h * 0.40f), 24f, w * 0.18f, w * 0.065f, color.copy(alpha = 0.18f))
        drawLeaf(Offset(w * 0.51f, h * 0.48f), -44f, w * 0.16f, w * 0.058f, Mint.copy(alpha = 0.48f))
        drawLeaf(Offset(w * 0.41f, h * 0.54f), 32f, w * 0.17f, w * 0.060f, color.copy(alpha = 0.16f))
        drawLeaf(Offset(w * 0.31f, h * 0.58f), -28f, w * 0.15f, w * 0.055f, Mint.copy(alpha = 0.45f))
        drawLeaf(Offset(w * 0.21f, h * 0.61f), 38f, w * 0.14f, w * 0.052f, color.copy(alpha = 0.15f))
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

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(eyebrow)
            if (trailing != null) {
                Spacer(Modifier.weight(1f))
                trailing()
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            color = Ink,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 46.sp,
                lineHeight = 48.sp,
            ),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(17.dp))
        Hairline(modifier = Modifier.width(40.dp))
    }
}
