package com.sweetapps.nocaffeinediet.core.util

import android.content.Context
import com.sweetapps.nocaffeinediet.core.model.SobrietyRecord
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object LifePlusUtils {
    // MaxGoal(%) mapping based on cost(style) and frequency(cups)
    // IDs ref: H3/H2/H1, M3/M2/M1, L3/L2/L1
    fun maxGoalPercent(cost: String, frequency: String): Int {
        return when (cost) {
            "프리미엄" -> when (frequency) {
                "3잔 이상" -> 15 // H3
                "2잔" -> 14      // H2
                "1잔" -> 12      // H1
                else -> 12
            }
            "프랜차이즈" -> when (frequency) {
                "3잔 이상" -> 14 // M3
                "2잔" -> 12      // M2
                "1잔" -> 11      // M1
                else -> 11
            }
            "가성비" -> when (frequency) {
                "3잔 이상" -> 12 // L3
                "2잔" -> 11      // L2
                "1잔" -> 10      // L1
                else -> 10
            }
            else -> { // fallback to defaults
                when (frequency) {
                    "3잔 이상" -> 12
                    "2잔" -> 11
                    else -> 10
                }
            }
        }
    }

    // Compute '삶의 질+' percent given total success days within the 30-day target window and user settings.
    fun computeLifePlusPercent(totalSuccessDays: Int, cost: String, frequency: String): Int {
        val cappedDays = totalSuccessDays.coerceIn(0, 30)
        val maxGoal = maxGoalPercent(cost, frequency)
        val percent = (cappedDays / 30.0) * maxGoal
        return PercentUtils.roundPercent(percent)
    }

    // Overload using stored user settings.
    fun computeLifePlusPercent(context: Context, totalSuccessDays: Int): Int {
        val (cost, frequency) = Constants.getUserSettings(context)
        return computeLifePlusPercent(totalSuccessDays, cost, frequency)
    }

    // Compute '삶의 질+' percent with one decimal place (HALF_UP)
    fun computeLifePlusPercentOneDecimal(totalSuccessDays: Int, cost: String, frequency: String): Double {
        val cappedDays = totalSuccessDays.coerceIn(0, 30)
        val maxGoal = maxGoalPercent(cost, frequency)
        val value = (cappedDays / 30.0) * maxGoal
        return BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toDouble()
    }

    fun computeLifePlusPercentOneDecimal(context: Context, totalSuccessDays: Int): Double {
        val (cost, frequency) = Constants.getUserSettings(context)
        return computeLifePlusPercentOneDecimal(totalSuccessDays, cost, frequency)
    }

    // Compute LifePlus with Double days input (one decimal HALF_UP)
    fun computeLifePlusPercentOneDecimal(totalSuccessDaysDays: Double, cost: String, frequency: String): Double {
        val cappedDays = totalSuccessDaysDays.coerceIn(0.0, 30.0)
        val maxGoal = maxGoalPercent(cost, frequency)
        val value = (cappedDays / 30.0) * maxGoal
        return BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toDouble()
    }

    // Calculate D_total: total success days within the last [days] window from now.
    fun computeSuccessDaysInLastDays(records: List<SobrietyRecord>, days: Int = 30, nowMillis: Long = System.currentTimeMillis()): Int {
        if (days <= 0) return 0
        val windowStart = nowMillis - days * Constants.DAY_IN_MILLIS
        val totalDays = records.sumOf { record ->
            val overlapStart = max(record.startTime, windowStart)
            val overlapEnd = min(record.endTime, nowMillis)
            if (overlapStart < overlapEnd) {
                ((overlapEnd - overlapStart) / (Constants.DAY_IN_MILLIS.toFloat())).roundToInt()
            } else 0
        }
        return totalDays.coerceAtMost(days)
    }

    // Calculate D_total precisely (Double days) within the last [days] from now
    fun computeSuccessDaysInLastDaysPrecise(records: List<SobrietyRecord>, days: Int = 30, nowMillis: Long = System.currentTimeMillis()): Double {
        if (days <= 0) return 0.0
        val windowStart = nowMillis - days * Constants.DAY_IN_MILLIS
        val totalDays = records.sumOf { record ->
            val overlapStart = max(record.startTime, windowStart)
            val overlapEnd = min(record.endTime, nowMillis)
            if (overlapStart < overlapEnd) {
                (overlapEnd - overlapStart).toDouble() / Constants.DAY_IN_MILLIS.toDouble()
            } else 0.0
        }
        // 상한 보호: 최대 days 일
        return totalDays.coerceAtMost(days.toDouble())
    }
}
