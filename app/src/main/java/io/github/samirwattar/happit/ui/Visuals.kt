package io.github.samirwattar.happit.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.MobileOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.data.HabitType
import io.github.samirwattar.happit.data.Slot
import io.github.samirwattar.happit.logic.Achievement
import io.github.samirwattar.happit.logic.Rank
import io.github.samirwattar.happit.ui.theme.LocalPalette
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

val HabitIcons: List<Pair<String, ImageVector>> = listOf(
    "star" to Icons.Rounded.Star,
    "sun" to Icons.Rounded.WbSunny,
    "water" to Icons.Rounded.WaterDrop,
    "fitness" to Icons.Rounded.FitnessCenter,
    "book" to Icons.AutoMirrored.Rounded.MenuBook,
    "language" to Icons.Rounded.Translate,
    "meditate" to Icons.Rounded.SelfImprovement,
    "walk" to Icons.AutoMirrored.Rounded.DirectionsWalk,
    "run" to Icons.AutoMirrored.Rounded.DirectionsRun,
    "bike" to Icons.Rounded.PedalBike,
    "pool" to Icons.Rounded.Pool,
    "bed" to Icons.Rounded.Bedtime,
    "phone_off" to Icons.Rounded.MobileOff,
    "school" to Icons.Rounded.School,
    "pen" to Icons.Rounded.EditNote,
    "pill" to Icons.Rounded.Medication,
    "food" to Icons.Rounded.Restaurant,
    "spa" to Icons.Rounded.Spa,
    "heart" to Icons.Rounded.Favorite,
    "code" to Icons.Rounded.Code,
    "music" to Icons.Rounded.MusicNote,
    "brush" to Icons.Rounded.Brush,
    "money" to Icons.Rounded.Savings,
    "clean" to Icons.Rounded.CleaningServices,
    "smoke_free" to Icons.Rounded.SmokeFree,
    "coffee" to Icons.Rounded.LocalCafe,
    "nature" to Icons.Rounded.Park,
    "pets" to Icons.Rounded.Pets,
    "work" to Icons.Rounded.Work,
    "bolt" to Icons.Rounded.Bolt,
)

fun habitIcon(key: String): ImageVector = HabitIcons.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Star

class Tint(val lightBg: Color, val lightFg: Color, val darkBg: Color, val darkFg: Color)

private fun tint(lb: Long, lf: Long, db: Long, df: Long) = Tint(Color(lb), Color(lf), Color(db), Color(df))

val HabitTints = listOf(
    tint(0xFFE1F5EE, 0xFF0F6E56, 0xFF0B3D33, 0xFF5DCAA5), // teal
    tint(0xFFEEEDFE, 0xFF534AB7, 0xFF2D2760, 0xFFAFA9EC), // purple
    tint(0xFFE6F1FB, 0xFF185FA5, 0xFF0C2E4F, 0xFF85B7EB), // blue
    tint(0xFFFAEEDA, 0xFF854F0B, 0xFF3F2A0A, 0xFFFAC775), // amber
    tint(0xFFFAECE7, 0xFF993C1D, 0xFF45200F, 0xFFF0997B), // coral
    tint(0xFFFBEAF0, 0xFF993556, 0xFF461A2D, 0xFFED93B1), // pink
    tint(0xFFEAF3DE, 0xFF3B6D11, 0xFF213A0D, 0xFF97C459), // green
    tint(0xFFFCEBEB, 0xFFA32D2D, 0xFF4A1717, 0xFFF09595), // red
    tint(0xFFF1EFE8, 0xFF5F5E5A, 0xFF2C2A3A, 0xFFB4B2A9), // gray
)

/** Background and foreground for a habit color in the current theme. */
@Composable
@ReadOnlyComposable
fun tintColors(index: Int): Pair<Color, Color> {
    val t = HabitTints[index.mod(HabitTints.size)]
    return if (LocalPalette.current.isDark) t.darkBg to t.darkFg else t.lightBg to t.lightFg
}

val Rank.icon: ImageVector
    get() = when (this) {
        Rank.BEGINNER -> Icons.Rounded.Eco
        Rank.APPRENTICE -> Icons.Rounded.School
        Rank.COMMITTED -> Icons.Rounded.Bolt
        Rank.DISCIPLINED -> Icons.Rounded.Shield
        Rank.WARRIOR -> Icons.Rounded.LocalFireDepartment
        Rank.CHAMPION -> Icons.Rounded.EmojiEvents
        Rank.MASTER -> Icons.Rounded.MilitaryTech
        Rank.LEGEND -> Icons.Rounded.Diamond
    }

val Rank.tintIndex: Int
    get() = when (this) {
        Rank.BEGINNER -> 8
        Rank.APPRENTICE -> 6
        Rank.COMMITTED -> 0
        Rank.DISCIPLINED -> 2
        Rank.WARRIOR -> 4
        Rank.CHAMPION -> 3
        Rank.MASTER -> 1
        Rank.LEGEND -> 5
    }

@get:StringRes
val Rank.label: Int
    get() = when (this) {
        Rank.BEGINNER -> R.string.rank_beginner
        Rank.APPRENTICE -> R.string.rank_apprentice
        Rank.COMMITTED -> R.string.rank_committed
        Rank.DISCIPLINED -> R.string.rank_disciplined
        Rank.WARRIOR -> R.string.rank_warrior
        Rank.CHAMPION -> R.string.rank_champion
        Rank.MASTER -> R.string.rank_master
        Rank.LEGEND -> R.string.rank_legend
    }

