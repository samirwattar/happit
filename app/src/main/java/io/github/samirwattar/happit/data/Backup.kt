package io.github.samirwattar.happit.data

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON backup so it can be read and fixed by hand if ever needed. */
object Backup {
    private const val APP = "happit"
    private const val VERSION = 1

    class Data(
        val habits: List<Habit>,
        val entries: List<Entry>,
        val bonuses: List<Bonus>,
        val frozen: List<FrozenDay>,
        val unlocks: List<Unlock>,
        val name: String?,
        val title: String?,
        val freezes: Int?,
        val habitsCreated: Int?,
    )

    fun write(
        habits: List<Habit>,
        entries: List<Entry>,
        bonuses: List<Bonus>,
        frozen: List<FrozenDay>,
        unlocks: List<Unlock>,
        settings: Settings,
    ): String {
        val root = JSONObject()
        root.put("app", APP)
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("settings", JSONObject().apply {
            put("name", settings.name)
            put("title", settings.title ?: JSONObject.NULL)
            put("freezes", settings.freezes)
            put("habitsCreated", settings.habitsCreated)
        })
        root.put("habits", JSONArray().apply {
            habits.forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("icon", h.icon)
                    put("color", h.color)
                    put("type", h.type)
                    put("target", h.target)
                    put("unit", h.unit)
                    put("days", h.days)
                    put("slot", h.slot)
                    put("reminder", h.reminder)
                    put("createdDay", h.createdDay)
                    put("sortOrder", h.sortOrder)
                })
            }
        })
        root.put("entries", JSONArray().apply {
            entries.forEach { e ->
                put(JSONArray().put(e.habitId).put(e.day).put(e.value).put(if (e.done) 1 else 0).put(e.xp))
            }
        })
        root.put("bonuses", JSONArray().apply { bonuses.forEach { put(JSONArray().put(it.day).put(it.xp)) } })
        root.put("frozen", JSONArray().apply { frozen.forEach { put(it.day) } })
        root.put("unlocks", JSONArray().apply { unlocks.forEach { put(JSONArray().put(it.key).put(it.at)) } })
        return root.toString(2)
    }

    fun read(text: String): Data {
        val root = JSONObject(text)
        require(root.optString("app") == APP) { "Not a Happit backup" }
        val settings = root.optJSONObject("settings")
        val habits = root.getJSONArray("habits").objects().map { o ->
            Habit(
                id = o.getLong("id"),
                name = o.getString("name"),
                icon = o.optString("icon", "star"),
                color = o.optInt("color", 0),
                type = o.optInt("type", HabitType.CHECK),
                target = o.optInt("target", 1).coerceAtLeast(1),
                unit = o.optString("unit", ""),
                days = o.optInt("days", Habit.ALL_DAYS),
                slot = o.optInt("slot", Slot.ANYTIME),
                reminder = o.optInt("reminder", -1),
                createdDay = o.getLong("createdDay"),
                sortOrder = o.optInt("sortOrder", 0),
            )
        }
        val entries = root.getJSONArray("entries").arrays().map { a ->
            Entry(a.getLong(0), a.getLong(1), a.getInt(2), a.getInt(3) == 1, a.getInt(4))
        }
        val bonuses = root.optJSONArray("bonuses")?.arrays()?.map { Bonus(it.getLong(0), it.getInt(1)) }.orEmpty()
        val frozen = root.optJSONArray("frozen")?.let { arr -> (0 until arr.length()).map { FrozenDay(arr.getLong(it)) } }.orEmpty()
        val unlocks = root.optJSONArray("unlocks")?.arrays()?.map { Unlock(it.getString(0), it.getLong(1)) }.orEmpty()
        return Data(
            habits = habits,
            entries = entries,
            bonuses = bonuses,
            frozen = frozen,
            unlocks = unlocks,
            name = settings?.optString("name"),
            title = settings?.let { if (it.isNull("title")) null else it.optString("title") },
            freezes = settings?.optInt("freezes"),
            habitsCreated = settings?.optInt("habitsCreated"),
        )
    }

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.arrays() = (0 until length()).map { getJSONArray(it) }
}
