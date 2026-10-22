package com.studyapp.decaycharge.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyapp.decaycharge.domain.DecayCalculator
import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChargeMode
import com.studyapp.decaycharge.model.Zone
import com.studyapp.decaycharge.navigation.LocalWindowSizeClass
import com.studyapp.decaycharge.ui.ChargerUiState
import com.studyapp.decaycharge.ui.ChargerViewModel
import com.studyapp.decaycharge.ui.components.BatteryCell
import com.studyapp.decaycharge.ui.components.BotanicalBranch
import com.studyapp.decaycharge.ui.components.CalmButton
import com.studyapp.decaycharge.ui.components.CalmButtonVariant
import com.studyapp.decaycharge.ui.components.CalmCard
import com.studyapp.decaycharge.ui.components.CalmIconButton
import com.studyapp.decaycharge.ui.components.Eyebrow
import com.studyapp.decaycharge.ui.components.Hairline
import com.studyapp.decaycharge.ui.components.SectionLabel
import com.studyapp.decaycharge.ui.components.SubjectTag
import com.studyapp.decaycharge.ui.components.ZoneDot
import com.studyapp.decaycharge.ui.components.ZonePill
import com.studyapp.decaycharge.ui.components.zoneColor
import com.studyapp.decaycharge.ui.components.zoneLabel
import com.studyapp.decaycharge.ui.components.zoneSoftColor
import com.studyapp.decaycharge.ui.theme.Border
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.Ink
import com.studyapp.decaycharge.ui.theme.Ivory
import com.studyapp.decaycharge.ui.theme.Mint
import com.studyapp.decaycharge.ui.theme.MintLight
import com.studyapp.decaycharge.ui.theme.Muted
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.Parchment
import com.studyapp.decaycharge.ui.theme.Stone
import com.studyapp.decaycharge.ui.theme.StoneLight
import com.studyapp.decaycharge.ui.theme.White
import com.studyapp.decaycharge.ui.theme.ZoneRed
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargerScreen(
    viewModel: ChargerViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = LocalWindowSizeClass.current
    val split = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact ||
        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val handleBack = {
        if (viewModel.requestBack()) onBack()
    }

    BackHandler { handleBack() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ivory),
    ) {
        BotanicalBranch(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 64.dp, y = (-30).dp)
                .size(160.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            ChargerTopBar(
                chapter = state.chapter,
                onBack = handleBack,
            )
            val chapter = state.chapter
            if (chapter == null) {
                MissingChapter()
            } else if (split) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ChargeStage(
                            state = state,
                            chapter = chapter,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ChapterIntro(chapter)
                        Spacer(Modifier.height(15.dp))
                        if (state.isComplete) {
                            RatingPanel(
                                state = state,
                                chapter = chapter,
                                onSelectRating = viewModel::selectRating,
                                onSave = {
                                    viewModel.saveCompletedCharge(onBack)
                                },
                            )
                        } else {
                            SetupPanel(
                                state = state,
                                chapter = chapter,
                                onMode = viewModel::setMode,
                                onNote = viewModel::setNote,
                                onToggleTimer = viewModel::toggleTimer,
                                onEndEarly = viewModel::endEarly,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 28.dp),
                ) {
                    ChapterIntro(chapter)
                    Spacer(Modifier.height(16.dp))
                    ChargeStage(
                        state = state,
                        chapter = chapter,
                    )
                    Spacer(Modifier.height(15.dp))
                    if (state.isComplete) {
                        RatingPanel(
                            state = state,
                            chapter = chapter,
                            onSelectRating = viewModel::selectRating,
                            onSave = {
                                viewModel.saveCompletedCharge(onBack)
                            },
                        )
                    } else {
                        SetupPanel(
                            state = state,
                            chapter = chapter,
                            onMode = viewModel::setMode,
                            onNote = viewModel::setNote,
                            onToggleTimer = viewModel::toggleTimer,
                            onEndEarly = viewModel::endEarly,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
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

        if (state.showBackSheet) {
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissBackSheet,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Paper,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 11.dp, bottom = 3.dp)
                            .size(width = 34.dp, height = 3.dp)
                            .background(Stone, CircleShape),
                    )
                },
            ) {
                LeaveSessionSheet(
                    state = state,
                    onKeepCharging = {
                        viewModel.keepChargingInBackground()
                        onBack()
                    },
                    onEndEarly = viewModel::endEarly,
                    onDiscard = viewModel::showDiscardConfirmation,
                )
            }
        }

        if (state.showDiscardDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDiscardConfirmation,
                containerColor = Paper,
                shape = RoundedCornerShape(26.dp),
                title = {
                    Text(
                        text = "Discard this charge?",
                        color = Ink,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = {
                    Text(
                        text = "The timer and any progress in this session will be released.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.discardSession()
                            onBack()
                        },
                    ) {
                        Text(
                            text = "Discard",
                            color = ZoneRed,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDiscardConfirmation) {
                        Text(
                            text = "Keep charging",
                            color = Forest,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ChargerTopBar(
    chapter: Chapter?,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalmIconButton(
            icon = Icons.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            filled = false,
            tint = Ink,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Eyebrow("LIVE CHARGER")
            Text(
                text = chapter?.title ?: "Session",
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            )
        }
        if (chapter != null) {
            SubjectTag(
                shortName = chapter.subjectShortName,
                color = Color(chapter.subjectColorHex),
            )
        }
    }
}

@Composable
private fun ChapterIntro(chapter: Chapter) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(chapter.unitName.uppercase(), color = Color(chapter.subjectColorHex))
            Spacer(Modifier.weight(1f))
            ZonePill(zone = chapter.zone)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = chapter.title,
            color = Ink,
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 38.sp,
                lineHeight = 40.sp,
            ),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "${chapter.subjectName} · target ${chapter.targetMinutes}m · ${chapter.difficultyLabel.lowercase()} yield",
            color = Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        )
    }
}

@Composable
private fun ChargeStage(
    state: ChargerUiState,
    chapter: Chapter,
) {
    val progress = state.displayBattery
    val displayZone = when {
        progress < 35f -> Zone.RED
        progress < 70f -> Zone.AMBER
        else -> Zone.GREEN
    }
    val remaining = when {
        state.isForThisChapter -> state.active?.remainingSeconds ?: 0
        state.mode == ChargeMode.QUICK_TOP_UP -> 15 * 60
        else -> chapter.targetMinutes * 60
    }
    val stageLabel = when {
        state.isComplete -> "CHARGE HELD"
        state.isRunning -> "CHARGING"
        state.active?.isRunning == false && state.isForThisChapter -> "PAUSED"
        else -> "READY TO CHARGE"
    }

    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 30.dp,
        elevation = 9.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 17.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(index = "01", title = "BATTERY")
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(zoneColor(displayZone), CircleShape),
                    )
                    Text(
                        text = stageLabel,
                        color = zoneColor(displayZone),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 7.sp,
                            letterSpacing = 1.2.sp,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${progress.toInt()}",
                    color = Ink,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 52.sp,
                        lineHeight = 53.sp,
                        fontFeatureSettings = "lnum",
                    ),
                )
                Text(
                    text = "%",
                    color = zoneColor(displayZone),
                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Text(
                text = "PROJECTED AFTER THIS CHARGE",
                color = Muted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    letterSpacing = 1.1.sp,
                ),
            )
            Spacer(Modifier.height(5.dp))
            BatteryCell(
                progress = progress,
                title = chapter.title,
                zone = displayZone,
                modifier = Modifier
                    .width(104.dp)
                    .height(174.dp),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = formatClock(remaining),
                color = Ink,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 36.sp,
                    lineHeight = 38.sp,
                    fontFeatureSettings = "lnum, tnum",
                ),
            )
            Text(
                text = if (state.isComplete) "CHARGE COMPLETE" else "REMAINING",
                color = Muted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    letterSpacing = 1.5.sp,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Hairline(color = Border)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StageMetric(label = "MODE", value = if (state.mode == ChargeMode.DEEP_CHARGE) "Deep" else "Top-Up")
                StageMetric(label = "STABILITY", value = "${"%.1f".format(chapter.stabilityDays)}d")
                StageMetric(label = "TARGET", value = "${state.mode.targetMinutes(chapter)}m")
            }
        }
    }
}

