package com.studyapp.decaycharge.model

enum class ChapterStatus {
    ACTIVE,
    LOCKED,
    ARCHIVED,
}

enum class Zone {
    RED,
    AMBER,
    GREEN,
    LOCKED,
}

enum class ChargeMode {
    DEEP,
    QUICK_TOP_UP,
}

enum class ZoneFilter(val label: String) {
    ALL("All"),
    RED("Red"),
    AMBER("Amber"),
    GREEN("Green"),
    LOCKED("Locked");

    fun accepts(zone: Zone): Boolean = when (this) {
        ALL -> true
        RED -> zone == Zone.RED
        AMBER -> zone == Zone.AMBER
        GREEN -> zone == Zone.GREEN
        LOCKED -> zone == Zone.LOCKED
    }
}

data class Subject(
    val id: Long,
    val name: String,
    val shortName: String,
    val colorHex: Long,
    val examWeight: Float,
)

data class Chapter(
    val id: Long,
    val subjectId: Long,
    val subjectName: String,
    val subjectShortName: String,
    val subjectColorHex: Long,
    val unitName: String,
    val title: String,
    val status: ChapterStatus,
    val battery: Float,
    val stabilityDays: Float,
    val daysSinceCharge: Int,
    val drainRatePerDay: Float,
    val difficultyWeight: Float,
    val targetMinutes: Int,
    val reviewCount: Int,
    val note: String?,
) {
    val zone: Zone
        get() = when {
            status == ChapterStatus.LOCKED -> Zone.LOCKED
            battery < 35f -> Zone.RED
            battery < 70f -> Zone.AMBER
            else -> Zone.GREEN
        }

    val difficultyLabel: String
        get() = when {
            difficultyWeight >= 1.15f -> "Heavy"
            difficultyWeight >= 0.95f -> "Medium"
            else -> "Light"
        }
}

data class SubjectSummary(
    val subject: Subject,
    val chapters: List<Chapter>,
    val averageBattery: Float,
    val redCount: Int,
    val amberCount: Int,
    val greenCount: Int,
    val lockedCount: Int,
)

fun List<Chapter>.weightedReadiness(): Int {
    val active = filter { it.status == ChapterStatus.ACTIVE }
    if (active.isEmpty()) return 0
    val weights = active.map { chapter ->
        val subjectWeight = DemoSyllabus.subjects.firstOrNull { it.id == chapter.subjectId }?.examWeight ?: 1f
        subjectWeight
    }
    val weighted = active.mapIndexed { index, chapter -> chapter.battery * weights[index] }.sum()
    return (weighted / weights.sum()).toInt()
}

fun List<Chapter>.toSubjectSummaries(subjects: List<Subject>): List<SubjectSummary> =
    subjects.map { subject ->
        val subjectChapters = filter { it.subjectId == subject.id }
        val active = subjectChapters.filter { it.status == ChapterStatus.ACTIVE }
        val average = if (active.isEmpty()) 0f else active.map { it.battery }.average().toFloat()
        SubjectSummary(
            subject = subject,
            chapters = subjectChapters,
            averageBattery = average,
            redCount = subjectChapters.count { it.zone == Zone.RED },
            amberCount = subjectChapters.count { it.zone == Zone.AMBER },
            greenCount = subjectChapters.count { it.zone == Zone.GREEN },
            lockedCount = subjectChapters.count { it.zone == Zone.LOCKED },
        )
    }

object DemoSyllabus {
    val subjects = listOf(
        Subject(1, "Physiology", "PHY", 0xFF0F5132, 1.3f),
        Subject(2, "Pathology", "PAT", 0xFFA65D50, 1.2f),
        Subject(3, "Pharmacology", "PHA", 0xFFB4864C, 1.1f),
        Subject(4, "Anatomy", "ANA", 0xFF64776C, 1.0f),
        Subject(5, "Biochemistry", "BIO", 0xFF80627A, 1.0f),
    )

    private fun chapter(
        id: Long,
        subject: Subject,
        unit: String,
        title: String,
        battery: Float,
        stability: Float,
        days: Int,
        drain: Float,
        difficulty: Float,
        target: Int,
        reviewCount: Int,
        note: String?,
        status: ChapterStatus = ChapterStatus.ACTIVE,
    ) = Chapter(
        id = id,
        subjectId = subject.id,
        subjectName = subject.name,
        subjectShortName = subject.shortName,
        subjectColorHex = subject.colorHex,
        unitName = unit,
        title = title,
        status = status,
        battery = battery,
        stabilityDays = stability,
        daysSinceCharge = days,
        drainRatePerDay = drain,
        difficultyWeight = difficulty,
        targetMinutes = target,
        reviewCount = reviewCount,
        note = note,
    )

    private val physiology = subjects[0]
    private val pathology = subjects[1]
    private val pharmacology = subjects[2]
    private val anatomy = subjects[3]
    private val biochemistry = subjects[4]

    val chapters = listOf(
        chapter(
            1, physiology, "Renal System", "Renal Regulation",
            24f, 3.2f, 9, 6.2f, 1.2f, 45, 2,
            "Revise the tubular reabsorption table. Current notes stop at page 68.",
        ),
        chapter(
            2, physiology, "Cardiovascular", "Coronary Circulation",
            34f, 4.0f, 7, 5.1f, 1.0f, 45, 1,
            "Redraw the pressure-volume loop from memory before reading further.",
        ),
        chapter(
            3, physiology, "Cardiovascular", "Cardiac Cycle",
            66f, 6.2f, 4, 3.4f, 1.0f, 30, 2, null,
        ),
        chapter(
            4, pathology, "General Pathology", "Acute Inflammation",
            31f, 4.4f, 6, 4.8f, 1.2f, 45, 1,
            "Compare granuloma types before the next pass.",
        ),
        chapter(
            5, pathology, "General Pathology", "Neoplasia Classification",
            42f, 7.1f, 5, 3.9f, 1.2f, 45, 3,
            "Carcinoma and sarcoma markers still feel shaky.",
        ),
        chapter(
            6, pathology, "General Pathology", "Cell Injury",
            78f, 11.0f, 2, 2.1f, 1.0f, 30, 3, null,
        ),
        chapter(
            7, pharmacology, "Autonomic Drugs", "Beta Blockers",
            57f, 8.4f, 3, 2.9f, 1.0f, 30, 2,
            "Revisit cardioselectivity exceptions and overdose rules.",
        ),
        chapter(
            8, pharmacology, "Anti-Infectives", "Antimicrobial Spectrum",
            82f, 14.0f, 1, 1.4f, 0.8f, 45, 4, null,
        ),
        chapter(
            9, pharmacology, "Core Principles", "Pharmacokinetics",
            90f, 17.0f, 1, 1.0f, 0.8f, 30, 4, null,
        ),
        chapter(
            10, anatomy, "Upper Limb", "Brachial Plexus",
            74f, 9.2f, 2, 2.4f, 1.2f, 30, 2,
            "Trace the cords once more; roots are secure.",
        ),
        chapter(
            11, anatomy, "Head & Neck", "Cranial Nerves",
            63f, 6.8f, 4, 3.2f, 1.0f, 30, 2,
            "Olfactory and facial pathways still need a clean sketch.",
        ),
        chapter(
            12, biochemistry, "Metabolism", "Glycolysis Regulation",
            86f, 12.5f, 1, 1.2f, 1.0f, 30, 3, null,
        ),
        chapter(
            13, pathology, "Systemic Pathology", "Cardiovascular Pathology",
            0f, 3.0f, 0, 0f, 1.0f, 45, 0, null,
            ChapterStatus.LOCKED,
        ),
    )
}
