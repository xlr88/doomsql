package com.manish.doomsql

import com.manish.doomsql.data.engine.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 9, 22) // Tuesday
    private val yesterday = today.minusDays(1)
    private val twoDaysAgo = today.minusDays(2)
    private val threeDaysAgo = today.minusDays(3)

    // 1. Consecutive days (e.g. today, yesterday, 2 days ago -> streak = 3)
    @Test
    fun testConsecutiveDaysActive() {
        val activeDates = setOf(today, yesterday, twoDaysAgo)
        val streak = StreakCalculator.calculateCurrentStreak(activeDates, today)
        assertEquals(3, streak)
    }

    // 2. Gap (e.g. today active, yesterday active, 3 days ago active but 2 days ago missing -> streak = 2)
    @Test
    fun testGapBreaksCurrentStreak() {
        val activeDates = setOf(today, yesterday, threeDaysAgo) // gap at twoDaysAgo
        val streak = StreakCalculator.calculateCurrentStreak(activeDates, today)
        assertEquals(2, streak)
    }

    // 3. Today inactive but yesterday active (e.g. yesterday and 2 days ago active -> streak = 2)
    @Test
    fun testTodayInactiveButYesterdayActive() {
        val activeDates = setOf(yesterday, twoDaysAgo)
        val streak = StreakCalculator.calculateCurrentStreak(activeDates, today)
        assertEquals(2, streak)
    }

    // 4. Today and yesterday inactive (e.g. only 2 days ago active -> streak = 0)
    @Test
    fun testTodayAndYesterdayInactive() {
        val activeDates = setOf(twoDaysAgo, threeDaysAgo)
        val streak = StreakCalculator.calculateCurrentStreak(activeDates, today)
        assertEquals(0, streak)
    }

    // 5. Empty active dates -> streak = 0
    @Test
    fun testEmptyActiveDates() {
        val activeDates = emptySet<LocalDate>()
        val streak = StreakCalculator.calculateCurrentStreak(activeDates, today)
        assertEquals(0, streak)
    }

    // 6. Longest streak calculation
    @Test
    fun testLongestStreak() {
        val day1 = LocalDate.of(2026, 9, 1)
        val day2 = LocalDate.of(2026, 9, 2)
        val day3 = LocalDate.of(2026, 9, 3)
        val day4 = LocalDate.of(2026, 9, 4)
        val day5 = LocalDate.of(2026, 9, 5)

        val day10 = LocalDate.of(2026, 9, 10)
        val day11 = LocalDate.of(2026, 9, 11)

        val activeDates = setOf(day1, day2, day3, day4, day5, day10, day11)
        val longest = StreakCalculator.calculateLongestStreak(activeDates)
        assertEquals(5, longest)
    }
}
