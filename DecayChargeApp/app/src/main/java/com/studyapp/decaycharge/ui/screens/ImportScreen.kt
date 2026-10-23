package com.studyapp.decaycharge.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyapp.decaycharge.data.importer.ChapterDifficulty
import com.studyapp.decaycharge.data.importer.DraftChapter
import com.studyapp.decaycharge.data.importer.DraftSubject
import com.studyapp.decaycharge.data.importer.SyllabusPack
import com.studyapp.decaycharge.ui.ImportSource
import com.studyapp.decaycharge.ui.ImportUiState
import com.studyapp.decaycharge.ui.ImportViewModel
import com.studyapp.decaycharge.ui.components.CalmButton
import com.studyapp.decaycharge.ui.components.CalmButtonVariant
import com.studyapp.decaycharge.ui.components.CalmCard
import com.studyapp.decaycharge.ui.components.CalmIconButton
import com.studyapp.decaycharge.ui.components.Eyebrow
import com.studyapp.decaycharge.ui.components.FilterPill
import com.studyapp.decaycharge.ui.components.Hairline
import com.studyapp.decaycharge.ui.components.SectionLabel
import com.studyapp.decaycharge.ui.theme.Border
import com.studyapp.decaycharge.ui.theme.Error
import com.studyapp.decaycharge.ui.theme.Forest
import com.studyapp.decaycharge.ui.theme.Ink
import com.studyapp.decaycharge.ui.theme.Ivory
import com.studyapp.decaycharge.ui.theme.MintLight
import com.studyapp.decaycharge.ui.theme.Muted
import com.studyapp.decaycharge.ui.theme.MutedLight
import com.studyapp.decaycharge.ui.theme.Paper
import com.studyapp.decaycharge.ui.theme.Parchment
import com.studyapp.decaycharge.ui.theme.StoneLight

