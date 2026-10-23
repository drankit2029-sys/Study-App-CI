package com.studyapp.decaycharge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.domain.PlannedCharge
import com.studyapp.decaycharge.domain.PlannedChargeKind
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.CryoFreezeStatus
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.model.ZoneFilter
import com.studyapp.decaycharge.navigation.LocalWindowSizeClass
import com.studyapp.decaycharge.ui.TriageUiState
import com.studyapp.decaycharge.ui.TriageViewModel
import com.studyapp.decaycharge.ui.components.BatteryBar
import com.studyapp.decaycharge.ui.components.BotanicalBranch
import com.studyapp.decaycharge.ui.components.CalmButton
import com.studyapp.decaycharge.ui.components.CalmButtonVariant
import com.studyapp.decaycharge.ui.components.CalmCard
import com.studyapp.decaycharge.ui.components.CalmIconButton
import com.studyapp.decaycharge.ui.components.Eyebrow
import com.studyapp.decaycharge.ui.components.FilterPill
import com.studyapp.decaycharge.ui.components.Hairline
import com.studyapp.decaycharge.ui.components.RingGauge
import com.studyapp.decaycharge.ui.components.SectionLabel
import com.studyapp.decaycharge.ui.components.SubjectTag
import com.studyapp.decaycharge.ui.components.ZoneDot
import com.studyapp.decaycharge.ui.components.ZonePill
import com.studyapp.decaycharge.ui.components.zoneColor
import com.studyapp.decaycharge.ui.components.zoneLabel
import com.studyapp.decaycharge.ui.components.zoneSoftColor
import com.studyapp.decaycharge.ui.theme.Border
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.Ice
import com.studyapp.decaycharge.ui.theme.IceSoft
import com.studyapp.decaycharge.ui.theme.Ink
import com.studyapp.decaycharge.ui.theme.Ivory
import com.studyapp.decaycharge.ui.theme.MintLight
import com.studyapp.decaycharge.ui.theme.Muted
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.Parchment
import com.studyapp.decaycharge.ui.theme.Stone
import com.studyapp.decaycharge.ui.theme.StoneLight
import com.studyapp.decaycharge.ui.theme.White
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriageScreen(
    viewModel: TriageViewModel,
    onOpenCharger: (Long) -> Unit,
    onOpenQuickCharge: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = LocalWindowSizeClass.current
    val columns = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) 2 else 1
    val horizontalPadding = if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) 24.dp else 32.dp
    val snackbarHostState = remember { SnackbarHostState() }
    var showFreezeSheet by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        BotanicalBranch(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 54.dp, y = (-16).dp)
                .size(132.dp),
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
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
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "readiness") {
                    Spacer(Modifier.height(4.dp))
                    ReadinessCard(
                        state = state,
                        onOpenCharger = onOpenCharger,
                        onOpenFreeze = { showFreezeSheet = true },
                        onOpenSettings = onOpenSettings,
                    )
                }
                state.activeFreeze?.let { freeze ->
                    item(span = { GridItemSpan(maxLineSpan) }, contentType = "freeze") {
                        CryoFreezeBanner(
                            freeze = freeze,
                            onOpen = { showFreezeSheet = true },
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "planner") {
                    PlannerCard(
                        state = state,
                        onBudget = viewModel::setDailyBudget,
                        onOpenCharger = onOpenCharger,
                        onOpenQuickCharge = onOpenQuickCharge,
                        onUnlock = viewModel::unlockChapter,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "filters") {
                    ZoneFilters(
                        selected = state.filter,
                        redCount = state.redCount,
                        amberCount = state.amberCount,
                        greenCount = state.greenCount,
                        lockedCount = state.lockedCount,
                        onSelect = viewModel::setFilter,
                    )
                }

                val order = when (state.filter) {
                    ZoneFilter.ALL -> listOf(Zone.RED, Zone.AMBER, Zone.GREEN, Zone.LOCKED)
                    ZoneFilter.RED -> listOf(Zone.RED)
                    ZoneFilter.AMBER -> listOf(Zone.AMBER)
                    ZoneFilter.GREEN -> listOf(Zone.GREEN)
                    ZoneFilter.LOCKED -> listOf(Zone.LOCKED)
                }
                order.forEach { zone ->
                    val zoneChapters = state.chapters
                        .filter { it.zone == zone }
                        .let { zoneList ->
                            if (zone == Zone.RED || zone == Zone.AMBER) {
                                zoneList.sortedByDescending { it.urgencyScore }
                            } else {
                                zoneList.sortedBy { it.battery }
                            }
                        }
                    if (zoneChapters.isNotEmpty()) {
                        item(
                            key = "section-${zone.name}",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "section",
                        ) {
                            Spacer(Modifier.height(9.dp))
                            ZoneSectionHeader(zone = zone, count = zoneChapters.size)
                        }
                        items(
                            items = zoneChapters,
                            key = { it.id },
                            contentType = { "chapter" },
                        ) { chapter ->
                            ChapterCard(
                                chapter = chapter,
                                onOpenCharger = onOpenCharger,
                                onOpenQuickCharge = onOpenQuickCharge,
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                    FooterNote()
                }
            }
        }
        if (showFreezeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFreezeSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Paper,
            ) {
                CryoFreezeSheet(
                    freeze = state.activeFreeze,
                    onFreeze = { hours ->
                        viewModel.startFreeze(hours)
                        showFreezeSheet = false
                    },
                    onExtend = { hours ->
                        viewModel.extendFreeze(hours)
                        showFreezeSheet = false
                    },
                    onEnd = {
                        viewModel.endFreeze()
                        showFreezeSheet = false
                    },
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) { data ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Forest)
                    .padding(horizontal = 18.dp, vertical = 13.dp),
            ) {
                Text(
                    text = data.visuals.message,
                    color = Ivory,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ReadinessCard(
    state: TriageUiState,
    onOpenCharger: (Long) -> Unit,
    onOpenFreeze: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        elevation = 9.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel(index = "01", title = "SYLLABUS READINESS") {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    CalmIconButton(
                        icon = Icons.Rounded.AcUnit,
                        contentDescription = "Cryo-Freeze",
                        onClick = onOpenFreeze,
                        size = 34.dp,
                        iconSize = 15.dp,
                        tint = if (state.activeFreeze != null) Ice else Forest,
                        filled = false,
                    )
                    CalmIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        onClick = onOpenSettings,
                        size = 34.dp,
                        iconSize = 15.dp,
                        filled = false,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RingGauge(
                    progress = state.readiness / 100f,
                    color = Forest,
                    size = 96.dp,
                    strokeWidth = 6.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.readiness}%",
                            color = Ink,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 32.sp,
                                lineHeight = 34.sp,
                                fontFeatureSettings = "lnum",
                            ),
                        )
                        Text(
                            text = "HELD",
                            color = Muted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.sp,
                                letterSpacing = 1.4.sp,
                            ),
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "A steady syllabus.",
                        color = Ink,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            lineHeight = 27.sp,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${state.redCount} topics are fading. A focused recharge now protects the next few days.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(17.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZoneCount(
                    zone = Zone.RED,
                    count = state.redCount,
                    modifier = Modifier.weight(1f),
                )
                ZoneCount(
                    zone = Zone.AMBER,
                    count = state.amberCount,
                    modifier = Modifier.weight(1f),
                )
                ZoneCount(
                    zone = Zone.GREEN,
                    count = state.greenCount,
                    modifier = Modifier.weight(1f),
                )
                ZoneCount(
                    zone = Zone.LOCKED,
                    count = state.lockedCount,
                    modifier = Modifier.weight(1f),
                )
            }
            state.nextCharge?.let { next ->
                Spacer(Modifier.height(16.dp))
                Hairline(color = Border)
                Spacer(Modifier.height(13.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(29.dp)
                            .background(MintLight, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Forest,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXT BEST CHARGE",
                            color = Muted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.sp,
                                letterSpacing = 1.5.sp,
                            ),
                        )
                        Text(
                            text = next.title,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 21.sp,
                            ),
                        )
                    }
                    Text(
                        text = "${next.targetMinutes}m",
                        color = Forest,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Spacer(Modifier.width(9.dp))
                    CalmIconButton(
                        icon = Icons.Rounded.ArrowForward,
                        contentDescription = "Open ${next.title}",
                        onClick = { onOpenCharger(next.id) },
                        size = 34.dp,
                        iconSize = 15.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneCount(
    zone: Zone,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(zoneSoftColor(zone))
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoneDot(zone = zone, size = 5.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = count.toString().padStart(2, '0'),
            color = zoneColor(zone),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = zoneLabel(zone),
            color = zoneColor(zone).copy(alpha = 0.76f),
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 6.sp,
                letterSpacing = 0.7.sp,
            ),
        )
    }
}

@Composable
private fun ZoneFilters(
    selected: ZoneFilter,
    redCount: Int,
    amberCount: Int,
    greenCount: Int,
    lockedCount: Int,
    onSelect: (ZoneFilter) -> Unit,
) {
    val counts = mapOf(
        ZoneFilter.ALL to redCount + amberCount + greenCount + lockedCount,
        ZoneFilter.RED to redCount,
        ZoneFilter.AMBER to amberCount,
        ZoneFilter.GREEN to greenCount,
        ZoneFilter.LOCKED to lockedCount,
    )
    val tints = mapOf(
        ZoneFilter.RED to zoneColorStatic(Zone.RED),
        ZoneFilter.AMBER to zoneColorStatic(Zone.AMBER),
        ZoneFilter.GREEN to zoneColorStatic(Zone.GREEN),
        ZoneFilter.LOCKED to Color(0xFFA4A79D),
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(ZoneFilter.entries.size) { index ->
            val filter = ZoneFilter.entries[index]
            FilterPill(
                label = filter.label,
                count = counts[filter],
                selected = selected == filter,
                tint = tints[filter] ?: Forest,
                onClick = { onSelect(filter) },
            )
        }
    }
}

private fun zoneColorStatic(zone: Zone): Color = when (zone) {
    Zone.RED -> Color(0xFFB2574A)
    Zone.AMBER -> Color(0xFFBD8848)
    Zone.GREEN -> Color(0xFF4E7A5A)
    Zone.LOCKED -> Color(0xFFA4A79D)
}

@Composable
private fun ZoneSectionHeader(
    zone: Zone,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoneDot(zone = zone, size = 7.dp)
        Spacer(Modifier.width(9.dp))
        Column {
            Text(
                text = "${zoneLabel(zone)} ZONE",
                color = Ink,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                ),
            )
            Text(
                text = when (zone) {
                    Zone.RED -> "Charge these first"
                    Zone.AMBER -> "A short top-up will hold"
                    Zone.GREEN -> "Resting well"
                    Zone.LOCKED -> "Waiting to be opened"
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                ),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = count.toString().padStart(2, '0'),
            color = zoneColor(zone),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun ChapterCard(
    chapter: Chapter,
    onOpenCharger: (Long) -> Unit,
    onOpenQuickCharge: (Long) -> Unit,
) {
    val accent = zoneColor(chapter.zone)
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectTag(
                    shortName = chapter.subjectShortName,
                    color = Color(chapter.subjectColorHex),
                )
                Spacer(Modifier.weight(1f))
                ZonePill(zone = chapter.zone)
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.unitName.uppercase(),
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            letterSpacing = 1.5.sp,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = chapter.title,
                        color = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 25.sp,
                            lineHeight = 28.sp,
                        ),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${chapter.battery.toInt()}%",
                    color = accent,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        fontFeatureSettings = "lnum",
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            BatteryBar(
                progress = chapter.battery,
                color = accent,
                track = zoneSoftColor(chapter.zone),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                MetaText(
                    label = if (chapter.zone == Zone.LOCKED) "NOT STARTED"
                    else "${chapter.daysSinceCharge} DAYS QUIET",
                )
                MetaText(label = "S ${chapter.stabilityDays.formatOne()}D")
                MetaText(label = chapter.difficultyLabel.uppercase())
                MetaText(label = "${"%.1f".format(chapter.drainRatePerDay)}%/DAY")
            }
            if (!chapter.note.isNullOrBlank()) {
                Spacer(Modifier.height(13.dp))
                Text(
                    text = "“${chapter.note}”",
                    color = Muted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = MaterialTheme.typography.headlineSmall.fontFamily,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))
            if (chapter.status == ChapterStatus.LOCKED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StoneLight)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Muted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "OPEN IN SYLLABUS",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 1.3.sp,
                        ),
                    )
                }
            } else {
                CalmButton(
                    label = if (chapter.zone == Zone.AMBER) {
                        "Quick top-up · 15m"
                    } else {
                        "Charge · ${chapter.targetMinutes}m"
                    },
                    onClick = {
                        if (chapter.zone == Zone.AMBER) {
                            onOpenQuickCharge(chapter.id)
                        } else {
                            onOpenCharger(chapter.id)
                        }
                    },
                    trailingIcon = Icons.Rounded.ArrowForward,
                    variant = if (chapter.zone == Zone.RED) {
                        CalmButtonVariant.Primary
                    } else {
                        CalmButtonVariant.Secondary
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MetaText(label: String) {
    Text(
        text = label,
        color = Muted,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 7.sp,
            letterSpacing = 1.1.sp,
        ),
    )
}

@Composable
private fun FooterNote() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(18.dp))
        Hairline(modifier = Modifier.width(32.dp), color = Stone)
        Spacer(Modifier.height(12.dp))
        Eyebrow("A SOFTER WAY TO REMEMBER", color = Muted)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Review before the forgetting curve steepens. Small charges, held often.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                lineHeight = 15.sp,
            ),
        )
    }
}

private fun Float.formatOne(): String = ((this * 10).toInt() / 10f).toString()


@Composable
private fun PlannerCard(
    state: TriageUiState,
    onBudget: (Int) -> Unit,
    onOpenCharger: (Long) -> Unit,
    onOpenQuickCharge: (Long) -> Unit,
    onUnlock: (Long) -> Unit,
) {
    val totalMinutes = state.plan.sumOf { it.minutes }
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        elevation = 5.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            SectionLabel(index = "02", title = "TODAY'S CHARGING PLAYLIST") {
                Text(
                    text = "${totalMinutes}m / ${state.dailyBudgetMinutes}m",
                    color = Forest,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 0.8.sp,
                    ),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    state.daysUntilExam == null -> "Set an exam date in Settings to sharpen priorities."
                    state.daysUntilExam <= 30 ->
                        "Exam in ${state.daysUntilExam} days · crunch scaling is active."
                    else -> "Exam in ${state.daysUntilExam} days."
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                listOf(60, 120, 180, 240, 360).forEach { minutes ->
                    FilterPill(
                        label = if (minutes < 60) "${minutes}m" else "${minutes / 60}h",
                        selected = state.dailyBudgetMinutes == minutes,
                        onClick = { onBudget(minutes) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (state.plan.isEmpty()) {
                Text(
                    text = "No charge is needed today. Let the garden rest.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                )
            } else {
                state.plan.take(3).forEachIndexed { index, planned ->
                    if (index > 0) {
                        Spacer(Modifier.height(5.dp))
                        Hairline(color = Border)
                        Spacer(Modifier.height(5.dp))
                    }
                    PlannedRow(
                        planned = planned,
                        onOpenCharger = onOpenCharger,
                        onOpenQuickCharge = onOpenQuickCharge,
                        onUnlock = onUnlock,
                    )
                }
                if (state.plan.size > 3) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "+${state.plan.size - 3} more planned charges",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedRow(
    planned: PlannedCharge,
    onOpenCharger: (Long) -> Unit,
    onOpenQuickCharge: (Long) -> Unit,
    onUnlock: (Long) -> Unit,
) {
    val chapter = planned.chapter
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                when (planned.kind) {
                    PlannedChargeKind.AMBER_TOP_UP -> onOpenQuickCharge(chapter.id)
                    PlannedChargeKind.UNLOCK -> onUnlock(chapter.id)
                    PlannedChargeKind.RED_CHARGE -> onOpenCharger(chapter.id)
                }
            }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(zoneColor(chapter.zone), CircleShape),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
            )
            Text(
                text = when (planned.kind) {
                    PlannedChargeKind.RED_CHARGE -> "Priority charge"
                    PlannedChargeKind.AMBER_TOP_UP -> "Quick top-up"
                    PlannedChargeKind.UNLOCK -> "Unlock one new chapter"
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            )
        }
        Text(
            text = "${planned.minutes}m",
            color = Forest,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.width(5.dp))
        Icon(
            imageVector = Icons.Rounded.ArrowForward,
            contentDescription = "Open ${chapter.title}",
            tint = Forest,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun CryoFreezeBanner(
    freeze: CryoFreezeStatus,
    onOpen: () -> Unit,
) {
    val detail = freeze.endEpochMs?.let { end ->
        val formatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")
        "All decay paused until ${Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).format(formatter)}."
    } ?: "All decay paused until you end the freeze."
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(IceSoft)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.AcUnit,
            contentDescription = null,
            tint = Ice,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CRYO-FREEZE ACTIVE",
                color = Ice,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    letterSpacing = 1.4.sp,
                ),
            )
            Text(
                text = detail,
                color = Ink,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            )
        }
        Icon(
            imageVector = Icons.Rounded.ArrowForward,
            contentDescription = "Manage Cryo-Freeze",
            tint = Ice,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun CryoFreezeSheet(
    freeze: CryoFreezeStatus?,
    onFreeze: (Int?) -> Unit,
    onExtend: (Int) -> Unit,
    onEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
    ) {
        Eyebrow("CRYO-FREEZE", color = Ice)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (freeze == null) "Pause the garden." else "The garden is resting.",
            color = Ink,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (freeze == null) {
                "Vacation, illness, or a deliberate break. Frozen hours are subtracted from every chapter's decay."
            } else {
                "No battery loses charge while Cryo-Freeze is active. You can end it at any time."
            },
            color = Muted,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
        )
        Spacer(Modifier.height(18.dp))
        if (freeze == null) {
            FreezeChoice(
                title = "Freeze for 24 hours",
                subtitle = "A short recovery day.",
                onClick = { onFreeze(24) },
            )
            Spacer(Modifier.height(8.dp))
            FreezeChoice(
                title = "Freeze for 3 days",
                subtitle = "A long weekend or sick leave.",
                onClick = { onFreeze(72) },
            )
            Spacer(Modifier.height(8.dp))
            FreezeChoice(
                title = "Indefinite manual freeze",
                subtitle = "Stay paused until you return.",
                onClick = { onFreeze(null) },
            )
        } else {
            CalmButton(
                label = "End Cryo-Freeze",
                onClick = onEnd,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!freeze.isIndefinite) {
                Spacer(Modifier.height(9.dp))
                FreezeChoice(
                    title = "Extend by 24 hours",
                    subtitle = "Adds a day to the current freeze.",
                    onClick = { onExtend(24) },
                )
            }
        }
    }
}

@Composable
private fun FreezeChoice(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(Parchment.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(IceSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AcUnit,
                contentDescription = null,
                tint = Ice,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Ink,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
            )
            Text(
                text = subtitle,
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            )
        }
        Icon(
            imageVector = Icons.Rounded.ArrowForward,
            contentDescription = null,
            tint = Ice,
            modifier = Modifier.size(15.dp),
        )
    }
}
