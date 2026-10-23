package io.github.samirwattar.happit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.HabitType
import io.github.samirwattar.happit.data.Settings
import io.github.samirwattar.happit.data.Slot
import io.github.samirwattar.happit.logic.Days
import io.github.samirwattar.happit.logic.Summary
import io.github.samirwattar.happit.ui.theme.LocalPalette
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.TextStyle as DateTextStyle

@Composable
fun TodayScreen(
    vm: AppViewModel,
    s: Summary,
    settings: Settings,
    onOpen: (Habit) -> Unit,
    onEdit: (Habit) -> Unit,
    onAdd: () -> Unit,
    onRanks: () -> Unit,
    onProfile: () -> Unit,
) {
    val day by vm.selectedDay.collectAsStateWithLifecycle()
    val scheduled = s.scheduledOn(day)
    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        item { Header(settings.name, s, day, onProfile) }
        item { WeekStrip(s, day, vm::selectDay) }
        item { SummaryCard(s, day, scheduled, onRanks) }
        item { Quote(day) }
        when {
            s.habits.isEmpty() -> item { EmptyState(onAdd) }
            scheduled.isEmpty() -> item {
                EmptyHint(Icons.Rounded.SelfImprovement, stringResource(R.string.rest_day))
            }
            else -> for (slot in listOf(Slot.MORNING, Slot.AFTERNOON, Slot.EVENING, Slot.ANYTIME)) {
                val list = scheduled.filter { it.slot == slot }
                if (list.isEmpty()) continue
                item(key = "slot$slot") { SectionLabel(stringResource(slotLabel(slot))) }
                items(list, key = { it.id }) { h ->
                    HabitRow(
                        habit = h,
                        s = s,
                        day = day,
                        pop = vm.xpPop,
                        onOpen = { onOpen(h) },
                        onEdit = { onEdit(h) },
                        onAction = {
                            val done = s.isDone(h.id, day)
                            when {
                                h.type == HabitType.CHECK -> vm.toggle(h, day)
                                h.type == HabitType.COUNT && !done -> vm.addProgress(h, day, 1)
                                else -> onOpen(h)
                            }
                        },
                    )
                }
            }
        }
        if (day != s.today) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { vm.selectDay(s.today) }) {
                        Text(stringResource(R.string.back_to_today), color = LocalPalette.current.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(name: String, s: Summary, day: Long, onProfile: () -> Unit) {
    val c = LocalPalette.current
    val locale = appLocale()
    val date = Days.date(day).format(
        DateTimeFormatter.ofPattern("EEEE d MMMM", locale).withDecimalStyle(DecimalStyle.of(locale)),
    )
    val hour = LocalTime.now().hour
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.greeting_morning
            hour < 18 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(date, fontSize = 13.sp, color = c.text2)
            Text(
                if (name.isBlank()) greeting else stringResource(R.string.greeting_with_name, greeting, name),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(c.accentSoft)
                .clickable(onClick = onProfile),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                s.level.level.fmt(locale),
                color = c.accentText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun WeekStrip(s: Summary, selected: Long, onSelect: (Long) -> Unit) {
    val c = LocalPalette.current
    val locale = appLocale()
    val style = if (locale.language == "ar") DateTextStyle.NARROW else DateTextStyle.SHORT
    Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        for (d in s.today - 6..s.today) {
            val date = Days.date(d)
            val isSel = d == selected
            val ratio = s.ratio(d)
            val bg by animateColorAsState(if (isSel) c.accentSoft else Color.Transparent, label = "day")
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onSelect(d) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(style, locale),
                    fontSize = 11.sp,
                    color = if (isSel) c.accentText else c.text3,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Ring(
                        progress = ratio.coerceAtLeast(0f),
                        modifier = Modifier.size(34.dp),
                        stroke = 3.dp,
                        track = if (ratio < 0f) Color.Transparent else c.track,
                    )
                    Text(
                        date.dayOfMonth.fmt(locale),
                        fontSize = 13.sp,
                        fontWeight = if (d == s.today) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) c.accentText else c.text,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(s: Summary, day: Long, scheduled: List<Habit>, onRanks: () -> Unit) {
    val c = LocalPalette.current
    val locale = appLocale()
    val done = scheduled.count { s.isDone(it.id, day) }
    Panel(Modifier.padding(top = 12.dp), onClick = onRanks, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                Ring(
                    progress = if (scheduled.isEmpty()) 0f else done.toFloat() / scheduled.size,
                    modifier = Modifier.size(68.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${done.fmt(locale)}/${scheduled.size.fmt(locale)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.done_label), fontSize = 10.sp, color = c.text2)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(s.rank.label),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.level_n, s.level.level), fontSize = 12.sp, color = c.text2)
                }
                XpBar(s.level.progress, Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.xp_progress, s.level.into, s.level.needed),
                        fontSize = 11.sp,
                        color = c.text2,
                        modifier = Modifier.weight(1f),
                    )
                    StreakLabel(s.streak)
                }
            }
        }
    }
}

