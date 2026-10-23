package io.github.samirwattar.happit.logic

import io.github.samirwattar.happit.data.Bonus
import io.github.samirwattar.happit.data.Entry
import io.github.samirwattar.happit.data.FrozenDay
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.Slot
import java.time.LocalDate

/** Days are stored as epoch days (days since 1970-01-01) in local time. */
object Days {
    fun today(): Long = LocalDate.now().toEpochDay()
    fun date(day: Long): LocalDate = LocalDate.ofEpochDay(day)

    /** Monday-first weekday bit used by [Habit.days]. */
    fun bit(day: Long): Int = 1 shl (date(day).dayOfWeek.value - 1)
}

enum class Rank(val minLevel: Int) {
    BEGINNER(1), APPRENTICE(5), COMMITTED(10), DISCIPLINED(15),
    WARRIOR(20), CHAMPION(30), MASTER(40), LEGEND(50);

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forLevel(level: Int): Rank = entries.last { level >= it.minLevel }
    }
}

data class LevelInfo(val level: Int, val into: Int, val needed: Int, val total: Int) {
    val progress: Float get() = into.toFloat() / needed
}

/** Each level costs 20 XP more than the one before: 100, 120, 140, ... */
object Leveling {
    fun stepXp(level: Int): Int = 100 + 20 * (level - 1)
    fun totalFor(level: Int): Int = 100 * (level - 1) + 10 * (level - 1) * (level - 2)

    fun info(total: Int): LevelInfo {
        var level = 1
        while (totalFor(level + 1) <= total) level++
        return LevelInfo(level, total - totalFor(level), stepXp(level), total)
    }
}

object Xp {
    const val BASE = 10
    const val PERFECT_DAY = 50
    const val MAX_FREEZES = 5

    /** XP for finishing a habit whose streak (including this completion) is [streak]. */
    fun forCompletion(streak: Int): Int = BASE + when {
        streak >= 30 -> 10
        streak >= 7 -> 5
        else -> 0
    }
}

object Streaks {
    fun scheduled(h: Habit, day: Long): Boolean =
        day >= h.createdDay && (h.days and Days.bit(day)) != 0

    /** Consecutive scheduled days ending at [end] that are done or frozen. */
    fun habitRun(h: Habit, done: Set<Long>, frozen: Set<Long>, end: Long): Int {
        var d = end
        var n = 0
        while (d >= h.createdDay) {
            if (scheduled(h, d)) {
                if (d in done || d in frozen) n++ else break
            }
            d--
        }
        return n
    }

    /** Today only counts once it is done, so an unfinished today never breaks a streak. */
    fun habitCurrent(h: Habit, done: Set<Long>, frozen: Set<Long>, today: Long): Int =
        habitRun(h, done, frozen, if (today in done) today else today - 1)

    fun habitBest(h: Habit, done: Set<Long>, frozen: Set<Long>, today: Long): Int {
        var best = 0
        var run = 0
        var d = h.createdDay
        while (d <= today) {
            if (scheduled(h, d)) {
                if (d in done || d in frozen) {
                    run++
                    if (run > best) best = run
                } else if (d < today) {
                    run = 0
                }
            }
            d++
        }
        return best
    }

    /**
     * Overall streak: consecutive days with at least one habit done (or frozen).
     * Days with nothing scheduled are neutral.
     */
    fun globalRun(
        active: Set<Long>, frozen: Set<Long>, anyScheduled: (Long) -> Boolean, start: Long, end: Long,
    ): Int {
        var d = end
        var n = 0
        while (d >= start) {
            if (d in active || d in frozen) n++ else if (anyScheduled(d)) break
            d--
        }
        return n
    }

    fun globalBest(
        active: Set<Long>, frozen: Set<Long>, anyScheduled: (Long) -> Boolean, start: Long, today: Long,
    ): Int {
        var best = 0
        var run = 0
        var d = start
        while (d <= today) {
            if (d in active || d in frozen) {
                run++
                if (run > best) best = run
            } else if (anyScheduled(d) && d < today) {
                run = 0
            }
            d++
        }
        return best
    }

    fun isPerfect(day: Long, habits: List<Habit>, doneByHabit: Map<Long, Set<Long>>): Boolean {
        val scheduled = habits.filter { scheduled(it, day) }
        return scheduled.isNotEmpty() && scheduled.all { day in doneByHabit[it.id].orEmpty() }
    }
}

enum class Achievement {
    FIRST_STEP, ARCHITECT, EARLY_BIRD, NIGHT_OWL, WEEK_WARRIOR, IRON_WILL, MARATHON,
    UNBREAKABLE, PERFECTIONIST, CENTURION, HABIT_MACHINE, COMEBACK, RISING_STAR, LEGENDARY;

    companion object {
        fun of(key: String?): Achievement? = entries.firstOrNull { it.name == key }
    }
}

data class HabitStat(val current: Int, val best: Int, val rate30: Int)

/** Everything the UI needs, derived from the database in one pass. */
class Summary(
    val today: Long,
    val habits: List<Habit>,
    val entries: Map<Long, Map<Long, Entry>>,
    val doneByHabit: Map<Long, Set<Long>>,
    val active: Set<Long>,
    val frozen: Set<Long>,
    val bonusDays: Set<Long>,
    val totalXp: Int,
    val totalDone: Int,
    val streak: Int,
    val bestStreak: Int,
    val habitStats: Map<Long, HabitStat>,
    val comeback: Boolean,
) {
    val level: LevelInfo = Leveling.info(totalXp)
    val rank: Rank = Rank.forLevel(level.level)
    val perfectDays: Int get() = bonusDays.size

    fun scheduledOn(day: Long): List<Habit> = habits.filter { Streaks.scheduled(it, day) }
    fun entry(habitId: Long, day: Long): Entry? = entries[habitId]?.get(day)
    fun isDone(habitId: Long, day: Long): Boolean = day in doneByHabit[habitId].orEmpty()

    /** Share of scheduled habits done on [day], or -1 when nothing was scheduled. */
    fun ratio(day: Long): Float {
        val scheduled = scheduledOn(day)
        if (scheduled.isEmpty()) return -1f
        return scheduled.count { isDone(it.id, day) }.toFloat() / scheduled.size
    }
}

