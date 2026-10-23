package io.github.samirwattar.happit.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.samirwattar.happit.BuildConfig
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.Settings
import io.github.samirwattar.happit.logic.Achievement
import io.github.samirwattar.happit.logic.Days
import io.github.samirwattar.happit.logic.Rank
import io.github.samirwattar.happit.logic.Summary
import io.github.samirwattar.happit.repo
import io.github.samirwattar.happit.update.Release
import io.github.samirwattar.happit.update.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Celebration {
    data class LevelUp(val level: Int, val rank: Rank?) : Celebration
    data object PerfectDay : Celebration
    data class Unlocked(val achievement: Achievement) : Celebration
    data object FreezeEarned : Celebration
}

data class XpPop(val habitId: Long, val xp: Int, val token: Long)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val percent: Int) : UpdateState
    data class Ready(val release: Release, val file: File) : UpdateState
    data class Failed(@StringRes val message: Int) : UpdateState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.repo
    private val today = MutableStateFlow(Days.today())

    val settings: StateFlow<Settings> = repo.prefs.state
    val selectedDay = MutableStateFlow(today.value)
    val summary: StateFlow<Summary?> =
        repo.summaries(today).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val unlocks: StateFlow<Map<String, Long>> = repo.unlocks
        .map { list -> list.associate { it.key to it.at } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val celebrations = mutableStateListOf<Celebration>()
    var xpPop by mutableStateOf<XpPop?>(null)
        private set
    val update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    var showUpdateDialog by mutableStateOf(false)
    var message by mutableStateOf<Int?>(null)

    init {
        viewModelScope.launch {
            repo.processFreezes()
            combine(summary.filterNotNull(), unlocks) { s, u -> s to u }
                .collect { (s, u) -> detectCelebrations(s, u) }
        }
        viewModelScope.launch {
            var last = settings.value
            settings.collect { st ->
                val streakFreeze = st.freezes > last.freezes &&
                    st.lastFreezeAwardDay != last.lastFreezeAwardDay &&
                    st.maxRankRewarded == last.maxRankRewarded
                if (streakFreeze) celebrate(Celebration.FreezeEarned)
                last = st
            }
        }
        if (autoUpdateCheck) autoCheckForUpdates()
    }

    private fun celebrate(c: Celebration) {
        if (c !in celebrations) celebrations += c
    }

    /** Celebrations come from saved progress, so they also fire for changes made from a notification. */
    private fun detectCelebrations(s: Summary, unlocked: Map<String, Long>) {
        val st = repo.prefs.value
        if (s.today in s.bonusDays && st.lastPerfectSeen != s.today) {
            celebrate(Celebration.PerfectDay)
            repo.prefs.update { copy(lastPerfectSeen = s.today) }
        }
        if (s.level.level > st.maxLevelSeen) {
            val oldRank = Rank.forLevel(st.maxLevelSeen)
            celebrate(Celebration.LevelUp(s.level.level, s.rank.takeIf { it != oldRank }))
            repo.prefs.update { copy(maxLevelSeen = s.level.level) }
        }
        val fresh = unlocked.filter { it.value > st.seenUnlocks }.entries.sortedBy { it.value }
        if (fresh.isNotEmpty()) {
            fresh.forEach { e -> Achievement.of(e.key)?.let { celebrate(Celebration.Unlocked(it)) } }
            repo.prefs.update { copy(seenUnlocks = fresh.maxOf { it.value }) }
        }
    }

    fun dismissCelebration() {
        if (celebrations.isNotEmpty()) celebrations.removeAt(0)
    }

    fun onResume() {
        val t = Days.today()
        if (t != today.value) {
            val wasToday = selectedDay.value == today.value
            today.value = t
            if (wasToday) selectedDay.value = t
            viewModelScope.launch { repo.processFreezes(t) }
        }
    }

    fun selectDay(day: Long) {
        selectedDay.value = day.coerceAtMost(today.value)
    }

    private fun showGain(habit: Habit, gained: Int) {
        if (gained > 0) xpPop = XpPop(habit.id, gained, System.nanoTime())
    }

    fun setProgress(habit: Habit, day: Long, value: Int) {
        if (day > today.value) return
        viewModelScope.launch { showGain(habit, repo.setProgress(habit, day, value)) }
    }

    fun addProgress(habit: Habit, day: Long, delta: Int) {
        if (day > today.value) return
        viewModelScope.launch { showGain(habit, repo.addProgress(habit, day, delta)) }
    }

    fun toggle(habit: Habit, day: Long) {
        val done = summary.value?.isDone(habit.id, day) == true
        setProgress(habit, day, if (done) 0 else habit.target)
    }

    fun saveHabit(habit: Habit) {
        viewModelScope.launch { repo.saveHabit(habit) }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { repo.deleteHabit(habit) }
    }

    fun setName(name: String) = repo.prefs.update { copy(name = name.trim()) }
    fun setTheme(mode: Int) = repo.prefs.update { copy(theme = mode) }
    fun equipTitle(key: String?) = repo.prefs.update { copy(title = key) }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val json = repo.exportJson()
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!
                        .use { it.write(json.toByteArray()) }
                }
            }.isSuccess
            message = if (ok) R.string.export_done else R.string.export_failed
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)!!
                        .use { it.bufferedReader().readText() }
                }
                repo.importJson(text)
            }.isSuccess
            message = if (ok) R.string.import_done else R.string.import_failed
        }
    }

    private fun autoCheckForUpdates() {
        val elapsed = System.currentTimeMillis() - settings.value.lastUpdateCheck
        if (elapsed >= 12 * 60 * 60 * 1000L) checkForUpdates(manual = false)
    }

    fun checkForUpdates(manual: Boolean = true) {
        if (update.value is UpdateState.Checking || update.value is UpdateState.Downloading) return
        viewModelScope.launch {
            update.value = UpdateState.Checking
            val result = runCatching { Updater.latest() }
            repo.prefs.update { copy(lastUpdateCheck = System.currentTimeMillis()) }
            update.value = result.fold(
                onSuccess = { r ->
                    if (r != null && Updater.isNewer(r.version, BuildConfig.VERSION_NAME)) {
                        if (!manual) showUpdateDialog = true
                        UpdateState.Available(r)
                    } else if (manual) {
                        UpdateState.UpToDate
                    } else {
                        UpdateState.Idle
                    }
                },
                onFailure = { if (manual) UpdateState.Failed(R.string.update_error) else UpdateState.Idle },
            )
        }
    }

    fun downloadUpdate(release: Release) {
        if (update.value is UpdateState.Downloading) return
        viewModelScope.launch {
            update.value = UpdateState.Downloading(release, 0)
            runCatching {
                Updater.download(getApplication(), release) { p ->
                    update.value = UpdateState.Downloading(release, p)
                }
            }.fold(
                onSuccess = { file ->
                    update.value = UpdateState.Ready(release, file)
                    installUpdate()
                },
                onFailure = { update.value = UpdateState.Failed(R.string.download_error) },
            )
        }
    }

    fun installUpdate() {
        val ready = update.value as? UpdateState.Ready ?: return
        val ctx = getApplication<Application>()
        if (!Updater.canInstall(ctx)) {
            message = R.string.allow_installs
            Updater.openInstallPermission(ctx)
            return
        }
        Updater.install(ctx, ready.file)
    }

    companion object {
        /** Tests switch this off so they never touch the network. */
        var autoUpdateCheck = true
    }
}
