package com.studyapp.decaycharge.domain

import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.Zone
import kotlin.math.exp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DecayCalculatorTest {
    private val day = 86_400_000L
    private val start = 1_700_000_000_000L

    @Test
    fun `elapsed time is zero without a previous charge`() {
        assertEquals(0L, DecayCalculator.activeElapsedMs(null, start, emptyList()))
        assertEquals(0f, DecayCalculator.elapsedDays(null, start, emptyList()), 0f)
    }

    @Test
    fun `elapsed time is zero for future or identical timestamps`() {
        assertEquals(0L, DecayCalculator.activeElapsedMs(start, start, emptyList()))
        assertEquals(0L, DecayCalculator.activeElapsedMs(start, start - 1, emptyList()))
        assertEquals(0L, DecayCalculator.frozenOverlapMs(start, start, emptyList()))
    }

    @Test
    fun `elapsed time without freezes equals wall time`() {
        val now = start + 10 * day
        assertEquals(10 * day, DecayCalculator.activeElapsedMs(start, now, emptyList()))
        assertEquals(10f, DecayCalculator.elapsedDays(start, now, emptyList()), 0.0001f)
    }

    @Test
    fun `closed freeze subtracts only its overlapping portion`() {
        val now = start + 10 * day
        val freeze = CryoFreezeInterval(start + 2 * day, start + 3 * day)
        assertEquals(9 * day, DecayCalculator.activeElapsedMs(start, now, listOf(freeze)))
    }

    @Test
    fun `open freeze subtracts through the current time`() {
        val now = start + 10 * day
        val freeze = CryoFreezeInterval(start + 8 * day, null)
        assertEquals(8 * day, DecayCalculator.activeElapsedMs(start, now, listOf(freeze)))
    }

    @Test
    fun `freezes outside the charge window are ignored`() {
        val now = start + 10 * day
        val freezes = listOf(
            CryoFreezeInterval(start - 3 * day, start - day),
            CryoFreezeInterval(now + day, null),
            CryoFreezeInterval(now + 2 * day, now + 3 * day),
        )
        assertEquals(10 * day, DecayCalculator.activeElapsedMs(start, now, freezes))
    }

    @Test
    fun `overlapping freezes are merged before subtraction`() {
        val now = start + 10 * day
        val freezes = listOf(
            CryoFreezeInterval(start + 2 * day, start + 4 * day),
            CryoFreezeInterval(start + 3 * day, start + 5 * day),
        )
        assertEquals(7 * day, DecayCalculator.activeElapsedMs(start, now, freezes))
    }

    @Test
    fun `disjoint freezes are all subtracted`() {
        val now = start + 10 * day
        val freezes = listOf(
            CryoFreezeInterval(start + 2 * day, start + 3 * day),
            CryoFreezeInterval(start + 5 * day, start + 6 * day),
        )
        assertEquals(8 * day, DecayCalculator.activeElapsedMs(start, now, freezes))
    }

    @Test
    fun `freeze covering the whole window results in zero active time`() {
        val now = start + 10 * day
        val freeze = CryoFreezeInterval(start - day, now + day)
        assertEquals(0L, DecayCalculator.activeElapsedMs(start, now, listOf(freeze)))
    }

    @Test
    fun `invalid freeze intervals are ignored`() {
        val now = start + 10 * day
        val freeze = CryoFreezeInterval(start + 6 * day, start + 4 * day)
        assertEquals(10 * day, DecayCalculator.activeElapsedMs(start, now, listOf(freeze)))
    }

    @Test
    fun `frozen overlap is clamped to the charge window`() {
        val now = start + 2 * day
        val freezes = listOf(
            CryoFreezeInterval(start - day, start + day),
            CryoFreezeInterval(start + day, now + day),
        )
        assertEquals(2 * day, DecayCalculator.frozenOverlapMs(start, now, freezes))
    }

    @Test
    fun `effective stability respects its floor and crunch multiplier`() {
        assertEquals(4f, DecayCalculator.effectiveStabilityDays(4f, 1f), 0.0001f)
        assertEquals(0.5f, DecayCalculator.effectiveStabilityDays(0.1f, 0.1f), 0.0001f)
        assertEquals(2f, DecayCalculator.effectiveStabilityDays(4f, 1f, 0.5f), 0.0001f)
        assertEquals(4f, DecayCalculator.effectiveStabilityDays(4f, 1f, 0f), 0.0001f)
    }

    @Test
    fun `locked chapters and empty batteries have no charge`() {
        assertEquals(
            0f,
            DecayCalculator.batteryLevel(
                100f, ChapterStatus.LOCKED, 4f, 1f, 0f,
            ),
            0f,
        )
        assertEquals(
            0f,
            DecayCalculator.batteryLevel(
                0f, ChapterStatus.ACTIVE, 4f, 1f, 0f,
            ),
            0f,
        )
    }

    @Test
    fun `battery begins full and follows the ebbinghaus curve`() {
        val atStart = DecayCalculator.batteryLevel(
            100f, ChapterStatus.ACTIVE, 4f, 1f, 0f,
        )
        assertEquals(100f, atStart, 0.0001f)

        val oneStability = DecayCalculator.batteryLevel(
            100f, ChapterStatus.ACTIVE, 4f, 1f, 4f,
        )
        assertEquals((100f * exp(-1.05).toFloat()), oneStability, 0.001f)
        assertEquals(35f, oneStability, 0.05f)
    }

    @Test
    fun `battery clamps to the active floor and full ceiling`() {
        val drained = DecayCalculator.batteryLevel(
            100f, ChapterStatus.ACTIVE, 4f, 1f, 1_000f,
        )
        assertEquals(1f, drained, 0f)

        val capped = DecayCalculator.batteryLevel(
            120f, ChapterStatus.ACTIVE, 4f, 1f, 0f,
        )
        assertEquals(100f, capped, 0f)
    }

    @Test
    fun `archived chapters use a zero floor`() {
        val drained = DecayCalculator.batteryLevel(
            50f, ChapterStatus.ARCHIVED, 4f, 1f, 1_000f,
        )
        assertEquals(0f, drained, 0.0001f)
    }

    @Test
    fun `negative elapsed time is treated as fresh`() {
        val battery = DecayCalculator.batteryLevel(
            80f, ChapterStatus.ACTIVE, 4f, 1f, -5f,
        )
        assertEquals(80f, battery, 0.0001f)
    }

    @Test
    fun `crunch factor shortens effective stability`() {
        val normal = DecayCalculator.batteryLevel(
            100f, ChapterStatus.ACTIVE, 4f, 1f, 2f,
        )
        val crunch = DecayCalculator.batteryLevel(
            100f, ChapterStatus.ACTIVE, 4f, 1f, 2f, 0.5f,
        )
        assertEquals(35f, crunch, 0.05f)
        assertEquals(true, crunch < normal)
    }

    @Test
    fun `projected battery clamps active chapters at the floor`() {
        val dayZero = DecayCalculator.projectedBattery(
            currentBattery = 80f,
            stabilityDays = 3f,
            difficultyWeight = 1f,
            daysAhead = 0f,
        )
        val farFuture = DecayCalculator.projectedBattery(
            currentBattery = 80f,
            stabilityDays = 3f,
            difficultyWeight = 1f,
            daysAhead = 365f,
        )
        assertEquals(80f, dayZero, 0.0001f)
        assertEquals(1f, farFuture, 0.0001f)
    }

    @Test
    fun `projected battery follows the same curve as current decay`() {
        val projected = DecayCalculator.projectedBattery(
            currentBattery = 72f,
            stabilityDays = 4f,
            difficultyWeight = 1.2f,
            daysAhead = 5f,
        )
        val direct = DecayCalculator.batteryLevel(
            lastBatteryLevel = 72f,
            status = ChapterStatus.ACTIVE,
            stabilityDays = 4f,
            difficultyWeight = 1.2f,
            elapsedDays = 5f,
        )
        assertEquals(direct, projected, 0.0001f)
    }

    @Test
    fun `zone boundaries match triage thresholds`() {
        assertEquals(Zone.LOCKED, DecayCalculator.zoneFor(100f, ChapterStatus.LOCKED))
        assertEquals(Zone.RED, DecayCalculator.zoneFor(0f, ChapterStatus.ACTIVE))
        assertEquals(Zone.RED, DecayCalculator.zoneFor(34.9f, ChapterStatus.ACTIVE))
        assertEquals(Zone.AMBER, DecayCalculator.zoneFor(35f, ChapterStatus.ACTIVE))
        assertEquals(Zone.AMBER, DecayCalculator.zoneFor(69.9f, ChapterStatus.ACTIVE))
        assertEquals(Zone.GREEN, DecayCalculator.zoneFor(70f, ChapterStatus.ACTIVE))
        assertEquals(Zone.GREEN, DecayCalculator.zoneFor(100f, ChapterStatus.ACTIVE))
    }

    @Test
    fun `recharge grows proportionally toward full`() {
        assertEquals(40f, DecayCalculator.rechargedBattery(40f, 0f, 45), 0f)
        assertEquals(70f, DecayCalculator.rechargedBattery(40f, 22.5f, 45), 0.0001f)
        assertEquals(100f, DecayCalculator.rechargedBattery(40f, 45f, 45), 0f)
        assertEquals(100f, DecayCalculator.rechargedBattery(40f, 90f, 45), 0f)
        assertEquals(100f, DecayCalculator.rechargedBattery(100f, 45f, 45), 0f)
    }

    @Test
    fun `recharge guards invalid target and input ranges`() {
        assertEquals(35f, DecayCalculator.rechargedBattery(35f, 10f, 0), 0f)
        assertEquals(40f, DecayCalculator.rechargedBattery(40f, -10f, 45), 0f)
        assertEquals(50f, DecayCalculator.rechargedBattery(-20f, 22.5f, 45), 0.0001f)
        assertEquals(100f, DecayCalculator.rechargedBattery(120f, 0f, 45), 0f)
    }

    @Test
    fun `cold start stability depends only on recall rating`() {
        assertEquals(2f, stability(rating = 1, reviews = 0), 0f)
        assertEquals(3f, stability(rating = 2, reviews = 0), 0f)
        assertEquals(4.5f, stability(rating = 3, reviews = 0), 0f)
        assertEquals(4.5f, stability(rating = 3, reviews = -1), 0f)
    }

    @Test
    fun `hard reviews shrink stability with a floor`() {
        assertEquals(3.4f, stability(rating = 1, old = 4f, battery = 60f), 0.0001f)
        assertEquals(1.8f, stability(rating = 1, old = 1.9f, battery = 60f), 0.0001f)
        assertEquals(1.8f, stability(rating = 1, old = 0f, battery = 60f), 0.0001f)
    }

    @Test
    fun `flatline hard reviews reset stability`() {
        assertEquals(3f, stability(rating = 1, old = 6f, battery = 5f), 0.0001f)
        assertEquals(2f, stability(rating = 1, old = 2f, battery = 5f), 0.0001f)
        assertEquals(5.1f, stability(rating = 1, old = 6f, battery = 5.1f), 0.0001f)
    }

    @Test
    fun `medium and easy reviews grow stability by their multipliers`() {
        assertEquals(6.4f, stability(rating = 2, old = 4f, battery = 60f), 0.0001f)
        assertEquals(8.8f, stability(rating = 3, old = 4f, battery = 60f), 0.0001f)
    }

    @Test
    fun `anti spam timing factor applies above eighty percent`() {
        assertEquals(4.84f, stability(rating = 2, old = 4f, battery = 81f), 0.0001f)
        assertEquals(6.4f, stability(rating = 2, old = 4f, battery = 80f), 0.0001f)
    }

    @Test
    fun `charge ratio is floored at a quarter and capped at one`() {
        assertEquals(4.6f, stability(rating = 2, old = 4f, ratio = 0f), 0.0001f)
        assertEquals(4.6f, stability(rating = 2, old = 4f, ratio = -2f), 0.0001f)
        assertEquals(6.4f, stability(rating = 2, old = 4f, ratio = 2f), 0.0001f)
    }

    @Test
    fun `stability growth is capped at one hundred eighty days`() {
        assertEquals(180f, stability(rating = 3, old = 100f, battery = 60f), 0f)
    }

    @Test
    fun `quick top-up growth scale softens stability gains`() {
        assertEquals(5.8f, stabilityWithScale(scale = 0.75f), 0.0001f)
        assertEquals(4f, stabilityWithScale(scale = 0f), 0.0001f)
        assertEquals(6.4f, stabilityWithScale(scale = 1f), 0.0001f)
    }

    @Test
    fun `exam crunch is neutral without an upcoming exam`() {
        assertEquals(1f, DecayCalculator.examCrunchFactor(null, 1.2f), 0f)
        assertEquals(1f, DecayCalculator.examCrunchFactor(31f, 1.2f), 0f)
        assertEquals(1f, DecayCalculator.examCrunchFactor(365f, 2f), 0f)
    }

    @Test
    fun `exam crunch scales with urgency and subject weight`() {
        assertEquals(0.55f, DecayCalculator.examCrunchFactor(0f, 1.2f), 0.0001f)
        assertEquals(0.73f, DecayCalculator.examCrunchFactor(0f, 0.5f), 0.0001f)
        assertEquals(0.45f, DecayCalculator.examCrunchFactor(0f, 2f), 0.0001f)
        assertEquals(0.775f, DecayCalculator.examCrunchFactor(15f, 1.2f), 0.0001f)
    }

    @Test
    fun `exam crunch accelerates visible decay`() {
        val normal = DecayCalculator.batteryLevel(
            100f, ChapterStatus.ACTIVE, 4f, 1f, 2f,
        )
        val crunch = DecayCalculator.batteryLevel(
            100f, ChapterStatus.ACTIVE, 4f, 1f, 2f, 0.55f,
        )
        assertEquals(true, crunch < normal)
    }

    @Test
    fun `invalid recall ratings are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            DecayCalculator.stabilityAfterReview(4f, 1, 0, 50f, 1f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DecayCalculator.stabilityAfterReview(4f, 0, 4, 50f, 1f)
        }
    }

    @Test
    fun `drain rate responds to battery and stability`() {
        assertEquals(0f, DecayCalculator.drainRatePerDay(0f, 4f, 1f), 0f)
        assertEquals(13.125f, DecayCalculator.drainRatePerDay(50f, 4f, 1f), 0.0001f)
        assertEquals(
            26.25f,
            DecayCalculator.drainRatePerDay(50f, 4f, 1f, 0.5f),
            0.0001f,
        )
    }

    @Test
    fun `snapshot returns zeros when a chapter has never charged`() {
        val snapshot = DecayCalculator.snapshot(
            lastChargedAtEpochMs = null,
            nowEpochMs = start,
            freezes = emptyList(),
            lastBatteryLevel = 100f,
            status = ChapterStatus.ACTIVE,
            stabilityDays = 4f,
            difficultyWeight = 1f,
        )
        assertEquals(0f, snapshot.battery, 0f)
        assertEquals(0f, snapshot.elapsedDays, 0f)
        assertEquals(0, snapshot.daysSinceCharge)
        assertEquals(0f, snapshot.drainRatePerDay, 0f)
        assertEquals(4f, snapshot.effectiveStabilityDays, 0f)
    }

    @Test
    fun `snapshot combines decay timing and rate`() {
        val snapshot = DecayCalculator.snapshot(
            lastChargedAtEpochMs = start,
            nowEpochMs = start + 4 * day,
            freezes = listOf(CryoFreezeInterval(start + day, start + 2 * day)),
            lastBatteryLevel = 100f,
            status = ChapterStatus.ACTIVE,
            stabilityDays = 4f,
            difficultyWeight = 1f,
        )
        assertEquals(3f, snapshot.elapsedDays, 0.0001f)
        assertEquals(3, snapshot.daysSinceCharge)
        assertEquals(45.50f, snapshot.battery, 0.01f)
        assertEquals(11.94f, snapshot.drainRatePerDay, 0.01f)
    }

    @Test
    fun `snapshot keeps locked chapters at zero battery`() {
        val snapshot = DecayCalculator.snapshot(
            lastChargedAtEpochMs = start,
            nowEpochMs = start + 4 * day,
            freezes = emptyList(),
            lastBatteryLevel = 100f,
            status = ChapterStatus.LOCKED,
            stabilityDays = 4f,
            difficultyWeight = 1f,
        )
        assertEquals(0f, snapshot.battery, 0f)
        assertEquals(4f, snapshot.elapsedDays, 0.0001f)
        assertEquals(4, snapshot.daysSinceCharge)
        assertEquals(0f, snapshot.drainRatePerDay, 0f)
    }

    private fun stability(
        rating: Int,
        reviews: Int = 1,
        old: Float = 4f,
        battery: Float = 60f,
        ratio: Float = 1f,
    ): Float = DecayCalculator.stabilityAfterReview(
        oldStabilityDays = old,
        reviewCount = reviews,
        recallRating = rating,
        batteryBefore = battery,
        chargeRatio = ratio,
    )

    private fun stabilityWithScale(scale: Float): Float = DecayCalculator.stabilityAfterReview(
        oldStabilityDays = 4f,
        reviewCount = 1,
        recallRating = 2,
        batteryBefore = 60f,
        chargeRatio = 1f,
        growthScale = scale,
    )
}
