package io.github.samirwattar.happit

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.samirwattar.happit.data.Backup
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.logic.Days
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest : UiTestBase() {
    @Test
    fun addHabitAndCompleteIt() = launch {
        waitForText("Build your routine")
        shot("01-empty")
        compose.onNodeWithText("Add your first habit").performClick()
        waitForText("Quick start")
        compose.onNodeWithText("Wake up early").performClick()
        shot("02-editor")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasContentDescription("Wake up early, Mark as done")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Wake up early, Mark as done").performClick()
        // One habit done = +10 XP and a perfect day (+50), celebrated with a sheet.
        waitForText("Perfect day")
        shot("03-perfect-day")
        val s = runBlocking { app.repo.summaryNow() }
        assertEquals(1, s.totalDone)
        assertEquals(1, s.perfectDays)
        assertEquals(60, s.totalXp)
    }
}

@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
@Config(sdk = [34])
class BackupTest : UiTestBase() {
    @Test
    fun backupRoundTrip() = runBlocking {
        val repo = app.repo
        val h = repo.saveHabit(Habit(name = "Read", icon = "book", color = 0, createdDay = Days.today()))
        repo.setProgress(h, Days.today(), 1)
        val json = repo.exportJson()
        val data = Backup.read(json)
        assertEquals(1, data.habits.size)
        assertEquals(1, data.entries.size)
        repo.importJson(json)
        assertEquals(60, repo.summaryNow().totalXp)
        var failed = false
        try {
            repo.importJson("{\"app\":\"other\"}")
        } catch (e: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class LightScreensTest : UiTestBase() {
    @Test
    fun screenshots() {
        seed(arabic = false)
        app.repo.prefs.update { copy(theme = io.github.samirwattar.happit.data.ThemeMode.LIGHT) }
        tour("en-light", arabic = false)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class DarkScreensTest : UiTestBase() {
    @Test
    fun screenshots() {
        seed(arabic = false)
        app.repo.prefs.update { copy(theme = io.github.samirwattar.happit.data.ThemeMode.DARK) }
        tour("en-dark", arabic = false)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ar-w411dp-h891dp-xxhdpi")
class ArabicScreensTest : UiTestBase() {
    @Test
    fun screenshots() {
        seed(arabic = true)
        app.repo.prefs.update { copy(theme = io.github.samirwattar.happit.data.ThemeMode.DARK) }
        tour("ar-dark", arabic = true)
    }
}
