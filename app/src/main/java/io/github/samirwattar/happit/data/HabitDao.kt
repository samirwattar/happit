package io.github.samirwattar.happit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY slot, sortOrder, id")
    fun habits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY slot, sortOrder, id")
    suspend fun habitsNow(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun habit(id: Long): Habit?

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM entries")
    fun entries(): Flow<List<Entry>>

    @Query("SELECT * FROM entries")
    suspend fun entriesNow(): List<Entry>

    @Query("SELECT * FROM entries WHERE habitId = :habitId AND day = :day")
    suspend fun entry(habitId: Long, day: Long): Entry?

    @Upsert
    suspend fun upsert(entry: Entry)

    @Query("SELECT * FROM bonuses")
    fun bonuses(): Flow<List<Bonus>>

    @Query("SELECT * FROM bonuses")
    suspend fun bonusesNow(): List<Bonus>

    @Query("SELECT * FROM bonuses WHERE day = :day")
    suspend fun bonus(day: Long): Bonus?

    @Upsert
    suspend fun upsertBonus(bonus: Bonus)

    @Query("DELETE FROM bonuses WHERE day = :day")
    suspend fun deleteBonus(day: Long)

    @Query("SELECT * FROM frozen")
    fun frozen(): Flow<List<FrozenDay>>

    @Query("SELECT * FROM frozen")
    suspend fun frozenNow(): List<FrozenDay>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun freeze(day: FrozenDay)

    @Query("SELECT * FROM unlocks")
    fun unlocks(): Flow<List<Unlock>>

    @Query("SELECT * FROM unlocks")
    suspend fun unlocksNow(): List<Unlock>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(unlock: Unlock)

    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM bonuses") suspend fun clearBonuses()
    @Query("DELETE FROM frozen") suspend fun clearFrozen()
    @Query("DELETE FROM unlocks") suspend fun clearUnlocks()

    @Insert suspend fun insertHabits(habits: List<Habit>)
    @Insert suspend fun insertEntries(entries: List<Entry>)
    @Insert suspend fun insertBonuses(bonuses: List<Bonus>)
    @Insert suspend fun insertFrozen(days: List<FrozenDay>)
    @Insert suspend fun insertUnlocks(unlocks: List<Unlock>)

    @Transaction
    suspend fun replaceAll(
        habits: List<Habit>,
        entries: List<Entry>,
        bonuses: List<Bonus>,
        frozen: List<FrozenDay>,
        unlocks: List<Unlock>,
    ) {
        clearHabits(); clearEntries(); clearBonuses(); clearFrozen(); clearUnlocks()
        insertHabits(habits); insertEntries(entries); insertBonuses(bonuses)
        insertFrozen(frozen); insertUnlocks(unlocks)
    }
}