@Composable
fun StreakLabel(streak: Int) {
    val c = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (streak > 0) c.flame else c.text3,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            if (streak > 0) pluralStringResource(R.plurals.streak_days, streak, streak)
            else stringResource(R.string.no_streak_yet),
            fontSize = 11.sp,
            color = if (streak > 0) c.flame else c.text3,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun Quote(day: Long) {
    val c = LocalPalette.current
    val quotes = stringArrayResource(R.array.quotes)
    Row(Modifier.padding(top = 14.dp, start = 2.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.FormatQuote, contentDescription = null, tint = c.text3, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(quotes[day.mod(quotes.size)], fontSize = 13.sp, color = c.text2)
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().padding(top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.accentText, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.empty_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.empty_body),
            fontSize = 14.sp,
            color = c.text2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(stringResource(R.string.empty_cta), onAdd, icon = Icons.Rounded.Add)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitRow(
    habit: Habit,
    s: Summary,
    day: Long,
    pop: XpPop?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onAction: () -> Unit,
) {
    val c = LocalPalette.current
    val done = s.isDone(habit.id, day)
    val value = s.entry(habit.id, day)?.value ?: 0
    val streak = s.habitStats[habit.id]?.current ?: 0
    val (bg, fg) = tintColors(habit.color)
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onEdit)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(habitIcon(habit.icon), bg, fg, 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                habit.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (done) c.text2 else c.text,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (habit.type) {
                    HabitType.COUNT -> Text(
                        stringResource(R.string.progress_count, value, habit.target, habit.unit).trim(),
                        fontSize = 12.sp, color = c.text2,
                    )
                    HabitType.DURATION -> Text(
                        stringResource(R.string.progress_minutes, value, habit.target),
                        fontSize = 12.sp, color = c.text2,
                    )
                }
                if (streak > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment, contentDescription = null,
                            tint = c.flame, modifier = Modifier.size(13.dp),
                        )
                        Text(pluralStringResource(R.plurals.days, streak, streak), fontSize = 12.sp, color = c.text2)
                    }
                } else if (habit.type == HabitType.CHECK) {
                    Text(stringResource(slotLabel(habit.slot)), fontSize = 12.sp, color = c.text3)
                }
            }
        }
        ActionCircle(habit, done, pop, onAction)
    }
}

@Composable
private fun ActionCircle(habit: Habit, done: Boolean, pop: XpPop?, onClick: () -> Unit) {
    val c = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val fill by animateColorAsState(if (done) c.accent else Color.Transparent, label = "fill")
    val scale = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done) {
            scale.snapTo(0.8f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val action = when {
        done && habit.type == HabitType.CHECK -> stringResource(R.string.mark_undone)
        done -> null
        habit.type == HabitType.COUNT -> stringResource(R.string.increase)
        habit.type == HabitType.DURATION -> stringResource(R.string.timer_start)
        else -> stringResource(R.string.mark_done)
    }
    val description = if (action == null) habit.name else "${habit.name}, $action"
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(46.dp)
                .semantics { contentDescription = description }
                .clip(CircleShape)
                .clickable {
                    if (!done) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .clip(CircleShape)
                    .background(fill)
                    .border(2.dp, if (done) c.accent else c.track, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when {
                    done -> Icons.Rounded.Check
                    habit.type == HabitType.COUNT -> Icons.Rounded.Add
                    habit.type == HabitType.DURATION -> Icons.Rounded.PlayArrow
                    else -> null
                }
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (done) c.onAccent else c.text3,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (pop != null && pop.habitId == habit.id) XpFloat(pop)
    }
}

@Composable
private fun XpFloat(pop: XpPop) {
    val c = LocalPalette.current
    val anim = remember(pop.token) { Animatable(0f) }
    LaunchedEffect(pop.token) { anim.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    if (anim.value >= 1f) return
    Text(
        "+${pop.xp} XP",
        modifier = Modifier
            .requiredWidth(90.dp)
            .offset(y = (-22 - 26 * anim.value).dp)
            .graphicsLayer { alpha = 1f - anim.value },
        color = c.accent,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )
}
