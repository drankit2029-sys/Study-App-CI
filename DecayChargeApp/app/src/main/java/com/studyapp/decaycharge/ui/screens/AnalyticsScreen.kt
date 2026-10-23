package com.studyapp.decaycharge.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyapp.decaycharge.domain.DecayCalculator
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.StudySessionRecord
import com.studyapp.decaycharge.model.SubjectSummary
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.navigation.LocalWindowSizeClass
import com.studyapp.decaycharge.ui.AnalyticsUiState
import com.studyapp.decaycharge.ui.AnalyticsViewModel
import com.studyapp.decaycharge.ui.components.BatteryBar
import com.studyapp.decaycharge.ui.components.CalmCard
import com.studyapp.decaycharge.ui.components.FilterPill
import com.studyapp.decaycharge.ui.components.Hairline
import com.studyapp.decaycharge.ui.components.SectionLabel
import com.studyapp.decaycharge.ui.components.ZoneDot
import com.studyapp.decaycharge.ui.components.zoneLabel
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.Ink
import com.studyapp.decaycharge.ui.theme.Mint
import com.studyapp.decaycharge.ui.theme.Muted
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.StoneLight
import com.studyapp.decaycharge.ui.theme.ZoneAmber
import com.studyapp.decaycharge.ui.theme.ZoneGreen
import com.studyapp.decaycharge.ui.theme.ZoneRed
import com.studyapp.decaycharge.ui.theme.ZoneRedSoft
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBackToTriage: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = LocalWindowSizeClass.current
    val horizontalPadding = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
        24.dp
    } else {
        32.dp
    }
    BackHandler { onBackToTriage() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 310.dp),
            modifier = Modifier
                .widthIn(max = 1180.dp)
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 18.dp,
                bottom = 34.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(4.dp))
                ForecastCard(state)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(7.dp))
                SectionLabel(index = "02", title = "SUBJECT BALANCE")
            }
            items(
                items = state.summaries,
                key = { it.subject.id },
            ) { summary ->
                SubjectBalanceCard(summary)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(7.dp))
                SectionLabel(index = "03", title = "RECHARGE LOG")
                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    FilterPill(
                        label = "All",
                        selected = state.sessionFilter == null,
                        onClick = { viewModel.setSessionFilter(null) },
                    )
                    listOf(1 to "Hard", 2 to "Medium", 3 to "Easy").forEach { (rating, label) ->
                        FilterPill(
                            label = label,
                            selected = state.sessionFilter == rating,
                            onClick = { viewModel.setSessionFilter(rating) },
                        )
                    }
                }
            }
            if (state.sessions.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Completed charges will appear here with their battery and stability shift.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    )
                }
            } else {
                items(
                    items = state.sessions.take(30),
                    key = { "session-${it.id}" },
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = { "session" },
                ) { session ->
                    SessionLogCard(session)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                StabilityNote(state)
            }
        }
    }
}

@Composable
private fun ForecastCard(state: AnalyticsUiState) {
    val chartChapters = state.chapters
        .filter { it.status == ChapterStatus.ACTIVE }
        .sortedBy { it.battery }
        .take(3)
    val colors = listOf(Color(0xFFB2574A), Color(0xFFBD8848), Color(0xFF4E7A5A))
    val crossingCount = state.chapters.count { chapter ->
        chapter.status == ChapterStatus.ACTIVE && chapter.battery >= 35f &&
            projectedBattery(chapter, 14) < 35f
    }

    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        elevation = 9.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel(index = "01", title = "14-DAY OUTLOOK") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ZoneRedSoft)
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "PROJECTED",
                        color = Color(0xFFB2574A),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            letterSpacing = 1.1.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (crossingCount == 0) {
                            "Holding steady"
                        } else {
                            "$crossingCount topics drift red"
                        },
                        color = Ink,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 26.sp,
                            lineHeight = 29.sp,
                        ),
                    )
                    Text(
                        text = "If nothing is recharged, these curves meet the 35% line within two weeks.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        ),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${state.readiness}%",
                        color = Forest,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 36.sp,
                            lineHeight = 38.sp,
                            fontFeatureSettings = "lnum",
                        ),
                    )
                    Text(
                        text = "HELD NOW",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            letterSpacing = 1.3.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            DecayChart(
                chapters = chartChapters,
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(174.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "TODAY",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        letterSpacing = 1.2.sp,
                    ),
                )
                Text(
                    text = "7 DAYS",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        letterSpacing = 1.2.sp,
                    ),
                )
                Text(
                    text = "14 DAYS",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        letterSpacing = 1.2.sp,
                    ),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                chartChapters.forEachIndexed { index, chapter ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(colors[index % colors.size], CircleShape),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = chapter.title,
                            color = Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecayChart(
    chapters: List<Chapter>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val chartTop = 7.dp.toPx()
        val chartBottom = size.height - 7.dp.toPx()
        val chartLeft = 31.dp.toPx()
        val chartRight = size.width - 2.dp.toPx()
        val height = chartBottom - chartTop
        val axisTextSize = 8.sp.toPx()
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Muted.toArgb()
            textSize = axisTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }

        listOf(100, 70, 35, 0).forEach { level ->
            val y = chartBottom - height * (level / 100f)
            if (level != 35) {
                drawLine(
                    color = StoneLight,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            drawContext.canvas.nativeCanvas.drawText(
                "$level",
                chartLeft - 7.dp.toPx(),
                y + axisTextSize / 3f,
                axisPaint,
            )
        }
        val thresholdY = chartBottom - height * 0.35f
        drawLine(
            color = ZoneAmber.copy(alpha = 0.55f),
            start = Offset(chartLeft, thresholdY),
            end = Offset(chartRight, thresholdY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(5.dp.toPx(), 5.dp.toPx()),
            ),
        )

        chapters.forEachIndexed { index, chapter ->
            val color = colors[index % colors.size]
            val path = Path()
            val area = Path()
            for (day in 0..14) {
                val x = chartLeft + (chartRight - chartLeft) * (day / 14f)
                val value = projectedBattery(chapter, day).coerceIn(0f, 100f)
                val y = chartBottom - height * (value / 100f)
                if (day == 0) {
                    path.moveTo(x, y)
                    area.moveTo(x, chartBottom)
                    area.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    area.lineTo(x, y)
                }
            }
            area.lineTo(chartRight, chartBottom)
            area.close()
            if (index == 0) {
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.01f)),
                        startY = chartTop,
                        endY = chartBottom,
                    ),
                )
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = if (index == 0) 2.dp.toPx() else 1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
            val startValue = projectedBattery(chapter, 0).coerceIn(0f, 100f)
            val startY = chartBottom - height * (startValue / 100f)
            drawCircle(
                color = color,
                radius = 2.4.dp.toPx(),
                center = Offset(chartLeft, startY),
            )
            val endX = chartRight
            val endValue = projectedBattery(chapter, 14).coerceIn(0f, 100f)
            val endY = chartBottom - height * (endValue / 100f)
            drawCircle(
                color = color,
                radius = 3.dp.toPx(),
                center = Offset(endX, endY),
            )
            drawCircle(
                color = Paper,
                radius = 1.2.dp.toPx(),
                center = Offset(endX, endY),
            )
        }
    }
}

