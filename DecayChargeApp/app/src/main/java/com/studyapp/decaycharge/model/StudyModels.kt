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
    DEEP_CHARGE,
    QUICK_TOP_UP,
    STOPWATCH,
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

fun List<Chapter>.weightedReadiness(subjects: List<Subject>): Int {
    val weightsById = subjects.associate { it.id to it.examWeight.coerceAtLeast(0f) }
    val active = filter { it.status == ChapterStatus.ACTIVE }
    if (active.isEmpty()) return 0
    val weights = active.map { weightsById[it.subjectId] ?: 1f }
    val totalWeight = weights.sum()
    if (totalWeight <= 0f) return 0
    val weighted = active.mapIndexed { index, chapter -> chapter.battery * weights[index] }.sum()
    return (weighted / totalWeight).toInt().coerceIn(0, 100)
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
