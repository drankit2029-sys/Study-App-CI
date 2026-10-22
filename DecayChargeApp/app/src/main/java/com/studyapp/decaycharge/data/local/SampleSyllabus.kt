package com.studyapp.decaycharge.data.local

import com.studyapp.decaycharge.model.ChapterStatus

object SampleSyllabus {
    private const val DAY_MS = 86_400_000L

    fun subjects(nowEpochMs: Long): List<SubjectEntity> = listOf(
        SubjectEntity(
            id = 1,
            name = "Physiology",
            colorHex = "#0F5132",
            examWeight = 1.3f,
            sortOrder = 0,
            createdAtEpochMs = nowEpochMs - 30 * DAY_MS,
        ),
        SubjectEntity(
            id = 2,
            name = "Pathology",
            colorHex = "#A65D50",
            examWeight = 1.2f,
            sortOrder = 1,
            createdAtEpochMs = nowEpochMs - 30 * DAY_MS,
        ),
        SubjectEntity(
            id = 3,
            name = "Pharmacology",
            colorHex = "#B4864C",
            examWeight = 1.1f,
            sortOrder = 2,
            createdAtEpochMs = nowEpochMs - 30 * DAY_MS,
        ),
        SubjectEntity(
            id = 4,
            name = "Anatomy",
            colorHex = "#64776C",
            examWeight = 1f,
            sortOrder = 3,
            createdAtEpochMs = nowEpochMs - 30 * DAY_MS,
        ),
        SubjectEntity(
            id = 5,
            name = "Biochemistry",
            colorHex = "#80627A",
            examWeight = 1f,
            sortOrder = 4,
            createdAtEpochMs = nowEpochMs - 30 * DAY_MS,
        ),
    )

    fun chapters(nowEpochMs: Long): List<ChapterEntity> = listOf(
        chapter(
            id = 1, subjectId = 1, unit = "Renal System", title = "Renal Regulation",
            daysAgo = 9, stability = 3.2f, difficulty = 1.2f, target = 45, reviews = 2,
            note = "Revise the tubular reabsorption table. Current notes stop at page 68.",
            now = nowEpochMs,
        ),
        chapter(
            id = 2, subjectId = 1, unit = "Cardiovascular", title = "Coronary Circulation",
            daysAgo = 7, stability = 4f, difficulty = 1f, target = 45, reviews = 1,
            note = "Redraw the pressure-volume loop from memory before reading further.",
            now = nowEpochMs,
        ),
        chapter(
            id = 3, subjectId = 1, unit = "Cardiovascular", title = "Cardiac Cycle",
            daysAgo = 4, stability = 6.2f, difficulty = 1f, target = 30, reviews = 2,
            note = null, now = nowEpochMs,
        ),
        chapter(
            id = 4, subjectId = 2, unit = "General Pathology", title = "Acute Inflammation",
            daysAgo = 6, stability = 4.4f, difficulty = 1.2f, target = 45, reviews = 1,
            note = "Compare granuloma types before the next pass.", now = nowEpochMs,
        ),
        chapter(
            id = 5, subjectId = 2, unit = "General Pathology", title = "Neoplasia Classification",
            daysAgo = 5, stability = 7.1f, difficulty = 1.2f, target = 45, reviews = 3,
            note = "Carcinoma and sarcoma markers still feel shaky.", now = nowEpochMs,
        ),
        chapter(
            id = 6, subjectId = 2, unit = "General Pathology", title = "Cell Injury",
            daysAgo = 2, stability = 11f, difficulty = 1f, target = 30, reviews = 3,
            note = null, now = nowEpochMs,
        ),
        chapter(
            id = 7, subjectId = 3, unit = "Autonomic Drugs", title = "Beta Blockers",
            daysAgo = 3, stability = 8.4f, difficulty = 1f, target = 30, reviews = 2,
            note = "Revisit cardioselectivity exceptions and overdose rules.", now = nowEpochMs,
        ),
        chapter(
            id = 8, subjectId = 3, unit = "Anti-Infectives", title = "Antimicrobial Spectrum",
            daysAgo = 1, stability = 14f, difficulty = 0.8f, target = 45, reviews = 4,
            note = null, now = nowEpochMs,
        ),
        chapter(
            id = 9, subjectId = 3, unit = "Core Principles", title = "Pharmacokinetics",
            daysAgo = 1, stability = 17f, difficulty = 0.8f, target = 30, reviews = 4,
            note = null, now = nowEpochMs,
        ),
        chapter(
            id = 10, subjectId = 4, unit = "Upper Limb", title = "Brachial Plexus",
            daysAgo = 2, stability = 9.2f, difficulty = 1.2f, target = 30, reviews = 2,
            note = "Trace the cords once more; roots are secure.", now = nowEpochMs,
        ),
        chapter(
            id = 11, subjectId = 4, unit = "Head & Neck", title = "Cranial Nerves",
            daysAgo = 4, stability = 6.8f, difficulty = 1f, target = 30, reviews = 2,
            note = "Olfactory and facial pathways still need a clean sketch.", now = nowEpochMs,
        ),
        chapter(
            id = 12, subjectId = 5, unit = "Metabolism", title = "Glycolysis Regulation",
            daysAgo = 1, stability = 12.5f, difficulty = 1f, target = 30, reviews = 3,
            note = null, now = nowEpochMs,
        ),
        ChapterEntity(
            id = 13,
            subjectId = 2,
            unitName = "Systemic Pathology",
            title = "Cardiovascular Pathology",
            status = ChapterStatus.LOCKED,
            difficultyWeight = 1f,
            targetMinutes = 45,
            lastChargedAtEpochMs = null,
            lastBatteryLevel = 0f,
            stabilityDays = 3f,
            reviewCount = 0,
            totalMinutesStudied = 0,
            breadcrumbNote = null,
        ),
    )

    private fun chapter(
        id: Long,
        subjectId: Long,
        unit: String,
        title: String,
        daysAgo: Int,
        stability: Float,
        difficulty: Float,
        target: Int,
        reviews: Int,
        note: String?,
        now: Long,
    ) = ChapterEntity(
        id = id,
        subjectId = subjectId,
        unitName = unit,
        title = title,
        status = ChapterStatus.ACTIVE,
        difficultyWeight = difficulty,
        targetMinutes = target,
        lastChargedAtEpochMs = now - daysAgo * DAY_MS,
        lastBatteryLevel = 100f,
        stabilityDays = stability,
        reviewCount = reviews,
        totalMinutesStudied = reviews * target,
        breadcrumbNote = note,
    )
}
