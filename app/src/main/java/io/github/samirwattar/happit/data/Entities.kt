package io.github.samirwattar.happit.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object HabitType {
    const val CHECK = 0
    const val COUNT = 1
    const val DURATION = 2
}

object Slot {
    const val MORNING = 0
    const val AFTERNOON = 1
    const val EVENING = 2
    const val ANYTIME = 3
}

/**
 * A habit the user tracks. [days] is a Monday-first bitmask (bit 0 = Monday),
 * [target] is 1 for check habits, a count for count habits and minutes for
 * duration habits. [reminder] is minutes after midnight, or -1 for none.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Int,
    val type: Int = HabitType.CHECK,
    val target: Int = 1,
    val unit: String = "",
    val days: Int = ALL_DAYS,
    val slot: Int = Slot.ANYTIME,
    val reminder: Int = -1,
    val createdDay: Long,
    val sortOrder: Int = 0,
) {
    companion object {
        const val ALL_DAYS = 0b1111111
    }
}

/** Progress for one habit on one day. Entries outlive deleted habits so earned XP stays. */
@Entity(tableName = "entries", primaryKeys = ["habitId", "day"], indices = [Index("day")])
data class Entry(
    val habitId: Long,
    val day: Long,
    val value: Int,
    val done: Boolean,
    val xp: Int,
)

/** Perfect-day bonus XP. */
@Entity(tableName = "bonuses")
data class Bonus(@PrimaryKey val day: Long, val xp: Int)

/** A missed day covered by a streak freeze. */
@Entity(tableName = "frozen")
data class FrozenDay(@PrimaryKey val day: Long)

@Entity(tableName = "unlocks")
data class Unlock(@PrimaryKey val key: String, val at: Long)
