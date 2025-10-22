package com.sweetapps.nocaffeinediet.core.util

import com.sweetapps.nocaffeinediet.core.model.SobrietyRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class LifePlusUtilsTest {

    @Test
    fun maxGoal_mapping_matches_spec() {
        // 프리미엄
        assertEquals(15, LifePlusUtils.maxGoalPercent("프리미엄", "3잔 이상"))
        assertEquals(14, LifePlusUtils.maxGoalPercent("프리미엄", "2잔"))
        assertEquals(12, LifePlusUtils.maxGoalPercent("프리미엄", "1잔"))
        // 프랜차이즈
        assertEquals(14, LifePlusUtils.maxGoalPercent("프랜차이즈", "3잔 이상"))
        assertEquals(12, LifePlusUtils.maxGoalPercent("프랜차이즈", "2잔"))
        assertEquals(11, LifePlusUtils.maxGoalPercent("프랜차이즈", "1잔"))
        // 가성비
        assertEquals(12, LifePlusUtils.maxGoalPercent("가성비", "3잔 이상"))
        assertEquals(11, LifePlusUtils.maxGoalPercent("가성비", "2잔"))
        assertEquals(10, LifePlusUtils.maxGoalPercent("가성비", "1잔"))
    }

    @Test
    fun computeLifePlusPercent_basic_and_rounding() {
        // 예시: 프리미엄 + 2잔 (14%), D_total=10 -> (10/30)*14=4.666.. -> 5%
        assertEquals(5, LifePlusUtils.computeLifePlusPercent(10, "프리미엄", "2잔"))
        // 최대 상한: 30일 달성 시 MaxGoal과 동일
        assertEquals(15, LifePlusUtils.computeLifePlusPercent(30, "프리미엄", "3잔 이상"))
        // 하한: 음수 입력은 0으로 취급
        assertEquals(0, LifePlusUtils.computeLifePlusPercent(-3, "가성비", "1잔"))
        // 30일 초과 cap 확인
        assertEquals(12, LifePlusUtils.computeLifePlusPercent(40, "가성비", "3잔 이상"))
    }

    @Test
    fun computeLifePlusPercent_one_decimal_rounding() {
        // 10/30 * 14 = 4.666.. -> 4.7
        assertEquals(4.7, LifePlusUtils.computeLifePlusPercentOneDecimal(10, "프리미엄", "2잔"), 1e-9)
        // 상한: 30일 달성
        assertEquals(15.0, LifePlusUtils.computeLifePlusPercentOneDecimal(30, "프리미엄", "3잔 이상"), 1e-9)
        // 하한: 음수 -> 0.0
        assertEquals(0.0, LifePlusUtils.computeLifePlusPercentOneDecimal(-2, "가성비", "1잔"), 1e-9)
        // cap: 40일 -> MaxGoal 12.0
        assertEquals(12.0, LifePlusUtils.computeLifePlusPercentOneDecimal(40, "가성비", "3잔 이상"), 1e-9)
        // 미세 반올림: L1(10%), D_total=2 -> 2/30*10 = 0.666.. -> 0.7
        assertEquals(0.7, LifePlusUtils.computeLifePlusPercentOneDecimal(2, "가성비", "1잔"), 1e-9)
    }

    @Test
    fun computeSuccessDaysInLastDays_window_overlap_cases() {
        val day = Constants.DAY_IN_MILLIS
        val now = 100L * day
        // 완전 겹침: 마지막 5일
        val r1 = SobrietyRecord(
            id = "r1", startTime = now - 5 * day, endTime = now,
            targetDays = 0, actualDays = 5, isCompleted = true,
            status = "완료", createdAt = now
        )
        // 비겹침: 40~35일 전 (0)
        val r2 = SobrietyRecord(
            id = "r2", startTime = now - 40 * day, endTime = now - 35 * day,
            targetDays = 0, actualDays = 5, isCompleted = true,
            status = "완료", createdAt = now
        )
        // 부분 겹침: 35~25일 전 -> 윈도우(30일)과 30~25 구간 5일만 카운트
        val r3 = SobrietyRecord(
            id = "r3", startTime = now - 35 * day, endTime = now - 25 * day,
            targetDays = 0, actualDays = 10, isCompleted = true,
            status = "완료", createdAt = now
        )
        val dTotal = LifePlusUtils.computeSuccessDaysInLastDays(listOf(r1, r2, r3), 30, now)
        assertEquals(10, dTotal) // 5 + 0 + 5 = 10

        // 상한 cap: 50일 모두 현재 30일에 포함되더라도 30으로 cap
        val r4 = SobrietyRecord(
            id = "r4", startTime = now - 50 * day, endTime = now,
            targetDays = 0, actualDays = 50, isCompleted = true,
            status = "완료", createdAt = now
        )
        val capped = LifePlusUtils.computeSuccessDaysInLastDays(listOf(r4), 30, now)
        assertEquals(30, capped)
    }
}
