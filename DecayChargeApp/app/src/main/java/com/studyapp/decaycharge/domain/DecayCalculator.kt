package com.studyapp.decaycharge.domain

import com.studyapp.decaycharge.model.ChapterStatus
import com.studyapp.decaycharge.model.Zone
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class CryoFreezeInterval(
    val startEpochMs: Long,
    val endEpochMs: Long?,
)

data class DecaySnapshot(
    val battery: Float,
    val elapsedDays: Float,
    val effectiveStabilityDays: Float,
    val daysSinceCharge: Int,
    val drainRatePerDay: Float,
)

object DecayCalculator {
    const val MILLIS_PER_DAY = 86_400_000.0

    fun activeElapsedMs(
        lastChargedAtEpochMs: Long?,
        nowEpochMs: Long,
        freezes: List<CryoFreezeInterval>,
    ): Long {
        if (lastChargedAtEpochMs == null || nowEpochMs <= lastChargedAtEpochMs) return 0L
        val span = nowEpochMs - lastChargedAtEpochMs
        return (span - frozenOverlapMs(lastChargedAtEpochMs, nowEpochMs, freezes))
            .coerceIn(0L, span)
    }

    fun elapsedDays(
        lastChargedAtEpochMs: Long?,
        nowEpochMs: Long,
        freezes: List<CryoFreezeInterval>,
    ): Float = (activeElapsedMs(lastChargedAtEpochMs, nowEpochMs, freezes) / MILLIS_PER_DAY)
        .toFloat()

    fun frozenOverlapMs(
        lastChargedAtEpochMs: Long,
        nowEpochMs: Long,
        freezes: List<CryoFreezeInterval>,
    ): Long {
        if (nowEpochMs <= lastChargedAtEpochMs) return 0L
        val clipped = freezes.mapNotNull { freeze ->
            val start = max(lastChargedAtEpochMs, freeze.startEpochMs)
            val end = min(nowEpochMs, freeze.endEpochMs ?: nowEpochMs)
            if (end > start) start to end else null
        }.sortedBy { it.first }

        var total = 0L
        var mergedStart: Long? = null
        var mergedEnd = 0L
        clipped.forEach { (start, end) ->
            if (mergedStart == null) {
                mergedStart = start
                mergedEnd = end
            } else if (start <= mergedEnd) {
                mergedEnd = max(mergedEnd, end)
            } else {
                total += mergedEnd - mergedStart!!
                mergedStart = start
                mergedEnd = end
            }
        }
        mergedStart?.let { total += mergedEnd - it }
        return total.coerceAtMost(nowEpochMs - lastChargedAtEpochMs)
    }

    fun effectiveStabilityDays(
        stabilityDays: Float,
        difficultyWeight: Float,
        examCrunchFactor: Float = 1f,
    ): Float {
        val crunch = examCrunchFactor.takeIf { it > 0f } ?: 1f
        return max(0.5f, stabilityDays * difficultyWeight) * crunch
    }

    fun batteryLevel(
        lastBatteryLevel: Float,
        status: ChapterStatus,
        stabilityDays: Float,
        difficultyWeight: Float,
        elapsedDays: Float,
        examCrunchFactor: Float = 1f,
    ): Float {
        if (status == ChapterStatus.LOCKED || lastBatteryLevel <= 0f) return 0f
        val effectiveStability = effectiveStabilityDays(
            stabilityDays = stabilityDays,
            difficultyWeight = difficultyWeight,
            examCrunchFactor = examCrunchFactor,
        )
        val decayed = lastBatteryLevel * exp(
            -1.05 * (elapsedDays.coerceAtLeast(0f) / effectiveStability),
        ).toFloat()
        val floor = if (status == ChapterStatus.ACTIVE) 1f else 0f
        return decayed.coerceIn(floor, 100f)
    }

    fun projectedBattery(
        currentBattery: Float,
        stabilityDays: Float,
        difficultyWeight: Float,
        daysAhead: Float,
        status: ChapterStatus = ChapterStatus.ACTIVE,
        examCrunchFactor: Float = 1f,
    ): Float = batteryLevel(
        lastBatteryLevel = currentBattery,
        status = status,
        stabilityDays = stabilityDays,
        difficultyWeight = difficultyWeight,
        elapsedDays = daysAhead,
        examCrunchFactor = examCrunchFactor,
    )

