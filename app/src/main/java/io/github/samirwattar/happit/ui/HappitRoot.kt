package io.github.samirwattar.happit.ui

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.data.Habit
import io.github.samirwattar.happit.data.ThemeMode
import io.github.samirwattar.happit.ui.theme.HappitTheme
import io.github.samirwattar.happit.ui.theme.LocalPalette

/** Which habit the editor is open for; a null habit means a new one. */
private data class EditRequest(val habit: Habit?)

@Composable
fun HappitRoot(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val unlocks by vm.unlocks.collectAsStateWithLifecycle()
    val selectedDay by vm.selectedDay.collectAsStateWithLifecycle()
    val dark = when (settings.theme) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isSystemInDarkTheme()
    }
    val activity = LocalContext.current as? ComponentActivity
    LaunchedEffect(dark) {
        val style = if (dark) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    HappitTheme(dark) {
        val c = LocalPalette.current
        val ctx = LocalContext.current
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var editing by remember { mutableStateOf<EditRequest?>(null) }
        var openHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(vm.message) {
            val msg = vm.message ?: return@LaunchedEffect
            vm.message = null
            snackbar.showSnackbar(ctx.getString(msg))
        }

        BackHandler(enabled = editing == null && tab != 0) { tab = 0 }

        Box(Modifier.fillMaxSize().background(c.bg)) {
            val s = summary
            val edit = editing
            if (s != null) {
                if (edit != null) {
                    EditHabitScreen(
                        initial = edit.habit,
                        onBack = { editing = null },
                        onSave = {
                            vm.saveHabit(it)
                            editing = null
                        },
                        onDelete = {
                            vm.deleteHabit(it)
                            editing = null
                        },
                    )
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (tab) {
                                0 -> TodayScreen(
                                    vm = vm,
                                    s = s,
                                    settings = settings,
                                    onOpen = { openHabitId = it.id },
                                    onEdit = { editing = EditRequest(it) },
                                    onAdd = { editing = EditRequest(null) },
                                    onRanks = { tab = 2 },
                                    onProfile = { tab = 3 },
                                )
                                1 -> StatsScreen(s)
                                2 -> RanksScreen(s, unlocks, settings.title, vm::equipTitle)
                                else -> ProfileScreen(vm, s, settings)
                            }
                        }
                        BottomBar(tab, onTab = { tab = it }, onAdd = { editing = EditRequest(null) })
                    }
                }

                val open = openHabitId?.let { id -> s.habits.firstOrNull { it.id == id } }
                if (open != null && edit == null) {
                    HabitSheet(
                        vm = vm,
                        s = s,
                        habit = open,
                        day = selectedDay,
                        onDismiss = { openHabitId = null },
                        onEdit = {
                            openHabitId = null
                            editing = EditRequest(open)
                        },
                    )
                }
            }

            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp),
            ) { data ->
                Snackbar(data, containerColor = c.text, contentColor = c.bg)
            }

            CelebrationOverlay(
                celebration = vm.celebrations.firstOrNull(),
                onEquip = vm::equipTitle,
                onDismiss = vm::dismissCelebration,
            )

            val update by vm.update.collectAsStateWithLifecycle()
            val available = update as? UpdateState.Available
            if (vm.showUpdateDialog && available != null && vm.celebrations.isEmpty()) {
                AlertDialog(
                    onDismissRequest = { vm.showUpdateDialog = false },
                    containerColor = c.sheet,
                    title = { Text(stringResource(R.string.update_available, available.release.version)) },
                    text = {
                        Column {
                            if (available.release.notes.isNotBlank()) {
                                Text(stringResource(R.string.whats_new), fontWeight = FontWeight.SemiBold)
                                Text(available.release.notes, color = c.text2, fontSize = 14.sp, maxLines = 10)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.showUpdateDialog = false
                            vm.downloadUpdate(available.release)
                            tab = 3
                        }) { Text(stringResource(R.string.update_now), color = c.accent) }
                    },
                    dismissButton = {
                        TextButton(onClick = { vm.showUpdateDialog = false }) {
                            Text(stringResource(R.string.later), color = c.text2)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit, onAdd: () -> Unit) {
    val c = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.bg)
            .drawBehind { drawLine(c.divider, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem(Icons.Rounded.TaskAlt, stringResource(R.string.nav_today), tab == 0) { onTab(0) }
        NavItem(Icons.Rounded.Insights, stringResource(R.string.nav_stats), tab == 1) { onTab(1) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .offset(y = (-10).dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(c.accent)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add_habit),
                    tint = c.onAccent,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        NavItem(Icons.Rounded.EmojiEvents, stringResource(R.string.nav_ranks), tab == 2) { onTab(2) }
        NavItem(Icons.Rounded.Person, stringResource(R.string.nav_profile), tab == 3) { onTab(3) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalPalette.current
    Column(
        Modifier
            .weight(1f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) c.accent else c.text3, modifier = Modifier.size(24.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) c.accent else c.text3,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
