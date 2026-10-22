package com.studyapp.decaycharge.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.SubjectSummary
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.model.ZoneFilter
import com.studyapp.decaycharge.navigation.LocalWindowSizeClass
import com.studyapp.decaycharge.ui.SyllabusUiState
import com.studyapp.decaycharge.ui.SyllabusViewModel
import com.studyapp.decaycharge.ui.components.BatteryBar
import com.studyapp.decaycharge.ui.components.CalmCard
import com.studyapp.decaycharge.ui.components.CalmIconButton
import com.studyapp.decaycharge.ui.components.FilterPill
import com.studyapp.decaycharge.ui.components.Hairline
import com.studyapp.decaycharge.ui.components.PageHeader
import com.studyapp.decaycharge.ui.components.SectionLabel
import com.studyapp.decaycharge.ui.components.ZoneDot
import com.studyapp.decaycharge.ui.components.zoneColor
import com.studyapp.decaycharge.ui.components.zoneLabel
import com.studyapp.decaycharge.ui.components.zoneSoftColor
import com.studyapp.decaycharge.ui.theme.Border
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.Ink
import com.studyapp.decaycharge.ui.theme.Ivory
import com.studyapp.decaycharge.ui.theme.Mint
import com.studyapp.decaycharge.ui.theme.Muted
import com.studyapp.decaycharge.ui.theme.MutedLight
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.Stone
import com.studyapp.decaycharge.ui.theme.StoneLight
import com.studyapp.decaycharge.ui.theme.ZoneAmber
import com.studyapp.decaycharge.ui.theme.ZoneGreen
import com.studyapp.decaycharge.ui.theme.ZoneRed
import kotlinx.coroutines.launch

@Composable
fun SyllabusScreen(
    viewModel: SyllabusViewModel,
    onBackToTriage: () -> Unit,
    onOpenCharger: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = LocalWindowSizeClass.current
    val widthClass = windowSizeClass.widthSizeClass
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler {
        if (state.hasNarrowing) {
            viewModel.clearNarrowing()
        } else {
            onBackToTriage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 1180.dp)
                .fillMaxHeight()
                .imePadding()
                .padding(
                    start = if (widthClass == WindowWidthSizeClass.Compact) 24.dp else 32.dp,
                    end = if (widthClass == WindowWidthSizeClass.Compact) 24.dp else 32.dp,
                    top = 18.dp,
                ),
        ) {
            PageHeader(
                eyebrow = "DECAY / CHARGE",
                title = "Syllabus",
                subtitle = "Every subject, softly accounted for.",
                trailing = {
                    CalmIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Import syllabus",
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Bulk syllabus import arrives in Phase 3.",
                                )
                            }
                        },
                    )
                },
            )
            Spacer(Modifier.height(17.dp))
            SearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
            )
            Spacer(Modifier.height(10.dp))
            SyllabusFilters(
                selected = state.filter,
                onSelect = viewModel::setFilter,
            )
            Spacer(Modifier.height(13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.hasNarrowing) {
                        "${state.visibleChapters} of ${state.totalChapters} chapters"
                    } else {
                        "${state.totalSubjects} subjects · ${state.totalChapters} chapters"
                    },
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                )
                Spacer(Modifier.weight(1f))
                if (state.hasNarrowing) {
                    Text(
                        text = "CLEAR",
                        color = Forest,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.clearNarrowing() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 1.2.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    state.subjects.isEmpty() -> {
                        EmptySyllabus(hasNarrowing = state.hasNarrowing)
                    }
                    widthClass == WindowWidthSizeClass.Expanded -> {
                        MasterDetailSyllabus(
                            state = state,
                            onSelectSubject = viewModel::selectSubject,
                            onOpenCharger = onOpenCharger,
                        )
                    }
                    widthClass == WindowWidthSizeClass.Medium -> {
                        MediumSyllabusGrid(
                            state = state,
                            onToggleExpanded = viewModel::toggleExpanded,
                            onOpenCharger = onOpenCharger,
                        )
                    }
                    else -> {
                        CompactSyllabusList(
                            state = state,
                            onToggleExpanded = viewModel::toggleExpanded,
                            onOpenCharger = onOpenCharger,
                        )
                    }
                }
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
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Paper)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Ink,
                fontSize = 13.sp,
            ),
            cursorBrush = SolidColor(Forest),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search subjects, units, chapters",
                            color = MutedLight,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable {
                        onValueChange("")
                        focusManager.clearFocus()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = Muted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun SyllabusFilters(
    selected: ZoneFilter,
    onSelect: (ZoneFilter) -> Unit,
) {
    val colors = mapOf(
        ZoneFilter.RED to ZoneRed,
        ZoneFilter.AMBER to ZoneAmber,
        ZoneFilter.GREEN to ZoneGreen,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ZoneFilter.entries.forEach { filter ->
            FilterPill(
                label = filter.label,
                selected = selected == filter,
                tint = colors[filter] ?: Forest,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun CompactSyllabusList(
    state: SyllabusUiState,
    onToggleExpanded: (Long) -> Unit,
    onOpenCharger: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = state.subjects,
            key = { it.subject.id },
        ) { summary ->
            SubjectCard(
                summary = summary,
                expanded = summary.subject.id in state.expandedSubjectIds,
                onToggleExpanded = { onToggleExpanded(summary.subject.id) },
                onOpenCharger = onOpenCharger,
            )
        }
    }
}

@Composable
private fun MediumSyllabusGrid(
    state: SyllabusUiState,
    onToggleExpanded: (Long) -> Unit,
    onOpenCharger: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = state.subjects,
            key = { it.subject.id },
        ) { summary ->
            SubjectCard(
                summary = summary,
                expanded = summary.subject.id in state.expandedSubjectIds,
                onToggleExpanded = { onToggleExpanded(summary.subject.id) },
                onOpenCharger = onOpenCharger,
            )
        }
    }
}

@Composable
private fun SubjectCard(
    summary: SubjectSummary,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onOpenCharger: (Long) -> Unit,
) {
    val subjectColor = Color(summary.subject.colorHex)
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevation = 5.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(start = 17.dp, end = 15.dp, top = 16.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(subjectColor.copy(alpha = 0.11f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(subjectColor, CircleShape),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.subject.name,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 24.sp,
                            lineHeight = 26.sp,
                        ),
                    )
                    Text(
                        text = "${summary.chapters.size} chapters · ${summary.subject.shortName}",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    )
                }
                Text(
                    text = "${summary.averageBattery.toInt()}%",
                    color = subjectColor,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 25.sp,
                        lineHeight = 27.sp,
                    ),
                )
                Spacer(Modifier.width(7.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Muted,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.padding(horizontal = 17.dp)) {
                BatteryBar(
                    progress = summary.averageBattery,
                    color = subjectColor,
                    track = StoneLight,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZoneCountChip(summary.redCount, Zone.RED)
                    ZoneCountChip(summary.amberCount, Zone.AMBER)
                    ZoneCountChip(summary.greenCount, Zone.GREEN)
                    if (summary.lockedCount > 0) {
                        ZoneCountChip(summary.lockedCount, Zone.LOCKED)
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(13.dp))
                    Hairline(color = Border)
                    summary.chapters.forEach { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            onOpenCharger = onOpenCharger,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ZoneCountChip(count: Int, zone: Zone) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(zoneSoftColor(zone))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoneDot(zone = zone, size = 4.dp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = count.toString(),
            color = zoneColor(zone),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp,
                letterSpacing = 0.4.sp,
            ),
        )
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    onOpenCharger: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = chapter.isChargeable,
                onClick = { onOpenCharger(chapter.id) },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ZoneDot(zone = chapter.zone)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                color = if (chapter.isChargeable) Ink else Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = "${chapter.unitName} · ${zoneLabel(chapter.zone)}",
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            )
        }
        Spacer(Modifier.width(8.dp))
        if (chapter.isChargeable) {
            Text(
                text = "${chapter.battery.toInt()}%",
                color = zoneColor(chapter.zone),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.width(7.dp))
            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                tint = MutedLight,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Locked",
                tint = MutedLight,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

private val Chapter.isChargeable: Boolean
    get() = status != com.studyapp.decaycharge.model.ChapterStatus.LOCKED

@Composable
private fun MasterDetailSyllabus(
    state: SyllabusUiState,
    onSelectSubject: (Long) -> Unit,
    onOpenCharger: (Long) -> Unit,
) {
    val selected = state.selectedSummary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .width(310.dp)
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = state.subjects,
                key = { it.subject.id },
            ) { summary ->
                SubjectRailRow(
                    summary = summary,
                    selected = summary.subject.id == selected?.subject?.id,
                    onClick = { onSelectSubject(summary.subject.id) },
                )
            }
        }
        if (selected != null) {
            SubjectDetailPane(
                summary = selected,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onOpenCharger = onOpenCharger,
            )
        }
    }
}

