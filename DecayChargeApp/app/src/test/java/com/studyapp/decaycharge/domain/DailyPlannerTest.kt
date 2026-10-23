package com.studyapp.decaycharge.domain

import com.studyapp.decaycharge.model.Chapter
import com.studyapp.decaycharge.model.ChapterStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPlannerTest {
    @Test
    fun `planner prioritizes red chapters within the red budget`() {
        val plan = DailyPlanner.build(
            chapters = listOf(
                chapter(id = 1, battery = 10f, urgency = 2f),
                chapter(id = 2, battery = 10f, urgency = 1f),
                chapter(id = 3, battery = 10f, urgency = 3f),
            ),
            budgetMinutes = 120,
        )

        assertEquals(PlannedChargeKind.RED_CHARGE, plan.first().kind)
        assertEquals(3L, plan.first().chapter.id)
        assertTrue(plan.filter { it.kind == PlannedChargeKind.RED_CHARGE }.sumOf { it.minutes } <= 84)
        assertTrue(plan.sumOf { it.minutes } <= 120)
    }

    @Test
    fun `planner adds at most two amber top-ups`() {
        val plan = DailyPlanner.build(
            chapters = listOf(
                chapter(id = 1, battery = 50f, urgency = 3f),
                chapter(id = 2, battery = 50f, urgency = 2f),
                chapter(id = 3, battery = 50f, urgency = 1f),
            ),
            budgetMinutes = 120,
        )

        val topUps = plan.filter { it.kind == PlannedChargeKind.AMBER_TOP_UP }
        assertEquals(2, topUps.size)
        assertTrue(topUps.all { it.minutes == 15 })
    }

    @Test
    fun `planner unlocks one chapter when red load is light`() {
        val plan = DailyPlanner.build(
            chapters = listOf(
                chapter(id = 1, battery = 50f, urgency = 1f),
                chapter(id = 2, status = ChapterStatus.LOCKED, urgency = 5f),
            ),
            budgetMinutes = 120,
        )

        assertEquals(PlannedChargeKind.UNLOCK, plan.last().kind)
        assertEquals(2L, plan.last().chapter.id)
    }

    @Test
    fun `planner does not unlock while more than three red chapters remain`() {
        val reds = (1L..4L).map { id ->
            chapter(id = id, battery = 10f, urgency = 1f)
        }
        val plan = DailyPlanner.build(
            chapters = reds + chapter(id = 9, status = ChapterStatus.LOCKED, urgency = 9f),
            budgetMinutes = 360,
        )

        assertTrue(plan.none { it.kind == PlannedChargeKind.UNLOCK })
    }

    @Test
    fun `planner returns no work for an empty syllabus`() {
        assertTrue(DailyPlanner.build(emptyList(), 120).isEmpty())
    }

    private fun chapter(
        id: Long,
        battery: Float = 50f,
        status: ChapterStatus = ChapterStatus.ACTIVE,
        urgency: Float = 1f,
    ): Chapter = Chapter(
        id = id,
        subjectId = 1L,
        subjectName = "Biology",
        subjectShortName = "BIO",
        subjectColorHex = 0xFF2E7D32,
        unitName = "Unit 1",
        title = "Chapter $id",
        status = status,
        battery = battery,
        stabilityDays = 4f,
        daysSinceCharge = 2,
        drainRatePerDay = 10f,
        difficultyWeight = 1f,
        targetMinutes = 45,
        reviewCount = 1,
        note = null,
        subjectExamWeight = urgency,
    )
}
