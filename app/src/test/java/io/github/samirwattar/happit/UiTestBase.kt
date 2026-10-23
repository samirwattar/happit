package io.github.samirwattar.happit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.HabitType
import io.github.samirwattar.happit.data.Slot
import io.github.samirwattar.happit.logic.Days
import io.github.samirwattar.happit.ui.AppViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Shared helpers for Robolectric UI tests. Each UI test class runs in its own JVM
 * (forkEvery = 1 in build.gradle.kts) because Compose test roots leak between
 * Robolectric tests in the same process.
 */
@OptIn(ExperimentalTestApi::class)
abstract class UiTestBase {
    @get:Rule
    val compose = createEmptyComposeRule()

    protected val app: HappitApp get() = ApplicationProvider.getApplicationContext()

    @Before
    fun disableNetwork() {
        AppViewModel.autoUpdateCheck = false
    }

    protected fun waitForText(text: String) {
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            shot("failed-waiting")
            throw e
        }
    }

    protected fun launch(block: () -> Unit) {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            block()
        } finally {
            scenario.moveToState(Lifecycle.State.DESTROYED)
            ShadowLooper.idleMainLooper()
        }
    }

    /** Draws every window (activity, sheets, dialogs) into one PNG under app/build/screens. */
    protected fun shot(name: String) {
        compose.waitForIdle()
        ShadowLooper.idleMainLooper()
        val global = Class.forName("android.view.WindowManagerGlobal")
        val instance = global.getMethod("getInstance").invoke(null)
        val field = global.getDeclaredField("mViews").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val views = (field.get(instance) as List<View>).filter { it.isShown && it.width > 0 }
        val base = views.first()
        val bmp = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        views.forEach { v ->
            val loc = IntArray(2)
            v.getLocationOnScreen(loc)
            canvas.save()
            canvas.translate(loc[0].toFloat(), loc[1].toFloat())
            v.draw(canvas)
            canvas.restore()
        }
        val dir = File("build/screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Six habits with 40 days of mostly-complete history and a half-done today. */
    protected fun seed(arabic: Boolean) = runBlocking {
        val repo = app.repo
        val today = Days.today()
        val start = today - 40
        fun n(en: String, ar: String) = if (arabic) ar else en
        val habits = listOf(
            Habit(name = n("Wake up at 6:00", "الاستيقاظ الساعة 6:00"), icon = "sun", color = 3, slot = Slot.MORNING, reminder = 6 * 60, createdDay = start),
            Habit(name = n("Drink water", "شرب الماء"), icon = "water", color = 2, type = HabitType.COUNT, target = 8, unit = n("glasses", "أكواب"), slot = Slot.MORNING, createdDay = start),
            Habit(name = n("Workout", "تمرين"), icon = "fitness", color = 4, type = HabitType.DURATION, target = 30, slot = Slot.MORNING, createdDay = start),
            Habit(name = n("Study Russian", "دراسة الروسية"), icon = "language", color = 1, slot = Slot.AFTERNOON, createdDay = start),
            Habit(name = n("Read 20 pages", "قراءة 20 صفحة"), icon = "book", color = 0, slot = Slot.EVENING, createdDay = start),
            Habit(name = n("No phone after 11 PM", "بدون هاتف بعد 11 مساءً"), icon = "phone_off", color = 8, slot = Slot.EVENING, createdDay = start),
        ).map { repo.saveHabit(it) }
        for (d in start until today) {
            habits.forEachIndexed { i, h ->
                if ((d + i) % 7 != 0L && (d - start) > 3) repo.setProgress(h, d, h.target)
            }
        }
        repo.setProgress(habits[0], today, 1)
        repo.setProgress(habits[1], today, 6)
        repo.setProgress(habits[2], today, 12)
        repo.setProgress(habits[3], today, 1)
        repo.prefs.update {
            copy(
                name = n("Alex", "سامي"),
                maxLevelSeen = 999,
                seenUnlocks = Long.MAX_VALUE,
                lastPerfectSeen = today,
                lastProcessed = today - 1,
            )
        }
    }

    /** Visits every screen and saves a screenshot of each. */
    protected fun tour(prefix: String, arabic: Boolean) {
        fun t(en: String, ar: String) = if (arabic) ar else en
        launch {
            waitForText(t("Drink water", "شرب الماء"))
            shot("$prefix-today")
            compose.onNodeWithText(t("Drink water", "شرب الماء")).performClick()
            waitForText(t("Last 30 days", "آخر 30 يومًا"))
            shot("$prefix-sheet")
            compose.onNodeWithText(t("Edit", "تعديل")).performClick()
            waitForText(t("Edit habit", "تعديل العادة"))
            shot("$prefix-edit")
            compose.onNodeWithContentDescription(t("Back", "رجوع")).performClick()
            compose.onNodeWithText(t("Stats", "الإحصاء")).performClick()
            waitForText(t("Perfect days", "أيام مثالية"))
            shot("$prefix-stats")
            compose.onNodeWithText(t("Ranks", "الرتب")).performClick()
            waitForText(t("Beginner", "مبتدئ"))
            shot("$prefix-ranks")
            compose.onNodeWithText(t("Profile", "الملف")).performClick()
            waitForText(t("Streak freezes", "تجميد السلسلة"))
            shot("$prefix-profile")
        }
    }
}