val Achievement.icon: ImageVector
    get() = when (this) {
        Achievement.FIRST_STEP -> Icons.Rounded.Flag
        Achievement.ARCHITECT -> Icons.Rounded.Construction
        Achievement.EARLY_BIRD -> Icons.Rounded.WbSunny
        Achievement.NIGHT_OWL -> Icons.Rounded.Bedtime
        Achievement.WEEK_WARRIOR -> Icons.Rounded.LocalFireDepartment
        Achievement.IRON_WILL -> Icons.Rounded.FitnessCenter
        Achievement.MARATHON -> Icons.AutoMirrored.Rounded.DirectionsRun
        Achievement.UNBREAKABLE -> Icons.Rounded.Shield
        Achievement.PERFECTIONIST -> Icons.Rounded.AutoAwesome
        Achievement.CENTURION -> Icons.Rounded.Star
        Achievement.HABIT_MACHINE -> Icons.Rounded.AllInclusive
        Achievement.COMEBACK -> Icons.Rounded.Replay
        Achievement.RISING_STAR -> Icons.AutoMirrored.Rounded.TrendingUp
        Achievement.LEGENDARY -> Icons.Rounded.Diamond
    }

@get:StringRes
val Achievement.label: Int
    get() = when (this) {
        Achievement.FIRST_STEP -> R.string.ach_first_step
        Achievement.ARCHITECT -> R.string.ach_architect
        Achievement.EARLY_BIRD -> R.string.ach_early_bird
        Achievement.NIGHT_OWL -> R.string.ach_night_owl
        Achievement.WEEK_WARRIOR -> R.string.ach_week_warrior
        Achievement.IRON_WILL -> R.string.ach_iron_will
        Achievement.MARATHON -> R.string.ach_marathon
        Achievement.UNBREAKABLE -> R.string.ach_unbreakable
        Achievement.PERFECTIONIST -> R.string.ach_perfectionist
        Achievement.CENTURION -> R.string.ach_centurion
        Achievement.HABIT_MACHINE -> R.string.ach_habit_machine
        Achievement.COMEBACK -> R.string.ach_comeback
        Achievement.RISING_STAR -> R.string.ach_rising_star
        Achievement.LEGENDARY -> R.string.ach_legendary
    }

@get:StringRes
val Achievement.description: Int
    get() = when (this) {
        Achievement.FIRST_STEP -> R.string.ach_first_step_d
        Achievement.ARCHITECT -> R.string.ach_architect_d
        Achievement.EARLY_BIRD -> R.string.ach_early_bird_d
        Achievement.NIGHT_OWL -> R.string.ach_night_owl_d
        Achievement.WEEK_WARRIOR -> R.string.ach_week_warrior_d
        Achievement.IRON_WILL -> R.string.ach_iron_will_d
        Achievement.MARATHON -> R.string.ach_marathon_d
        Achievement.UNBREAKABLE -> R.string.ach_unbreakable_d
        Achievement.PERFECTIONIST -> R.string.ach_perfectionist_d
        Achievement.CENTURION -> R.string.ach_centurion_d
        Achievement.HABIT_MACHINE -> R.string.ach_habit_machine_d
        Achievement.COMEBACK -> R.string.ach_comeback_d
        Achievement.RISING_STAR -> R.string.ach_rising_star_d
        Achievement.LEGENDARY -> R.string.ach_legendary_d
    }

@StringRes
fun slotLabel(slot: Int): Int = when (slot) {
    Slot.MORNING -> R.string.slot_morning
    Slot.AFTERNOON -> R.string.slot_afternoon
    Slot.EVENING -> R.string.slot_evening
    else -> R.string.slot_anytime
}

class Template(
    @StringRes val name: Int,
    val icon: String,
    val color: Int,
    val type: Int,
    val target: Int,
    @StringRes val unit: Int?,
    val slot: Int,
)

val Templates = listOf(
    Template(R.string.tpl_water, "water", 2, HabitType.COUNT, 8, R.string.tpl_water_unit, Slot.ANYTIME),
    Template(R.string.tpl_read, "book", 0, HabitType.DURATION, 20, null, Slot.EVENING),
    Template(R.string.tpl_workout, "fitness", 4, HabitType.DURATION, 30, null, Slot.MORNING),
    Template(R.string.tpl_wake, "sun", 3, HabitType.CHECK, 1, null, Slot.MORNING),
    Template(R.string.tpl_meditate, "meditate", 1, HabitType.DURATION, 10, null, Slot.MORNING),
    Template(R.string.tpl_walk, "walk", 6, HabitType.DURATION, 30, null, Slot.AFTERNOON),
    Template(R.string.tpl_study, "school", 2, HabitType.DURATION, 45, null, Slot.AFTERNOON),
    Template(R.string.tpl_sleep, "bed", 1, HabitType.CHECK, 1, null, Slot.EVENING),
    Template(R.string.tpl_no_phone, "phone_off", 8, HabitType.CHECK, 1, null, Slot.EVENING),
    Template(R.string.tpl_journal, "pen", 5, HabitType.CHECK, 1, null, Slot.EVENING),
    Template(R.string.tpl_vitamins, "pill", 7, HabitType.CHECK, 1, null, Slot.MORNING),
)

@Composable
@ReadOnlyComposable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]

/** Formats a number with the app language's digits, matching string resources. */
fun Int.fmt(locale: Locale): String = String.format(locale, "%d", this)

/** Weekdays in the order the locale expects (Saturday first in many Arabic locales). */
fun orderedWeek(locale: Locale): List<DayOfWeek> {
    val first = WeekFields.of(locale).firstDayOfWeek
    return (0L until 7L).map { first.plus(it) }
}