@Composable
private fun StageMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Ink,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 18.sp,
                lineHeight = 20.sp,
            ),
        )
        Text(
            text = label,
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 6.sp,
                letterSpacing = 1.2.sp,
            ),
        )
    }
}

@Composable
private fun SetupPanel(
    state: ChargerUiState,
    chapter: Chapter,
    onMode: (ChargeMode) -> Unit,
    onNote: (String) -> Unit,
    onToggleTimer: () -> Unit,
    onEndEarly: () -> Unit,
) {
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        elevation = 5.dp,
    ) {
        Column(modifier = Modifier.padding(19.dp)) {
            SectionLabel(index = "02", title = "SESSION SETUP")
            Spacer(Modifier.height(15.dp))
            ModeSelector(
                mode = state.mode,
                enabled = !state.isForThisChapter,
                quickEnabled = chapter.zone == Zone.AMBER,
                onMode = onMode,
            )
            Spacer(Modifier.height(17.dp))
            NoteField(
                value = state.note,
                onValueChange = onNote,
            )
            Spacer(Modifier.height(17.dp))
            val running = state.isRunning
            CalmButton(
                label = when {
                    state.isComplete -> "Charge complete"
                    state.isForThisChapter && running -> "Pause charge"
                    state.isForThisChapter -> "Resume charge"
                    else -> "Begin ${if (state.mode == ChargeMode.DEEP_CHARGE) "deep" else "top-up"} charge"
                },
                onClick = onToggleTimer,
                leadingIcon = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                trailingIcon = if (running) null else Icons.Rounded.ArrowForward,
                enabled = !state.isComplete,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isForThisChapter) {
                val elapsed = state.active?.elapsedSeconds ?: 0
                Spacer(Modifier.height(9.dp))
                CalmButton(
                    label = "Finish early",
                    onClick = onEndEarly,
                    variant = CalmButtonVariant.Secondary,
                    enabled = elapsed >= 5 * 60 && !state.isComplete,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (elapsed < 5 * 60) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "Early finish unlocks after five minutes of charge.",
                        color = Muted,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Leaving a running charge keeps it alive in a mini charger while the app stays open.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 14.sp,
                ),
            )
        }
    }
}

