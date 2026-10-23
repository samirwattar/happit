package io.github.samirwattar.happit.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.samirwattar.happit.R

@Immutable
data class Palette(
    val bg: Color,
    val sheet: Color,
    val card: Color,
    val divider: Color,
    val track: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentText: Color,
    val onAccent: Color,
    val flame: Color,
    val gold: Color,
    val goldSoft: Color,
    val ice: Color,
    val danger: Color,
    val scrim: Color,
    val isDark: Boolean,
)

/** The modern look: white, warm grays and a teal accent. */
val LightPalette = Palette(
    bg = Color(0xFFFFFFFF),
    sheet = Color(0xFFFFFFFF),
    card = Color(0xFFF7F6F2),
    divider = Color(0xFFF1EFE8),
    track = Color(0xFFE4E2DA),
    text = Color(0xFF2C2C2A),
    text2 = Color(0xFF888780),
    text3 = Color(0xFFB4B2A9),
    accent = Color(0xFF1D9E75),
    accentSoft = Color(0xFFE1F5EE),
    accentText = Color(0xFF0F6E56),
    onAccent = Color(0xFFFFFFFF),
    flame = Color(0xFFD85A30),
    gold = Color(0xFFBA7517),
    goldSoft = Color(0xFFFAEEDA),
    ice = Color(0xFF378ADD),
    danger = Color(0xFFA32D2D),
    scrim = Color(0x592C2C2A),
    isDark = false,
)

/** Dark mode: deep purple surfaces with a violet accent. */
val DarkPalette = Palette(
    bg = Color(0xFF110E22),
    sheet = Color(0xFF17132D),
    card = Color(0xFF211C3E),
    divider = Color(0xFF2A2450),
    track = Color(0xFF332C5C),
    text = Color(0xFFEEEDFE),
    text2 = Color(0xFFA39DCF),
    text3 = Color(0xFF6E6896),
    accent = Color(0xFF8F86F0),
    accentSoft = Color(0xFF2D2760),
    accentText = Color(0xFFCECBF6),
    onAccent = Color(0xFFFFFFFF),
    flame = Color(0xFFF0997B),
    gold = Color(0xFFEF9F27),
    goldSoft = Color(0xFF3F2A0A),
    ice = Color(0xFF85B7EB),
    danger = Color(0xFFF09595),
    scrim = Color(0x99000000),
    isDark = true,
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

val PlexArabic = FontFamily(
    Font(R.font.plex_arabic_regular, FontWeight.Normal),
    Font(R.font.plex_arabic_medium, FontWeight.Medium),
    Font(R.font.plex_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.plex_arabic_semibold, FontWeight.Bold),
)

private fun typography(f: FontFamily): Typography {
    val t = Typography()
    return Typography(
        displayLarge = t.displayLarge.copy(fontFamily = f),
        displayMedium = t.displayMedium.copy(fontFamily = f),
        displaySmall = t.displaySmall.copy(fontFamily = f),
        headlineLarge = t.headlineLarge.copy(fontFamily = f),
        headlineMedium = t.headlineMedium.copy(fontFamily = f),
        headlineSmall = t.headlineSmall.copy(fontFamily = f),
        titleLarge = t.titleLarge.copy(fontFamily = f),
        titleMedium = t.titleMedium.copy(fontFamily = f),
        titleSmall = t.titleSmall.copy(fontFamily = f),
        bodyLarge = t.bodyLarge.copy(fontFamily = f),
        bodyMedium = t.bodyMedium.copy(fontFamily = f),
        bodySmall = t.bodySmall.copy(fontFamily = f),
        labelLarge = t.labelLarge.copy(fontFamily = f),
        labelMedium = t.labelMedium.copy(fontFamily = f),
        labelSmall = t.labelSmall.copy(fontFamily = f),
    )
}

@Composable
fun HappitTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    val family = if (arabic) PlexArabic else Inter
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            primaryContainer = p.accentSoft, onPrimaryContainer = p.accentText,
            secondary = p.accent, onSecondary = p.onAccent,
            secondaryContainer = p.accentSoft, onSecondaryContainer = p.accentText,
            tertiary = p.gold,
            background = p.bg, onBackground = p.text,
            surface = p.sheet, onSurface = p.text,
            surfaceVariant = p.card, onSurfaceVariant = p.text2,
            surfaceContainerLowest = p.bg, surfaceContainerLow = p.sheet,
            surfaceContainer = p.sheet, surfaceContainerHigh = p.sheet,
            surfaceContainerHighest = p.card,
            outline = p.track, outlineVariant = p.divider,
            error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent,
            primaryContainer = p.accentSoft, onPrimaryContainer = p.accentText,
            secondary = p.accent, onSecondary = p.onAccent,
            secondaryContainer = p.accentSoft, onSecondaryContainer = p.accentText,
            tertiary = p.gold,
            background = p.bg, onBackground = p.text,
            surface = p.sheet, onSurface = p.text,
            surfaceVariant = p.card, onSurfaceVariant = p.text2,
            surfaceContainerLowest = p.bg, surfaceContainerLow = p.sheet,
            surfaceContainer = p.sheet, surfaceContainerHigh = p.sheet,
            surfaceContainerHighest = p.card,
            outline = p.track, outlineVariant = p.divider,
            error = p.danger,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typography(family)) {
        CompositionLocalProvider(
            LocalPalette provides p,
            LocalContentColor provides p.text,
            LocalTextStyle provides TextStyle(fontFamily = family, fontSize = 15.sp),
            content = content,
        )
    }
}
