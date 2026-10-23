package io.github.samirwattar.happit.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

/**
 * Small settings and progress markers. The "seen"/"rewarded" markers stop
 * celebrations and rewards from repeating when a habit is unchecked and checked again.
 */
data class Settings(
    val name: String = "",
    val theme: Int = ThemeMode.SYSTEM,
    val title: String? = null,
    val freezes: Int = 1,
    val lastProcessed: Long = -1,
    val lastFreezeAwardDay: Long = -1,
    val maxRankRewarded: Int = 0,
    val maxLevelSeen: Int = 1,
    val seenUnlocks: Long = 0,
    val lastPerfectSeen: Long = -1,
    val habitsCreated: Int = 0,
    val lastUpdateCheck: Long = 0,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("happit", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Settings> = _state

    val value: Settings get() = _state.value

    @Synchronized
    fun update(change: Settings.() -> Settings) {
        val next = _state.value.change()
        save(next)
        _state.value = next
    }

    private fun load() = Settings(
        name = sp.getString("name", "") ?: "",
        theme = sp.getInt("theme", ThemeMode.SYSTEM),
        title = sp.getString("title", null),
        freezes = sp.getInt("freezes", 1),
        lastProcessed = sp.getLong("lastProcessed", -1),
        lastFreezeAwardDay = sp.getLong("lastFreezeAwardDay", -1),
        maxRankRewarded = sp.getInt("maxRankRewarded", 0),
        maxLevelSeen = sp.getInt("maxLevelSeen", 1),
        seenUnlocks = sp.getLong("seenUnlocks", 0),
        lastPerfectSeen = sp.getLong("lastPerfectSeen", -1),
        habitsCreated = sp.getInt("habitsCreated", 0),
        lastUpdateCheck = sp.getLong("lastUpdateCheck", 0),
    )

    private fun save(s: Settings) {
        sp.edit()
            .putString("name", s.name)
            .putInt("theme", s.theme)
            .putString("title", s.title)
            .putInt("freezes", s.freezes)
            .putLong("lastProcessed", s.lastProcessed)
            .putLong("lastFreezeAwardDay", s.lastFreezeAwardDay)
            .putInt("maxRankRewarded", s.maxRankRewarded)
            .putInt("maxLevelSeen", s.maxLevelSeen)
            .putLong("seenUnlocks", s.seenUnlocks)
            .putLong("lastPerfectSeen", s.lastPerfectSeen)
            .putInt("habitsCreated", s.habitsCreated)
            .putLong("lastUpdateCheck", s.lastUpdateCheck)
            .apply()
    }
}