private fun projectedBattery(chapter: Chapter, day: Int): Float = DecayCalculator.projectedBattery(
    currentBattery = chapter.battery,
    stabilityDays = chapter.stabilityDays,
    difficultyWeight = chapter.difficultyWeight,
    daysAhead = day.toFloat(),
    status = chapter.status,
)

@Composable
private fun SubjectBalanceCard(summary: SubjectSummary) {
    val subjectColor = Color(summary.subject.colorHex)
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(subjectColor, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = summary.subject.name,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                    ),
                )
                Text(
                    text = "${summary.averageBattery.toInt()}%",
                    color = subjectColor,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Spacer(Modifier.height(11.dp))
            BatteryBar(
                progress = summary.averageBattery,
                color = subjectColor,
                track = StoneLight,
            )
            Spacer(Modifier.height(11.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    Zone.RED to summary.redCount,
                    Zone.AMBER to summary.amberCount,
                    Zone.GREEN to summary.greenCount,
                    Zone.LOCKED to summary.lockedCount,
                ).forEach { (zone, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZoneDot(zone = zone, size = 4.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$count ${zoneLabel(zone).lowercase()}",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StabilityNote(state: AnalyticsUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        CalmCard(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF0F4EE),
            cornerRadius = 22.dp,
            elevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(17.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Mint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ShowChart,
                        contentDescription = null,
                        tint = Forest,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "STABILITY IS BUILDING",
                        color = Forest,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            letterSpacing = 1.4.sp,
                        ),
                    )
                    Text(
                        text = "Average memory stability is ${"%.1f".format(state.averageStabilityDays)} days across ${state.totalReviewCount} reviews.",
                        color = Ink,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        ),
                    )
                }
            }
        }
    }
}


@Composable
private fun SessionLogCard(session: StudySessionRecord) {
    val ratingColor = when (session.recallRating) {
        1 -> ZoneRed
        2 -> ZoneAmber
        else -> ZoneGreen
    }
    val ratingLabel = when (session.recallRating) {
        1 -> "Hard"
        2 -> "Medium"
        else -> "Easy"
    }
    val date = Instant.ofEpochMilli(session.endedAtEpochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM · HH:mm"))
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.chapterTitle,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                    )
                    Text(
                        text = "${session.subjectName} · $date",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    )
                }
                Text(
                    text = "${session.durationMinutes}m",
                    color = Forest,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ratingColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = ratingLabel.uppercase(),
                        color = ratingColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            letterSpacing = 1.0.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Hairline(color = StoneLight)
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                LogMetric("BATTERY", "${session.batteryBefore.toInt()} → ${session.batteryAfter.toInt()}%")
                LogMetric("STABILITY", "${"%.1f".format(session.stabilityBefore)}d → ${"%.1f".format(session.stabilityAfter)}d")
                LogMetric(
                    "MODE",
                    when (session.mode) {
                        com.studyapp.decaycharge.model.ChargeMode.DEEP_CHARGE -> "Deep"
                        com.studyapp.decaycharge.model.ChargeMode.QUICK_TOP_UP -> "Top-up"
                        com.studyapp.decaycharge.model.ChargeMode.STOPWATCH -> "Stopwatch"
                    },
                )
            }
            session.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(9.dp))
                Text(
                    text = "“$note”",
                    color = Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = MaterialTheme.typography.headlineSmall.fontFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LogMetric(label: String, value: String) {
    Column {
        Text(
            text = value,
            color = Ink,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
        )
        Text(
            text = label,
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp, letterSpacing = 1.0.sp),
        )
    }
}
