package com.studyapp.decaycharge.domain

import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.Zone
import kotlin.math.roundToInt

enum class PlannedChargeKind {
    RED_CHARGE,
    AMBER_TOP_UP,
    UNLOCK,
}

data class PlannedCharge(
    val chapter: Chapter,
    val minutes: Int,
    val kind: PlannedChargeKind,
)

object DailyPlanner {
    fun build(chapters: List<Chapter>, budgetMinutes: Int): List<PlannedCharge> {
        val budget = budgetMinutes.coerceIn(30, 480)
        val red = chapters
            .filter { it.status == ChapterStatus.ACTIVE && it.zone == Zone.RED }
            .sortedByDescending { it.urgencyScore }
        val amber = chapters
            .filter { it.status == ChapterStatus.ACTIVE && it.zone == Zone.AMBER }
            .sortedByDescending { it.urgencyScore }
        val locked = chapters
            .filter { it.status == ChapterStatus.LOCKED }
            .sortedByDescending { it.subjectExamWeight }

        val plan = mutableListOf<PlannedCharge>()
        var remaining = budget
        var redBudget = (budget * 0.70f).roundToInt()
        red.forEach { chapter ->
            if (remaining <= 0 || redBudget <= 0) return@forEach
            val minutes = chapter.targetMinutes.coerceIn(20, 90).coerceAtMost(min(redBudget, remaining))
            if (minutes >= 20) {
                plan += PlannedCharge(chapter, minutes, PlannedChargeKind.RED_CHARGE)
                redBudget -= minutes
                remaining -= minutes
            }
        }

        var amberSlots = 2
        amber.forEach { chapter ->
            if (remaining >= 15 && amberSlots > 0 && remaining >= 15) {
                plan += PlannedCharge(chapter, 15, PlannedChargeKind.AMBER_TOP_UP)
                remaining -= 15
                amberSlots -= 1
            }
        }

        if (locked.isNotEmpty() && remaining >= 30 && red.size <= 3) {
            plan += PlannedCharge(locked.first(), 30, PlannedChargeKind.UNLOCK)
            remaining -= 30
        }
        return plan
    }

    private fun min(a: Int, b: Int): Int = if (a < b) a else b
}
