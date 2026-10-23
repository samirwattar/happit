package io.github.samirwattar.happit.ui

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.HabitType
import io.github.samirwattar.happit.data.Slot
import io.github.samirwattar.happit.logic.Days
import io.github.samirwattar.happit.ui.theme.LocalPalette
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DateTextStyle

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditHabitScreen(
    initial: Habit?,
    onBack: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: (Habit) -> Unit,
) {
    val c = LocalPalette.current
    val ctx = LocalContext.current
    val locale = appLocale()

    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: "star") }
    var color by rememberSaveable { mutableIntStateOf(initial?.color ?: 0) }
    var type by rememberSaveable { mutableIntStateOf(initial?.type ?: HabitType.CHECK) }
    var target by rememberSaveable { mutableIntStateOf(initial?.target ?: 1) }
    var unit by rememberSaveable { mutableStateOf(initial?.unit ?: "") }
    var days by rememberSaveable { mutableIntStateOf(initial?.days ?: Habit.ALL_DAYS) }
    var slot by rememberSaveable { mutableIntStateOf(initial?.slot ?: Slot.ANYTIME) }
    var reminder by rememberSaveable { mutableIntStateOf(initial?.reminder ?: -1) }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun setType(t: Int) {
        type = t
        target = when (t) {
            HabitType.COUNT -> if (target < 2) 8 else target
            HabitType.DURATION -> if (target < 5) 20 else target
            else -> 1
        }
    }

    fun save() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        if (days == 0) return
        onSave(
            Habit(
                id = initial?.id ?: 0,
                name = name.trim(),
                icon = icon,
                color = color,
                type = type,
                target = if (type == HabitType.CHECK) 1 else target.coerceAtLeast(1),
                unit = if (type == HabitType.COUNT) unit.trim() else "",
                days = days,
                slot = slot,
                reminder = reminder,
                createdDay = initial?.createdDay ?: Days.today(),
                sortOrder = initial?.sortOrder ?: 0,
            ),
        )
    }

    BackHandler(onBack = onBack)

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                stringResource(if (initial == null) R.string.new_habit else R.string.edit_habit),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(stringResource(R.string.save), ::save, Modifier.padding(end = 8.dp).height(42.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            if (initial == null) {
                SectionLabel(stringResource(R.string.templates))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Templates.forEach { t ->
                        val label = stringResource(t.name)
                        val unitText = t.unit?.let { stringResource(it) } ?: ""
                        val (tb, tf) = tintColors(t.color)
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(c.card)
                                .clickable {
                                    name = label
                                    icon = t.icon
                                    color = t.color
                                    type = t.type
                                    target = t.target
                                    unit = unitText
                                    slot = t.slot
                                    nameError = false
                                }
                                .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(habitIcon(t.icon), tb, tf, 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.habit_name))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.habit_name_hint), color = c.text3) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(stringResource(R.string.name_required)) }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors(),
            )

            SectionLabel(stringResource(R.string.icon))
            val (selBg, selFg) = tintColors(color)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HabitIcons.forEach { (key, vector) ->
                    val selected = key == icon
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) selBg else c.card)
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(vector, contentDescription = null, tint = if (selected) selFg else c.text2, modifier = Modifier.size(22.dp))
                    }
                }
            }

            SectionLabel(stringResource(R.string.color))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitTints.indices.forEach { i ->
                    val (b, f) = tintColors(i)
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(b)
                            .border(if (i == color) 2.dp else 0.dp, f, CircleShape)
                            .clickable { color = i },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(f))
                    }
                }
            }

            SectionLabel(stringResource(R.string.type))
            Choice(
                listOf(
                    stringResource(R.string.type_check),
                    stringResource(R.string.type_count),
                    stringResource(R.string.type_duration),
                ),
                selected = type,
                onSelect = ::setType,
            )

            if (type != HabitType.CHECK) {
                SectionLabel(stringResource(R.string.goal))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val step = if (type == HabitType.DURATION) 5 else 1
                    val min = if (type == HabitType.DURATION) 5 else 1
                    RoundIconButton(Icons.Rounded.Remove, stringResource(R.string.decrease), {
                        target = (target - step).coerceAtLeast(min)
                    })
                    Text(
                        target.fmt(locale),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(64.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    RoundIconButton(Icons.Rounded.Add, stringResource(R.string.increase), {
                        target = (target + step).coerceAtMost(999)
                    })
                    Spacer(Modifier.width(14.dp))
                    if (type == HabitType.COUNT) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it.take(16) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.unit_hint), color = c.text3) },
                            label = { Text(stringResource(R.string.unit)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors(),
                        )
                    } else {
                        Text(stringResource(R.string.minutes), color = c.text2, fontSize = 15.sp)
                    }
                }
            }

            SectionLabel(stringResource(R.string.repeat))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipToggle(stringResource(R.string.every_day), days == Habit.ALL_DAYS, { days = Habit.ALL_DAYS })
                ChipToggle(stringResource(R.string.specific_days), days != Habit.ALL_DAYS, {
                    if (days == Habit.ALL_DAYS) days = 0b0011111
                })
            }
            if (days != Habit.ALL_DAYS) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    orderedWeek(locale).forEach { dow ->
                        val bit = 1 shl (dow.value - 1)
                        ChipToggle(
                            dow.getDisplayName(DateTextStyle.SHORT, locale),
                            days and bit != 0,
                            { days = days xor bit },
                        )
                    }
                }
                if (days == 0) {
                    Text(
                        stringResource(R.string.pick_a_day),
                        color = c.danger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            SectionLabel(stringResource(R.string.time_of_day))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Slot.MORNING, Slot.AFTERNOON, Slot.EVENING, Slot.ANYTIME).forEach { s ->
                    ChipToggle(stringResource(slotLabel(s)), slot == s, { slot = s })
                }
            }

            SectionLabel(stringResource(R.string.reminder))
            Panel(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (reminder >= 0) {
                            LocalTime.of(reminder / 60, reminder % 60)
                                .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
                        } else {
                            stringResource(R.string.reminder_off)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (reminder >= 0) c.text else c.text2,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = reminder >= 0) { showTime = true }
                            .padding(vertical = 8.dp),
                    )
                    Switch(
                        checked = reminder >= 0,
                        onCheckedChange = { on ->
                            if (on) {
                                reminder = when (slot) {
                                    Slot.MORNING -> 8 * 60
                                    Slot.AFTERNOON -> 14 * 60
                                    else -> 20 * 60
                                }
                                showTime = true
                                askNotifications()
                            } else {
                                reminder = -1
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = c.accent,
                            checkedThumbColor = c.onAccent,
                            uncheckedTrackColor = c.track,
                            uncheckedThumbColor = c.sheet,
                            uncheckedBorderColor = c.track,
                        ),
                    )
                }
            }

            if (initial != null) {
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = c.danger)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.delete_habit), color = c.danger)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showTime) {
        val state = rememberTimePickerState(
            initialHour = reminder.coerceAtLeast(0) / 60,
            initialMinute = reminder.coerceAtLeast(0) % 60,
            is24Hour = DateFormat.is24HourFormat(ctx),
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            containerColor = c.sheet,
            confirmButton = {
                TextButton(onClick = {
                    reminder = state.hour * 60 + state.minute
                    showTime = false
                }) { Text(stringResource(R.string.ok), color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel), color = c.text2) }
            },
            text = { TimePicker(state = state) },
        )
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.sheet,
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body), color = c.text2) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(initial)
                }) { Text(stringResource(R.string.delete), color = c.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel), color = c.text2) }
            },
        )
    }
}

@Composable
fun fieldColors() = LocalPalette.current.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.track,
        focusedLabelColor = c.accent,
        cursorColor = c.accent,
        focusedContainerColor = c.bg,
        unfocusedContainerColor = c.bg,
    )
}