@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    canClose: Boolean,
    onClose: () -> Unit,
    onImported: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull().orEmpty()
            viewModel.loadCsvText(text, displayName(uri))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.imported.collect { count -> onImported(count) }
    }

    BackHandler(enabled = state.isPreviewing || canClose) {
        if (state.isPreviewing) {
            viewModel.backFromPreview()
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ivory),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (state.isPreviewing && state.draft != null) {
            DraftPreview(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .widthIn(max = 980.dp)
                    .fillMaxHeight(),
            )
        } else {
            ImportChooser(
                state = state,
                viewModel = viewModel,
                canClose = canClose,
                onClose = onClose,
                onChooseCsv = {
                    csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
                },
                modifier = Modifier
                    .widthIn(max = 980.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ImportChooser(
    state: ImportUiState,
    viewModel: ImportViewModel,
    canClose: Boolean,
    onClose: () -> Unit,
    onChooseCsv: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow("SYLLABUS / IMPORT")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Add your syllabus",
                    color = Ink,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 27.sp,
                        lineHeight = 30.sp,
                    ),
                )
            }
            if (canClose) {
                CalmIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close import",
                    onClick = onClose,
                    tint = Muted,
                )
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ImportSource.entries.forEach { source ->
                FilterPill(
                    label = source.label,
                    selected = state.source == source,
                    onClick = { viewModel.setSource(source) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        state.error?.let { ErrorNote(it) }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            when (state.source) {
                ImportSource.LIBRARY -> libraryContent(state, viewModel)
                ImportSource.PASTE -> pasteContent(state, viewModel)
                ImportSource.CSV -> csvContent(state, onChooseCsv)
                ImportSource.CUSTOM -> customContent(state, viewModel)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.libraryContent(
    state: ImportUiState,
    viewModel: ImportViewModel,
) {
    item {
        SectionLabel(index = "01", title = "CURATED OFFLINE PACKS")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Choose a ready-made exam syllabus. You can review every chapter before importing.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
        )
    }
    if (state.isLoadingPacks) {
        item {
            Text(
                text = "Loading packs…",
                color = Muted,
                modifier = Modifier.padding(vertical = 24.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    items(state.packs, key = { it.id }) { pack ->
        PackCard(pack = pack, onClick = { viewModel.openPack(pack) })
    }
    item {
        Text(
            text = "Packs stay on this device and import only once.",
            color = Muted,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.pasteContent(
    state: ImportUiState,
    viewModel: ImportViewModel,
) {
    item {
        SectionLabel(index = "01", title = "PASTE FROM A PDF OR NOTES")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Use Subject: Chapter lines, or an indented hierarchy with unit headings.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
        )
    }
    item {
        CalmCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevation = 3.dp) {
            Column(modifier = Modifier.padding(15.dp)) {
                BasicTextField(
                    value = state.pasteText,
                    onValueChange = viewModel::setPasteText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 300.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = Ink,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                    ),
                    cursorBrush = SolidColor(Forest),
                    decorationBox = { inner ->
                        Box {
                            if (state.pasteText.isBlank()) {
                                Text(
                                    text = "Physiology\n  Cardiovascular\n  - Coronary Circulation (Difficulty: Heavy, Target: 45m)\n\nPathology: Acute Inflammation",
                                    color = MutedLight,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 19.sp,
                                    ),
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Hairline(color = Border)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Difficulty may be Light, Medium, or Heavy. Target may be 30m, 45m, 60m, or 1h.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 14.sp),
                )
            }
        }
    }
    item {
        CalmButton(
            label = "Preview pasted syllabus",
            onClick = viewModel::previewPaste,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.csvContent(
    state: ImportUiState,
    onChooseCsv: () -> Unit,
) {
    item {
        SectionLabel(index = "01", title = "IMPORT A CSV")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Columns: Subject, Unit, Chapter, Difficulty, TargetMinutes. Header row is optional.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
        )
    }
    item {
        CsvImportCard(state = state, onChooseCsv = onChooseCsv)
    }
    item {
        Text(
            text = "Tip: export a CSV from your current syllabus, edit it in any spreadsheet, then import it back.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
        )
    }
}

@Composable
private fun CsvImportCard(
    state: ImportUiState,
    onChooseCsv: () -> Unit,
) {
    CalmCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevation = 3.dp) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MintLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = Forest,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.height(11.dp))
            Text(
                text = state.csvFileName.ifBlank { "No file selected" },
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose a .csv file from this device. Nothing is uploaded.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            )
            Spacer(Modifier.height(14.dp))
            CalmButton(
                label = "Choose CSV file",
                onClick = onChooseCsv,
                leadingIcon = Icons.Rounded.UploadFile,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.customContent(
    state: ImportUiState,
    viewModel: ImportViewModel,
) {
    item {
        SectionLabel(index = "01", title = "BUILD FROM SCRATCH")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Add one chapter at a time. Each new subject gets its own colour and weight.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
        )
    }
    item {
        CalmCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevation = 3.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ImportField(
                    label = "SUBJECT",
                    value = state.customSubject,
                    placeholder = "Physiology",
                    onValueChange = viewModel::setCustomSubject,
                )
                ImportField(
                    label = "UNIT (OPTIONAL)",
                    value = state.customUnit,
                    placeholder = "Cardiovascular",
                    onValueChange = viewModel::setCustomUnit,
                )
                ImportField(
                    label = "CHAPTER",
                    value = state.customChapter,
                    placeholder = "Coronary Circulation",
                    onValueChange = viewModel::setCustomChapter,
                )
                Text(
                    text = "DIFFICULTY",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, letterSpacing = 1.4.sp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ChapterDifficulty.entries.forEach { difficulty ->
                        FilterPill(
                            label = difficulty.label,
                            selected = state.customDifficulty == difficulty,
                            onClick = { viewModel.setCustomDifficulty(difficulty) },
                        )
                    }
                }
                Text(
                    text = "TARGET",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, letterSpacing = 1.4.sp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(30, 45, 60).forEach { minutes ->
                        FilterPill(
                            label = "${minutes}m",
                            selected = state.customTargetMinutes == minutes,
                            onClick = { viewModel.setCustomTargetMinutes(minutes) },
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                CalmButton(
                    label = "Add chapter",
                    onClick = viewModel::addCustomChapter,
                    leadingIcon = Icons.Rounded.Add,
                    variant = CalmButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    val customDraft = state.draft?.takeIf { it.title == "Custom syllabus" }
    if (customDraft != null && customDraft.subjects.isNotEmpty()) {
        item {
            CalmCard(
                modifier = Modifier.fillMaxWidth(),
                color = Parchment,
                cornerRadius = 20.dp,
                elevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${customDraft.chapterCount} chapters across ${customDraft.subjects.size} subjects",
                        color = Ink,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                    )
                    Spacer(Modifier.height(4.dp))
                    customDraft.subjects.forEach { subject ->
                        Text(
                            text = "${subject.name} · ${subject.chapters.size}",
                            color = Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    CalmButton(
                        label = "Review custom syllabus",
                        onClick = viewModel::showPreview,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PackCard(pack: SyllabusPack, onClick: () -> Unit) {
    CalmCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 22.dp,
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.category.uppercase(),
                    color = Forest,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        letterSpacing = 1.5.sp,
                    ),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = pack.title,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 23.sp,
                        lineHeight = 25.sp,
                    ),
                )
                Text(
                    text = pack.subtitle,
                    color = Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "${pack.subjectCount} subjects · ${pack.chapterCount} chapters",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        letterSpacing = 1.0.sp,
                    ),
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MintLight, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Forest,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ImportField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column {
        Text(
            text = label,
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, letterSpacing = 1.4.sp),
        )
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Paper)
                .padding(horizontal = 13.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink, fontSize = 13.sp),
            cursorBrush = SolidColor(Forest),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = MutedLight,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun DraftPreview(
    state: ImportUiState,
    viewModel: ImportViewModel,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CalmIconButton(
                icon = Icons.Rounded.KeyboardArrowLeft,
                contentDescription = "Back to import options",
                onClick = viewModel::backFromPreview,
                tint = Muted,
                filled = false,
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow("REVIEW / ${draft.title.uppercase()}")
                Text(
                    text = "Review your syllabus",
                    color = Ink,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 27.sp,
                        lineHeight = 30.sp,
                    ),
                )
            }
            Text(
                text = "${draft.chapterCount}/${draft.totalChapterCount}",
                color = Forest,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            )
        }
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${draft.subjectCount} subjects selected",
                color = Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (draft.chapterCount == draft.totalChapterCount) "DESELECT ALL" else "SELECT ALL",
                color = Forest,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        viewModel.setAllSelected(draft.chapterCount != draft.totalChapterCount)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    letterSpacing = 1.2.sp,
                ),
            )
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = "APPLY TO SELECTED",
            color = Muted,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, letterSpacing = 1.4.sp),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ChapterDifficulty.entries.forEach { difficulty ->
                FilterPill(
                    label = difficulty.label,
                    selected = false,
                    onClick = { viewModel.applyDifficulty(difficulty) },
                )
            }
            listOf(30, 45, 60).forEach { minutes ->
                FilterPill(
                    label = "${minutes}m",
                    selected = false,
                    onClick = { viewModel.applyTarget(minutes) },
                )
            }
        }
        Spacer(Modifier.height(11.dp))
        state.error?.let {
            ErrorNote(it)
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            itemsIndexed(draft.subjects, key = { index, subject -> "$index-${subject.name}" }) { subjectIndex, subject ->
                DraftSubjectCard(
                    subject = subject,
                    onToggleChapter = { chapterIndex ->
                        viewModel.toggleChapter(subjectIndex, chapterIndex)
                    },
                )
            }
        }
        Hairline(color = Border)
        Spacer(Modifier.height(12.dp))
        CalmButton(
            label = if (state.isImporting) {
                "Importing…"
            } else {
                "Import ${draft.chapterCount} chapters"
            },
            onClick = viewModel::importSelected,
            enabled = draft.chapterCount > 0 && !state.isImporting,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun DraftSubjectCard(
    subject: DraftSubject,
    onToggleChapter: (Int) -> Unit,
) {
    val subjectColor = remember(subject.colorHex) { parseColor(subject.colorHex) }
    CalmCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(subjectColor, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = subject.name,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                )
                Text(
                    text = "${subject.chapters.count { it.selected }}/${subject.chapters.size}",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                )
            }
            Spacer(Modifier.height(7.dp))
            subject.chapters.forEachIndexed { index, chapter ->
                DraftChapterRow(
                    chapter = chapter,
                    onClick = { onToggleChapter(index) },
                )
            }
        }
    }
}

@Composable
private fun DraftChapterRow(chapter: DraftChapter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (chapter.selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (chapter.selected) "Deselect ${chapter.title}" else "Select ${chapter.title}",
            tint = if (chapter.selected) Forest else MutedLight,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.title,
                color = if (chapter.selected) Ink else Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            val unit = chapter.unitName?.takeIf { it.isNotBlank() } ?: "General"
            Text(
                text = "$unit · ${chapter.difficulty.label} · ${chapter.targetMinutes}m",
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            )
        }
    }
}

@Composable
private fun ErrorNote(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Error.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = Error,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
        )
    }
}

private fun displayName(uri: Uri): String {
    return uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.substringAfterLast(':')
        ?.takeIf { it.isNotBlank() }
        ?: "syllabus.csv"
}

private fun parseColor(raw: String): Color {
    val value = raw.removePrefix("#").toLongOrNull(16) ?: 0xFF0F5132
    return Color(if (raw.removePrefix("#").length <= 6) value or 0xFF000000 else value)
}
