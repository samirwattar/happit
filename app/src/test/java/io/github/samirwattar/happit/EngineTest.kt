package io.github.samirwattar.happit

import io.github.samirwattar.happit.data.Bonus
import io.github.samirwattar.happit.data.Entry
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.Slot
import io.github.samirwattar.happit.logic.Achievement
import io.github.samirwattar.happit.logic.Engine
import io.github.samirwattar.happit.logic.Leveling
import io.github.samirwattar.happit.logic.Rank
import io.github.samirwattar.happit.logic.Streaks
import io.github.samirwattar.happit.logic.Xp
import io.github.samirwattar.happit.update.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EngineTest {
    // Monday 19 October 2026
    private val monday = LocalDate.of(2026, 10, 19).toEpochDay()

    private fun habit(id: Long, created: Long = monday, days: Int = Habit.ALL_DAYS, slot: Int = Slot.ANYTIME) =
        Habit(id = id, name = "h$id", icon = "star", color = 0, createdDay = created, days = days, slot = slot)

    private fun done(id: Long, vararg days: Long) = days.map { Entry(id, it, 1, true, 10) }

    @Test
    fun levelingCurve() {
        assertEquals(0, Leveling.totalFor(1))
        assertEquals(100, Leveling.totalFor(2))
        assertEquals(220, Leveling.totalFor(3))
        assertEquals(1, Leveling.info(0).level)
        assertEquals(1, Leveling.info(99).level)
        val l2 = Leveling.info(100)
        assertEquals(2, l2.level)
        assertEquals(0, l2.into)
        assertEquals(120, l2.needed)
        assertEquals(3, Leveling.info(220).level)
    }

    @Test
    fun ranksFollowLevels() {
        assertEquals(Rank.BEGINNER, Rank.forLevel(1))
        assertEquals(Rank.APPRENTICE, Rank.forLevel(9))
        assertEquals(Rank.COMMITTED, Rank.forLevel(10))
        assertEquals(Rank.LEGEND, Rank.forLevel(80))
        assertEquals(null, Rank.LEGEND.next)
    }

    @Test
    fun streakBonusXp() {
        assertEquals(10, Xp.forCompletion(1))
        assertEquals(15, Xp.forCompletion(7))
        assertEquals(20, Xp.forCompletion(30))
    }

    @Test
    fun unfinishedTodayDoesNotBreakStreak() {
        val h = habit(1)
        val doneDays = (monday..monday + 4).toSet()
        assertEquals(5, Streaks.habitCurrent(h, doneDays, emptySet(), monday + 5))
        assertEquals(6, Streaks.habitCurrent(h, doneDays + (monday + 5), emptySet(), monday + 5))
    }

    @Test
    fun missedDayBreaksStreakUnlessFrozen() {
        val h = habit(1)
        val doneDays = setOf(monday, monday + 1, monday + 2, monday + 4)
        assertEquals(1, Streaks.habitCurrent(h, doneDays, emptySet(), monday + 5))
        assertEquals(5, Streaks.habitCurrent(h, doneDays, setOf(monday + 3), monday + 5))
    }

    @Test
    fun weekendsSkippedForWeekdayHabit() {
        val weekdays = 0b0011111
        val h = habit(1, days = weekdays)
        val doneDays = (monday..monday + 4).toSet()
        // Next Monday, not done yet: Mon-Fri streak survives the weekend.
        assertEquals(5, Streaks.habitCurrent(h, doneDays, emptySet(), monday + 7))
        assertEquals(5, Streaks.habitBest(h, doneDays, emptySet(), monday + 7))
    }

    @Test
    fun summaryTotalsAndPerfectDays() {
        val habits = listOf(habit(1), habit(2))
        val entries = done(1, monday, monday + 1) + done(2, monday)
        val s = Engine.summarize(habits, entries, listOf(Bonus(monday, 50)), emptyList(), monday + 1)
        assertEquals(10 * 3 + 50, s.totalXp)
        assertEquals(3, s.totalDone)
        assertEquals(2, s.streak)
        assertTrue(Streaks.isPerfect(monday, habits, s.doneByHabit))
        assertFalse(Streaks.isPerfect(monday + 1, habits, s.doneByHabit))
        assertEquals(0.5f, s.ratio(monday + 1))
    }

    @Test
    fun freezesOnlyCoverGapsTheyCanBridge() {
        val habits = listOf(habit(1))
        val active = setOf(monday, monday + 1, monday + 2)
        // Missed one day (monday + 3), opening the app on monday + 4.
        assertEquals(
            listOf(monday + 3),
            Engine.daysToFreeze(monday + 2, monday + 4, 1, habits, active, emptySet()),
        )
        // Missed two days with only one freeze: keep the freeze.
        assertEquals(
            emptyList<Long>(),
            Engine.daysToFreeze(monday + 2, monday + 5, 1, habits, active, emptySet()),
        )
        // No streak before the gap: nothing worth saving.
        assertEquals(
            emptyList<Long>(),
            Engine.daysToFreeze(monday - 1, monday + 2, 3, habits, emptySet(), emptySet()),
        )
    }

    @Test
    fun achievementsUnlock() {
        val habits = listOf(habit(1, slot = Slot.MORNING))
        val entries = done(1, *(monday..monday + 6).toList().toLongArray())
        val s = Engine.summarize(habits, entries, emptyList(), emptyList(), monday + 6)
        val got = Engine.achievements(s, habitsCreated = 1)
        assertTrue(Achievement.FIRST_STEP in got)
        assertTrue(Achievement.EARLY_BIRD in got)
        assertTrue(Achievement.WEEK_WARRIOR in got)
        assertFalse(Achievement.ARCHITECT in got)
        assertFalse(Achievement.COMEBACK in got)
    }

    @Test
    fun comebackAfterBreak() {
        val habits = listOf(habit(1))
        val entries = done(1, monday, monday + 5)
        val s = Engine.summarize(habits, entries, emptyList(), emptyList(), monday + 5)
        assertTrue(Achievement.COMEBACK in Engine.achievements(s, 1))
    }

    @Test
    fun versionComparison() {
        assertTrue(Updater.isNewer("1.0.1", "1.0.0"))
        assertTrue(Updater.isNewer("1.10.0", "1.9.9"))
        assertTrue(Updater.isNewer("2.0", "1.9.9"))
        assertFalse(Updater.isNewer("1.0.0", "1.0.0-debug"))
        assertFalse(Updater.isNewer("1.0.0", "1.0.1"))
    }
}
