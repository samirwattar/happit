package io.github.samirwattar.happit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.logic.Days
import io.github.samirwattar.happit.logic.Summary
import io.github.samirwattar.happit.ui.theme.LocalPalette
import java.time.temporal.WeekFields
import java.time.format.TextStyle as DateTextStyle

private const val HEATMAP_WEEKS = 17

@Composable
fun StatsScreen(s: Summary) {
    val c = LocalPalette.current
    val locale = appLocale()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.stats_title)) }
        if (s.habits.isEmpty() && s.totalDone == 0) {
            item { EmptyHint(Icons.Rounded.Insights, stringResource(R.string.no_stats)) }
            return@LazyColumn
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        stringResource(R.string.current_streak),
                        pluralStringResource(R.plurals.days, s.streak, s.streak),
                        Icons.Rounded.LocalFireDepartment, c.flame, Modifier.weight(1f),
                    )
                    MetricCard(
                        stringResource(R.string.best_streak),
                        pluralStringResource(R.plurals.days, s.bestStreak, s.bestStreak),
                        Icons.Rounded.EmojiEvents, c.gold, Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        stringResource(R.string.perfect_days),
                        s.perfectDays.fmt(locale),
                        Icons.Rounded.AutoAwesome, c.accent, Modifier.weight(1f),
                    )
                    MetricCard(
                        stringResource(R.string.total_done),
                        s.totalDone.fmt(locale),
                        Icons.Rounded.CheckCircle, c.accent, Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Panel(Modifier.padding(top = 14.dp)) {
                Text(stringResource(R.string.this_week), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(14.dp))
                WeekBars(s)
            }
        }
        item {
            Panel(Modifier.padding(top = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.activity),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Legend()
                }
                Spacer(Modifier.height(14.dp))
                Heatmap(s)
            }
        }
        if (s.habits.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.per_habit)) }
            items(s.habits, key = { it.id }) { h ->
                val stat = s.habitStats[h.id]
                val (bg, fg) = tintColors(h.color)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(habitIcon(h.icon), bg, fg, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.habit_stats, stat?.current ?: 0, stat?.best ?: 0),
                            fontSize = 12.sp,
                            color = c.text2,
                        )
                    }
                    Column(Modifier.width(64.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.rate_30, stat?.rate30 ?: 0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        XpBar((stat?.rate30 ?: 0) / 100f, Modifier.padding(top = 4.dp), height = 5.dp, color = fg)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekBars(s: Summary) {
    val c = LocalPalette.current
    val locale = appLocale()
    val style = if (locale.language == "ar") DateTextStyle.NARROW else DateTextStyle.SHORT
    Row(Modifier.fillMaxWidth().height(150.dp), verticalAlignment = Alignment.Bottom) {
        for (d in s.today - 6..s.today) {
            val ratio = s.ratio(d)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (ratio < 0) "–" else stringResource(R.string.rate_30, (ratio * 100).toInt()),
                    fontSize = 10.sp,
                    color = c.text2,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(22.dp)
                        .height((100 * ratio.coerceAtLeast(0f)).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                ratio < 0 -> c.divider
                                ratio >= 1f -> c.accent
                                else -> c.accent.copy(alpha = 0.5f)
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    Days.date(d).dayOfWeek.getDisplayName(style, locale),
                    fontSize = 11.sp,
                    color = if (d == s.today) c.text else c.text3,
                    fontWeight = if (d == s.today) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun heatColor(ratio: Float, frozen: Boolean): Color {
    val c = LocalPalette.current
    return when {
        frozen -> c.ice.copy(alpha = 0.5f)
        ratio < 0f -> c.divider
        ratio == 0f -> c.track
        else -> c.accent.copy(alpha = 0.3f + 0.7f * ratio)
    }
}

@Composable
private fun Legend() {
    val c = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.less), fontSize = 11.sp, color = c.text2)
        Spacer(Modifier.width(4.dp))
        listOf(0f, 0.35f, 0.7f, 1f).forEach {
            Box(
                Modifier
                    .padding(horizontal = 1.5.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(heatColor(it, false)),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.more), fontSize = 11.sp, color = c.text2)
    }
}

/** GitHub-style grid: one column per week, the last column holds today. */
@Composable
private fun Heatmap(s: Summary) {
    val locale = appLocale()
    val firstDow = WeekFields.of(locale).firstDayOfWeek
    val todayDate = Days.date(s.today)
    val offset = (todayDate.dayOfWeek.value - firstDow.value + 7) % 7
    val lastWeekStart = s.today - offset
    val start = lastWeekStart - 7L * (HEATMAP_WEEKS - 1)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 3.dp
        val cell = (maxWidth - gap * (HEATMAP_WEEKS - 1)) / HEATMAP_WEEKS
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            for (w in 0 until HEATMAP_WEEKS) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (dIdx in 0 until 7) {
                        val d = start + w * 7L + dIdx
                        val future = d > s.today
                        Box(
                            Modifier
                                .size(cell)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (future) Color.Transparent
                                    else heatColor(s.ratio(d), d in s.frozen),
                                ),
                        )
                    }
                }
            }
        }
    }
}
