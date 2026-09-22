package com.manish.doomsql.data.engine

import java.time.LocalDate

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val activeDaysThisWeek: List<Boolean>, // 7 booleans for Monday through Sunday
    val solvedThisWeek: Int
)

object StreakCalculator {

    /**
     * An "active day" is a date where at least one query was run.
     * Current streak:
     * - If today is active, count back consecutive active days starting from today.
     * - Otherwise (if today is not active yet), start checking from yesterday.
     *   If yesterday was active, count consecutive active days starting from yesterday.
     * - If neither today nor yesterday was active, current streak is 0.
     * Calculated dynamically from dates, never a stored counter.
     */
    fun calculateCurrentStreak(activeDates: Set<LocalDate>, today: LocalDate): Int {
        if (today in activeDates) {
            var streak = 0
            var checkDate = today
            while (checkDate in activeDates) {
                streak++
                checkDate = checkDate.minusDays(1)
            }
            return streak
        }

        val yesterday = today.minusDays(1)
        if (yesterday in activeDates) {
            var streak = 0
            var checkDate = yesterday
            while (checkDate in activeDates) {
                streak++
                checkDate = checkDate.minusDays(1)
            }
            return streak
        }

        return 0
    }

    /**
     * Longest streak: maximum consecutive active dates across all recorded dates.
     */
    fun calculateLongestStreak(activeDates: Set<LocalDate>): Int {
        if (activeDates.isEmpty()) return 0
        val sorted = activeDates.sorted()
        var maxStreak = 0
        var currentStreak = 0
        var prevDate: LocalDate? = null

        for (date in sorted) {
            if (prevDate == null || date == prevDate.plusDays(1)) {
                currentStreak++
            } else if (date != prevDate) {
                currentStreak = 1
            }
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
            prevDate = date
        }
        return maxStreak
    }

    /**
     * Returns 7 booleans representing Monday through Sunday for the current week containing [today].
     */
    fun getWeekActiveDays(activeDates: Set<LocalDate>, today: LocalDate): List<Boolean> {
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return (0..6).map { dayOffset ->
            val date = monday.plusDays(dayOffset.toLong())
            date in activeDates
        }
    }

    fun getMondayOfWeek(today: LocalDate): LocalDate {
        return today.minusDays((today.dayOfWeek.value - 1).toLong())
    }

    fun getSundayOfWeek(today: LocalDate): LocalDate {
        return getMondayOfWeek(today).plusDays(6)
    }

    fun getDaysOfWeek(today: LocalDate): List<LocalDate> {
        val monday = getMondayOfWeek(today)
        return (0..6).map { monday.plusDays(it.toLong()) }
    }
}
