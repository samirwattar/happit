package io.github.samirwattar.happit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.HabitType
import io.github.samirwattar.happit.logic.Streaks
import io.github.samirwattar.happit.logic.Summary
import io.github.samirwattar.happit.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitSheet(
    vm: AppViewModel,
    s: Summary,
    habit: Habit,
    day: Long,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    val c = LocalPalette.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (bg, fg) = tintColors(habit.color)
    val stat = s.habitStats[habit.id]
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = c.sheet,
        dragHandle = { BottomSheetDefaults.DragHandle(color = c.track) },
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(habitIcon(habit.icon), bg, fg, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(habit.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(scheduleText(habit, appLocale()), fontSize = 13.sp, color = c.text2)
                }
            }
            Spacer(Modifier.height(22.dp))
            when (habit.type) {
                HabitType.COUNT -> CountControl(vm, s, habit, day)
                HabitType.DURATION -> TimerControl(vm, s, habit, day)
                else -> {
                    val done = s.isDone(habit.id, day)
                    if (done) {
                        SecondaryButton(
                            stringResource(R.string.mark_undone),
                            { vm.setProgress(habit, day, 0) },
                            Modifier.fillMaxWidth(),
                        )
                    } else {
                        PrimaryButton(
                            stringResource(R.string.mark_done),
                            { vm.setProgress(habit, day, 1) },
                            Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.Check,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(
                    pluralStringResource(R.plurals.days, stat?.current ?: 0, stat?.current ?: 0),
                    stringResource(R.string.current_streak),
                    Modifier.weight(1f),
                )
                MiniStat(
                    pluralStringResource(R.plurals.days, stat?.best ?: 0, stat?.best ?: 0),
                    stringResource(R.string.best_streak),
                    Modifier.weight(1f),
                )
                MiniStat(
                    stringResource(R.string.rate_30, stat?.rate30 ?: 0),
                    stringResource(R.string.rate_30_short),
                    Modifier.weight(1f),
                )
            }
            SectionLabel(stringResource(R.string.last_30))
            History30(s, habit, fg)
            Spacer(Modifier.height(20.dp))
            SecondaryButton(stringResource(R.string.edit), onEdit, Modifier.fillMaxWidth(), icon = Icons.Rounded.Edit)
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    val c = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(c.card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, fontSize = 11.sp, color = c.text2, maxLines = 1)
    }
}

@Composable
private fun CountControl(vm: AppViewModel, s: Summary, habit: Habit, day: Long) {
    val c = LocalPalette.current
    val locale = appLocale()
    val value = s.entry(habit.id, day)?.value ?: 0
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(
            Icons.Rounded.Remove, stringResource(R.string.decrease),
            { if (value > 0) vm.addProgress(habit, day, -1) }, size = 52.dp,
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${value.fmt(locale)} / ${habit.target.fmt(locale)}",
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (habit.unit.isNotBlank()) Text(habit.unit, fontSize = 13.sp, color = c.text2)
        }
        RoundIconButton(
            Icons.Rounded.Add, stringResource(R.string.increase),
            { vm.addProgress(habit, day, 1) }, size = 52.dp,
        )
    }
    XpBar(value.toFloat() / habit.target, Modifier.padding(top = 16.dp), height = 8.dp)
}

@Composable
private fun TimerControl(vm: AppViewModel, s: Summary, habit: Habit, day: Long) {
    val c = LocalPalette.current
    val locale = appLocale()
    val stored = s.entry(habit.id, day)?.value ?: 0
    var running by remember { mutableStateOf(false) }
    var seconds by remember(habit.id, day) { mutableIntStateOf(stored * 60) }
    LaunchedEffect(stored) { if (!running) seconds = stored * 60 }
    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            seconds++
            if (seconds % 60 == 0) {
                vm.setProgress(habit, day, seconds / 60)
                if (seconds / 60 >= habit.target) running = false
            }
        }
    }
    val goal = habit.target * 60
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Ring(seconds.toFloat() / goal, Modifier.size(150.dp), stroke = 9.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format(locale, "%02d:%02d", seconds / 60, seconds % 60),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.progress_minutes, seconds / 60, habit.target),
                    fontSize = 12.sp,
                    color = c.text2,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(
                stringResource(if (running) R.string.timer_pause else R.string.timer_start),
                { running = !running },
                icon = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            )
            SecondaryButton(stringResource(R.string.add_5_min), {
                seconds = (seconds / 60 + 5) * 60
                vm.setProgress(habit, day, seconds / 60)
            })
            TextButton(onClick = {
                running = false
                seconds = 0
                vm.setProgress(habit, day, 0)
            }) { Text(stringResource(R.string.reset), color = c.text2) }
        }
    }
}

@Composable
private fun History30(s: Summary, habit: Habit, color: Color) {
    val c = LocalPalette.current
    val days = (s.today - 29..s.today).toList()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        days.chunked(15).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                week.forEach { d ->
                    val scheduled = Streaks.scheduled(habit, d)
                    val done = s.isDone(habit.id, d)
                    val frozen = d in s.frozen && scheduled
                    val fill = when {
                        done -> color
                        frozen -> c.ice.copy(alpha = 0.45f)
                        scheduled && d < s.today -> c.track
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(fill)
                            .then(
                                if (fill == Color.Transparent) {
                                    Modifier.border(1.dp, c.divider, RoundedCornerShape(4.dp))
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}

/** "Every day · Morning · 7:30 AM" or "Mon, Wed, Fri · Evening". */
@Composable
private fun scheduleText(habit: Habit, locale: Locale): String {
    val parts = mutableListOf<String>()
    parts += if (habit.days == Habit.ALL_DAYS) {
        stringResource(R.string.schedule_every_day)
    } else {
        orderedWeek(locale)
            .filter { habit.days and (1 shl (it.value - 1)) != 0 }
            .joinToString(if (locale.language == "ar") "، " else ", ") {
                it.getDisplayName(DateTextStyle.SHORT, locale)
            }
    }
    parts += stringResource(slotLabel(habit.slot))
    if (habit.reminder >= 0) {
        parts += LocalTime.of(habit.reminder / 60, habit.reminder % 60)
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    }
    return parts.joinToString(" · ")
}