@Composable
private fun ModeSelector(
    mode: ChargeMode,
    enabled: Boolean,
    quickEnabled: Boolean,
    onMode: (ChargeMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Parchment)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ModeOption(
            label = "Deep charge",
            caption = "Full target",
            selected = mode == ChargeMode.DEEP_CHARGE,
            enabled = enabled,
            onClick = { onMode(ChargeMode.DEEP_CHARGE) },
            modifier = Modifier.weight(1f),
        )
        ModeOption(
            label = "Quick top-up",
            caption = if (quickEnabled) "15 minutes" else "Amber only",
            selected = mode == ChargeMode.QUICK_TOP_UP,
            enabled = enabled && quickEnabled,
            onClick = { onMode(ChargeMode.QUICK_TOP_UP) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeOption(
    label: String,
    caption: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(15.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Forest else Color.Transparent, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = when {
                selected -> Ivory
                enabled -> Ink
                else -> Stone
            },
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
        Text(
            text = caption.uppercase(),
            color = when {
                selected -> Mint
                enabled -> Muted
                else -> Stone
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 6.sp,
                letterSpacing = 1.0.sp,
            ),
        )
    }
}

@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow("BREADCRUMB NOTE")
            Spacer(Modifier.weight(1f))
            Text(
                text = "OPTIONAL",
                color = Muted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 6.sp,
                    letterSpacing = 1.1.sp,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 78.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Parchment.copy(alpha = 0.62f))
                .padding(12.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "Leave a bookmark for your next self…",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = Ink,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                ),
                cursorBrush = SolidColor(Forest),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            )
        }
    }
}