    fun zoneFor(battery: Float, status: ChapterStatus): Zone = when {
        status == ChapterStatus.LOCKED -> Zone.LOCKED
        battery < 35f -> Zone.RED
        battery < 70f -> Zone.AMBER
        else -> Zone.GREEN
    }

    fun rechargedBattery(
        currentBattery: Float,
        elapsedMinutes: Float,
        targetMinutes: Int,
    ): Float {
        val current = currentBattery.coerceIn(0f, 100f)
        if (targetMinutes <= 0) return current
        val ratio = (elapsedMinutes.coerceAtLeast(0f) / targetMinutes.toFloat()).coerceIn(0f, 1f)
        return min(100f, current + ratio * (100f - current))
    }

    fun stabilityAfterReview(
        oldStabilityDays: Float,
        reviewCount: Int,
        recallRating: Int,
        batteryBefore: Float,
        chargeRatio: Float,
        growthScale: Float = 1f,
    ): Float {
        require(recallRating in 1..3) { "Recall rating must be 1, 2, or 3." }
        if (reviewCount <= 0) {
            return when (recallRating) {
                1 -> 2f
                2 -> 3f
                else -> 4.5f
            }
        }

        val oldStability = oldStabilityDays.coerceAtLeast(0f)
        if (recallRating == 1) {
            return if (batteryBefore <= 5f) {
                max(2f, oldStability * 0.5f)
            } else {
                max(1.8f, oldStability * 0.85f)
            }
        }

        val multiplier = if (recallRating == 2) 1.6f else 2.2f
        val timingFactor = if (batteryBefore > 80f) 0.35f else 1f
        val ratio = chargeRatio.coerceIn(0.25f, 1f)
        val growth = (multiplier - 1f) * ratio * timingFactor * growthScale.coerceIn(0f, 1f)
        val updated = oldStability * (1f + growth)
        return min(180f, updated)
    }

    fun examCrunchFactor(daysUntilExam: Float?, examWeight: Float): Float {
        if (daysUntilExam == null) return 1f
        val days = daysUntilExam.coerceAtLeast(0f)
        if (days > 30f) return 1f
        val urgency = 1f - (days / 30f)
        val weight = (examWeight.coerceIn(0.5f, 2f) / 1.2f).coerceIn(0.6f, 1.4f)
        return (1f - 0.45f * urgency * weight).coerceIn(0.45f, 1f)
    }

    fun drainRatePerDay(
        battery: Float,
        stabilityDays: Float,
        difficultyWeight: Float,
        examCrunchFactor: Float = 1f,
    ): Float {
        if (battery <= 0f) return 0f
        val effectiveStability = effectiveStabilityDays(
            stabilityDays = stabilityDays,
            difficultyWeight = difficultyWeight,
            examCrunchFactor = examCrunchFactor,
        )
        return (battery * 1.05f / effectiveStability).coerceAtLeast(0f)
    }

    fun snapshot(
        lastChargedAtEpochMs: Long?,
        nowEpochMs: Long,
        freezes: List<CryoFreezeInterval>,
        lastBatteryLevel: Float,
        status: ChapterStatus,
        stabilityDays: Float,
        difficultyWeight: Float,
        examCrunchFactor: Float = 1f,
    ): DecaySnapshot {
        if (lastChargedAtEpochMs == null) {
            return DecaySnapshot(
                battery = 0f,
                elapsedDays = 0f,
                effectiveStabilityDays = effectiveStabilityDays(
                    stabilityDays,
                    difficultyWeight,
                    examCrunchFactor,
                ),
                daysSinceCharge = 0,
                drainRatePerDay = 0f,
            )
        }
        val elapsed = elapsedDays(lastChargedAtEpochMs, nowEpochMs, freezes)
        val battery = batteryLevel(
            lastBatteryLevel = lastBatteryLevel,
            status = status,
            stabilityDays = stabilityDays,
            difficultyWeight = difficultyWeight,
            elapsedDays = elapsed,
            examCrunchFactor = examCrunchFactor,
        )
        return DecaySnapshot(
            battery = battery,
            elapsedDays = elapsed,
            effectiveStabilityDays = effectiveStabilityDays(
                stabilityDays,
                difficultyWeight,
                examCrunchFactor,
            ),
            daysSinceCharge = elapsed.toInt(),
            drainRatePerDay = drainRatePerDay(
                battery = battery,
                stabilityDays = stabilityDays,
                difficultyWeight = difficultyWeight,
                examCrunchFactor = examCrunchFactor,
            ),
        )
    }
}