object Engine {
    fun anyScheduled(habits: List<Habit>): (Long) -> Boolean =
        { d -> habits.any { Streaks.scheduled(it, d) } }

    fun startDay(habits: List<Habit>, active: Set<Long>, today: Long): Long =
        minOf(habits.minOfOrNull { it.createdDay } ?: today, active.minOrNull() ?: today)

    fun summarize(
        habits: List<Habit>,
        entries: List<Entry>,
        bonuses: List<Bonus>,
        frozen: List<FrozenDay>,
        today: Long,
    ): Summary {
        val byHabit = entries.groupBy { it.habitId }.mapValues { (_, list) -> list.associateBy { it.day } }
        val doneByHabit = entries.filter { it.done }.groupBy { it.habitId }
            .mapValues { (_, list) -> list.mapTo(HashSet()) { it.day } }
        val active = entries.filter { it.done }.mapTo(HashSet()) { it.day }
        val frozenDays = frozen.mapTo(HashSet()) { it.day }
        val scheduledFn = anyScheduled(habits)
        val start = startDay(habits, active, today)

        val streak = Streaks.globalRun(
            active, frozenDays, scheduledFn, start, if (today in active) today else today - 1,
        )
        val best = Streaks.globalBest(active, frozenDays, scheduledFn, start, today)

        val stats = habits.associate { h ->
            val done = doneByHabit[h.id].orEmpty()
            h.id to HabitStat(
                current = Streaks.habitCurrent(h, done, frozenDays, today),
                best = Streaks.habitBest(h, done, frozenDays, today),
                rate30 = rate30(h, done, frozenDays, today),
            )
        }

        val sortedActive = active.sorted()
        val comeback = sortedActive.zipWithNext().any { (a, b) -> b - a >= 4 }

        return Summary(
            today = today,
            habits = habits,
            entries = byHabit,
            doneByHabit = doneByHabit,
            active = active,
            frozen = frozenDays,
            bonusDays = bonuses.mapTo(HashSet()) { it.day },
            totalXp = entries.sumOf { if (it.done) it.xp else 0 } + bonuses.sumOf { it.xp },
            totalDone = entries.count { it.done },
            streak = streak,
            bestStreak = best,
            habitStats = stats,
            comeback = comeback,
        )
    }

    private fun rate30(h: Habit, done: Set<Long>, frozen: Set<Long>, today: Long): Int {
        var scheduled = 0
        var hit = 0
        for (d in today - 29..today) {
            if (!Streaks.scheduled(h, d)) continue
            if (d == today && d !in done) continue
            scheduled++
            if (d in done || d in frozen) hit++
        }
        return if (scheduled == 0) 0 else hit * 100 / scheduled
    }

    fun achievements(s: Summary, habitsCreated: Int): Set<Achievement> {
        val best = { slot: Int ->
            s.habits.filter { it.slot == slot }.maxOfOrNull { s.habitStats[it.id]?.best ?: 0 } ?: 0
        }
        val bestHabit = s.habitStats.values.maxOfOrNull { it.best } ?: 0
        return buildSet {
            if (s.totalDone >= 1) add(Achievement.FIRST_STEP)
            if (habitsCreated >= 5) add(Achievement.ARCHITECT)
            if (best(Slot.MORNING) >= 7) add(Achievement.EARLY_BIRD)
            if (best(Slot.EVENING) >= 7) add(Achievement.NIGHT_OWL)
            if (s.bestStreak >= 7) add(Achievement.WEEK_WARRIOR)
            if (s.bestStreak >= 30) add(Achievement.IRON_WILL)
            if (s.bestStreak >= 100) add(Achievement.MARATHON)
            if (bestHabit >= 30) add(Achievement.UNBREAKABLE)
            if (s.perfectDays >= 7) add(Achievement.PERFECTIONIST)
            if (s.totalDone >= 100) add(Achievement.CENTURION)
            if (s.totalDone >= 1000) add(Achievement.HABIT_MACHINE)
            if (s.comeback) add(Achievement.COMEBACK)
            if (s.level.level >= 10) add(Achievement.RISING_STAR)
            if (s.level.level >= 50) add(Achievement.LEGENDARY)
        }
    }

    /**
     * Missed days (something scheduled, nothing done) between [lastProcessed] and
     * yesterday that should be covered by freezes. Freezes are only spent when they
     * can cover every missed day and there is a streak worth saving.
     */
    fun daysToFreeze(
        lastProcessed: Long,
        today: Long,
        freezes: Int,
        habits: List<Habit>,
        active: Set<Long>,
        frozen: Set<Long>,
    ): List<Long> {
        if (freezes <= 0 || habits.isEmpty()) return emptyList()
        val scheduledFn = anyScheduled(habits)
        val start = startDay(habits, active, today)
        val from = maxOf(lastProcessed + 1, start)
        val missed = (from until today).filter { scheduledFn(it) && it !in active && it !in frozen }
        if (missed.isEmpty() || missed.size > freezes) return emptyList()
        val before = Streaks.globalRun(active, frozen, scheduledFn, start, missed.first() - 1)
        return if (before > 0) missed else emptyList()
    }
}