@Composable
private fun RatingPanel(
    state: ChargerUiState,
    chapter: Chapter,
    onSelectRating: (Int) -> Unit,
    onSave: () -> Unit,
) {
    val progress = state.active?.progress ?: 1f
    val batteryBefore = state.active?.startBattery ?: chapter.battery
    fun projectedStability(rating: Int): Float = DecayCalculator.stabilityAfterReview(
        oldStabilityDays = chapter.stabilityDays,
        reviewCount = chapter.reviewCount,
        recallRating = rating,
        batteryBefore = batteryBefore,
        chargeRatio = progress,
    )
    val ratings = listOf(
        Triple(1, "Hard", projectedStability(1)),
        Triple(2, "Medium", projectedStability(2)),
        Triple(3, "Easy", projectedStability(3)),
    )
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        elevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(index = "02", title = "RECALL CHECK")
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MintLight)
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Forest,
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "CHARGE HELD",
                            color = Forest,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.sp,
                                letterSpacing = 1.0.sp,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(15.dp))
            Text(
                text = "How well did it hold?",
                color = Ink,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    lineHeight = 29.sp,
                ),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${chapter.battery.toInt()}% ${zoneLabel(chapter.zone).lowercase()} → ${state.displayBattery.toInt()}% held",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            )
            Spacer(Modifier.height(15.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ratings.forEach { (rating, label, stability) ->
                    val selected = state.selectedRating == rating
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (selected) MintLight else Parchment, shape)
                            .clickable { onSelectRating(rating) }
                            .padding(horizontal = 7.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Forest else Ink,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${"%.1f".format(stability)}d",
                            color = if (selected) Forest else Muted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 0.3.sp,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(15.dp))
            CalmButton(
                label = "Save charge",
                onClick = onSave,
                trailingIcon = Icons.Rounded.ArrowForward,
                enabled = state.selectedRating != null,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Your recall rating decides how slowly this topic fades next.",
                color = Muted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            )
        }
    }
}

@Composable
private fun LeaveSessionSheet(
    state: ChargerUiState,
    onKeepCharging: () -> Unit,
    onEndEarly: () -> Unit,
    onDiscard: () -> Unit,
) {
    val elapsed = state.active?.elapsedSeconds ?: 0
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp),
    ) {
        Eyebrow("SESSION IN PROGRESS")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Leave the charger?",
            color = Ink,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = "Your charge is safe. Choose how it should continue.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(18.dp))
        SheetAction(
            icon = Icons.Rounded.PlayArrow,
            title = "Keep charging in background",
            subtitle = "Returns to Triage with a mini charger close by.",
            onClick = onKeepCharging,
        )
        Spacer(Modifier.height(9.dp))
        SheetAction(
            icon = Icons.Rounded.StopCircle,
            title = "End early & rate recall",
            subtitle = if (elapsed >= 5 * 60) {
                "Log ${formatClock(elapsed)} of charge and review the fade."
            } else {
                "Available after five minutes of charge."
            },
            enabled = elapsed >= 5 * 60,
            onClick = onEndEarly,
        )
        Spacer(Modifier.height(9.dp))
        SheetAction(
            icon = Icons.Rounded.DeleteOutline,
            title = "Discard session",
            subtitle = "Release this charge and reset the timer.",
            destructive = true,
            onClick = onDiscard,
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val tint = when {
        !enabled -> Stone
        destructive -> ZoneRed
        else -> Forest
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (destructive) Color(0xFFFBF0ED) else Parchment.copy(alpha = 0.65f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    if (enabled) tint.copy(alpha = 0.10f) else StoneLight,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Ink else Muted,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
            )
            Text(
                text = subtitle,
                color = if (enabled) Muted else Stone,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            )
        }
    }
}

@Composable
private fun MissingChapter() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "This chapter could not be found.",
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun ChargeMode.targetMinutes(chapter: Chapter): Int =
    if (this == ChargeMode.QUICK_TOP_UP) 15 else chapter.targetMinutes


private fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%02d:%02d".format(minutes, seconds)
}
