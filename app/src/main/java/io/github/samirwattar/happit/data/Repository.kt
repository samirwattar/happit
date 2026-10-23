package io.github.samirwattar.happit.data

import android.content.Context
import io.github.samirwattar.happit.logic.Days
import io.github.samirwattar.happit.logic.Engine
import io.github.samirwattar.happit.logic.Streaks
import io.github.samirwattar.happit.logic.Summary
import io.github.samirwattar.happit.logic.Xp
import io.github.samirwattar.happit.reminders.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Repository(private val context: Context, db: AppDatabase, val prefs: Prefs) {
    private val dao = db.dao()
    private val mutex = Mutex()

    val unlocks: Flow<List<Unlock>> = dao.unlocks()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun summaries(today: Flow<Long>): Flow<Summary> = today.flatMapLatest { t ->
        combine(dao.habits(), dao.entries(), dao.bonuses(), dao.frozen()) { h, e, b, f ->
            Engine.summarize(h, e, b, f, t)
        }
    }.flowOn(Dispatchers.Default)

    suspend fun habit(id: Long): Habit? = dao.habit(id)
    suspend fun habits(): List<Habit> = dao.habitsNow()

    suspend fun summaryNow(today: Long = Days.today()): Summary =
        Engine.summarize(dao.habitsNow(), dao.entriesNow(), dao.bonusesNow(), dao.frozenNow(), today)

    /**
     * Sets a habit's progress on [day] and applies bonuses, rewards and unlocks.
     * Returns the XP gained, negative when a completion is undone.
     */
    suspend fun setProgress(habit: Habit, day: Long, value: Int): Int = mutex.withLock {
        setProgressLocked(habit, day, value)
    }

    /** Adds [delta] to the stored progress, read under the same lock so fast taps don't race. */
    suspend fun addProgress(habit: Habit, day: Long, delta: Int): Int = mutex.withLock {
        val current = dao.entry(habit.id, day)?.value ?: 0
        setProgressLocked(habit, day, current + delta)
    }

    private suspend fun setProgressLocked(habit: Habit, day: Long, value: Int): Int {
        val v = value.coerceAtLeast(0)
        val done = v >= habit.target
        val prev = dao.entry(habit.id, day)
        val prevXp = if (prev?.done == true) prev.xp else 0
        val xp = when {
            !done -> 0
            prev?.done == true -> prev.xp
            else -> {
                val doneDays = dao.entriesNow()
                    .filter { it.habitId == habit.id && it.done }
                    .mapTo(HashSet()) { it.day }
                    .apply { add(day) }
                val frozen = dao.frozenNow().mapTo(HashSet()) { it.day }
                Xp.forCompletion(Streaks.habitRun(habit, doneDays, frozen, day))
            }
        }
        dao.upsert(Entry(habit.id, day, v, done, xp))
        val bonusBefore = dao.bonus(day)?.xp ?: 0
        val unchecked = prev?.done == true && !done
        val bonusAfter = afterChangeLocked(day, unchecked)
        return xp - prevXp + bonusAfter - bonusBefore
    }

    /**
     * A perfect-day bonus is added when every scheduled habit is done, and only
     * taken back when a habit is unchecked (adding a new habit never costs XP).
     */
    private suspend fun afterChangeLocked(day: Long, unchecked: Boolean): Int {
        val today = Days.today()
        val habits = dao.habitsNow()
        val entries = dao.entriesNow()
        val doneBy = entries.filter { it.done }.groupBy { it.habitId }
            .mapValues { (_, list) -> list.mapTo(HashSet()) { it.day } }
        if (Streaks.isPerfect(day, habits, doneBy)) {
            dao.upsertBonus(Bonus(day, Xp.PERFECT_DAY))
        } else if (unchecked) {
            dao.deleteBonus(day)
        }
        val summary = Engine.summarize(habits, entries, dao.bonusesNow(), dao.frozenNow(), today)
        rewardAndUnlock(summary, today)
        return dao.bonus(day)?.xp ?: 0
    }

    private suspend fun rewardAndUnlock(s: Summary, today: Long) {
        val st = prefs.value
        var freezes = st.freezes
        var awardDay = st.lastFreezeAwardDay
        if (today in s.active && s.streak > 0 && s.streak % 7 == 0 && awardDay != today) {
            freezes++
            awardDay = today
        }
        var maxRank = st.maxRankRewarded
        if (s.rank.ordinal > maxRank) {
            freezes += s.rank.ordinal - maxRank
            maxRank = s.rank.ordinal
        }
        freezes = freezes.coerceAtMost(Xp.MAX_FREEZES)
        if (freezes != st.freezes || awardDay != st.lastFreezeAwardDay || maxRank != st.maxRankRewarded) {
            prefs.update { copy(freezes = freezes, lastFreezeAwardDay = awardDay, maxRankRewarded = maxRank) }
        }
        val have = dao.unlocksNow().mapTo(HashSet()) { it.key }
        val now = System.currentTimeMillis()
        Engine.achievements(s, prefs.value.habitsCreated)
            .filter { it.name !in have }
            .forEachIndexed { i, a -> dao.unlock(Unlock(a.name, now + i)) }
    }

    /** Spends streak freezes on days missed since the app last ran. */
    suspend fun processFreezes(today: Long = Days.today()) = mutex.withLock {
        val st = prefs.value
        if (st.lastProcessed < 0) {
            prefs.update { copy(lastProcessed = today - 1) }
            return@withLock
        }
        if (st.lastProcessed >= today - 1) return@withLock
        val habits = dao.habitsNow()
        val active = dao.entriesNow().filter { it.done }.mapTo(HashSet()) { it.day }
        val frozen = dao.frozenNow().mapTo(HashSet()) { it.day }
        val days = Engine.daysToFreeze(st.lastProcessed, today, st.freezes, habits, active, frozen)
        days.forEach { dao.freeze(FrozenDay(it)) }
        prefs.update { copy(lastProcessed = today - 1, freezes = freezes - days.size) }
    }

    suspend fun saveHabit(habit: Habit): Habit {
        val saved = if (habit.id == 0L) {
            val id = dao.insert(habit)
            prefs.update { copy(habitsCreated = habitsCreated + 1) }
            habit.copy(id = id)
        } else {
            dao.update(habit)
            habit
        }
        Reminders.schedule(context, saved)
        mutex.withLock { afterChangeLocked(Days.today(), unchecked = false) }
        return saved
    }

    suspend fun deleteHabit(habit: Habit) {
        Reminders.cancel(context, habit.id)
        dao.delete(habit.id)
    }

    suspend fun exportJson(): String = Backup.write(
        habits = dao.habitsNow(),
        entries = dao.entriesNow(),
        bonuses = dao.bonusesNow(),
        frozen = dao.frozenNow(),
        unlocks = dao.unlocksNow(),
        settings = prefs.value,
    )

    /** Replaces everything with the backup. Throws if the text isn't a Happit backup. */
    suspend fun importJson(text: String) {
        val data = Backup.read(text)
        dao.habitsNow().forEach { Reminders.cancel(context, it.id) }
        mutex.withLock {
            dao.replaceAll(data.habits, data.entries, data.bonuses, data.frozen, data.unlocks)
        }
        val today = Days.today()
        val s = summaryNow(today)
        prefs.update {
            copy(
                name = data.name ?: name,
                title = data.title,
                freezes = data.freezes ?: freezes,
                habitsCreated = maxOf(data.habitsCreated ?: 0, data.habits.size),
                lastProcessed = today - 1,
                lastFreezeAwardDay = today,
                maxRankRewarded = s.rank.ordinal,
                maxLevelSeen = s.level.level,
                seenUnlocks = data.unlocks.maxOfOrNull { it.at } ?: 0,
                lastPerfectSeen = today,
            )
        }
        data.habits.forEach { Reminders.schedule(context, it) }
    }
}