@Composable
private fun SubjectRailRow(
    summary: SubjectSummary,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val subjectColor = Color(summary.subject.colorHex)
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Paper else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(subjectColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(subjectColor, CircleShape),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary.subject.name,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
            )
            Text(
                text = "${summary.chapters.size} chapters",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            )
        }
        Text(
            text = "${summary.averageBattery.toInt()}%",
            color = subjectColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun SubjectDetailPane(
    summary: SubjectSummary,
    onOpenCharger: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subjectColor = Color(summary.subject.colorHex)
    CalmCard(
        modifier = modifier,
        cornerRadius = 26.dp,
        elevation = 5.dp,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SUBJECT / ${summary.subject.shortName}",
                        color = subjectColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 1.6.sp,
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    ZoneDot(zone = Zone.GREEN, size = 6.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${summary.averageBattery.toInt()}% HELD",
                        color = Muted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            letterSpacing = 1.2.sp,
                        ),
                    )
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    text = summary.subject.name,
                    color = Ink,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 36.sp,
                        lineHeight = 38.sp,
                    ),
                )
                Spacer(Modifier.height(15.dp))
                BatteryBar(
                    progress = summary.averageBattery,
                    color = subjectColor,
                    track = StoneLight,
                    height = 7.dp,
                )
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneCountChip(summary.redCount, Zone.RED)
                    ZoneCountChip(summary.amberCount, Zone.AMBER)
                    ZoneCountChip(summary.greenCount, Zone.GREEN)
                    if (summary.lockedCount > 0) {
                        ZoneCountChip(summary.lockedCount, Zone.LOCKED)
                    }
                }
                Spacer(Modifier.height(22.dp))
                SectionLabel(index = "01", title = "CHAPTERS")
                Spacer(Modifier.height(8.dp))
                Hairline(color = Border)
            }
            items(
                items = summary.chapters,
                key = { it.id },
            ) { chapter ->
                ChapterRow(
                    chapter = chapter,
                    onOpenCharger = onOpenCharger,
                )
                Hairline(color = Border)
            }
            item {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "A chapter returns to green when its battery is above 70%.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EmptySyllabus(hasNarrowing: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(StoneLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (hasNarrowing) "Nothing in this zone." else "Your syllabus is quiet.",
            color = Ink,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (hasNarrowing) {
                "Try a different filter or clear your search."
            } else {
                "Add subjects when you are ready."
            },
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
