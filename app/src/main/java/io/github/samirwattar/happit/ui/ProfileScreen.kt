package io.github.samirwattar.happit.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.samirwattar.happit.BuildConfig
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.data.Settings
import io.github.samirwattar.happit.data.ThemeMode
import io.github.samirwattar.happit.logic.Achievement
import io.github.samirwattar.happit.logic.Summary
import io.github.samirwattar.happit.reminders.Reminders
import io.github.samirwattar.happit.update.Updater
import io.github.samirwattar.happit.ui.theme.LocalPalette
import java.text.NumberFormat

@Composable
fun ProfileScreen(vm: AppViewModel, s: Summary, settings: Settings) {
    val c = LocalPalette.current
    val ctx = LocalContext.current
    val locale = appLocale()
    var editName by rememberSaveable { mutableStateOf(false) }
    var confirmImport by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri)
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.profile_title)) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(68.dp).clip(CircleShape).background(c.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    if (settings.name.isBlank()) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(34.dp))
                    } else {
                        Text(
                            settings.name.take(1).uppercase(locale),
                            color = c.onAccent,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.clickable { editName = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            settings.name.ifBlank { stringResource(R.string.name_hint) },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (settings.name.isBlank()) c.text2 else c.text,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.your_name), tint = c.text3, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    val title = Achievement.of(settings.title)
                    Pill(
                        if (title != null) stringResource(title.label) else stringResource(s.rank.label),
                        c.accentSoft,
                        c.accentText,
                    )
                }
            }
        }
        item {
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStat(stringResource(R.string.level_short), s.level.level.fmt(locale), Modifier.weight(1f))
                SmallStat(stringResource(R.string.xp), NumberFormat.getIntegerInstance(locale).format(s.totalXp), Modifier.weight(1f))
                SmallStat(stringResource(R.string.freezes), settings.freezes.fmt(locale), Modifier.weight(1f))
            }
        }
        item {
            Panel(Modifier.padding(top = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.AcUnit, c.ice.copy(alpha = 0.18f), c.ice, 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.freezes_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            pluralStringResource(R.plurals.freezes_count, settings.freezes, settings.freezes),
                            fontSize = 13.sp,
                            color = c.text2,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.freezes_body), fontSize = 13.sp, color = c.text2)
            }
        }

        item { SectionLabel(stringResource(R.string.settings), Modifier.padding(top = 8.dp)) }
        item {
            SettingBlock(stringResource(R.string.appearance)) {
                Choice(
                    listOf(
                        stringResource(R.string.theme_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark),
                    ),
                    selected = settings.theme,
                    onSelect = vm::setTheme,
                )
            }
        }
        item {
            val tags = listOf("", "en", "ar")
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
            SettingBlock(stringResource(R.string.language)) {
                Choice(
                    listOf(
                        stringResource(R.string.lang_system),
                        stringResource(R.string.lang_en),
                        stringResource(R.string.lang_ar),
                    ),
                    selected = tags.indexOf(current).coerceAtLeast(0),
                    onSelect = { i ->
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[i]))
                    },
                )
            }
        }
        item { NotificationsBlock() }
        item {
            SettingBlock(stringResource(R.string.backup)) {
                Text(stringResource(R.string.backup_desc), fontSize = 13.sp, color = c.text2)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        stringResource(R.string.export_data),
                        { exportLauncher.launch("happit-backup-${s.today}.json") },
                        Modifier.weight(1f),
                        icon = Icons.Rounded.FileUpload,
                    )
                    SecondaryButton(
                        stringResource(R.string.import_data),
                        { confirmImport = true },
                        Modifier.weight(1f),
                        icon = Icons.Rounded.FileDownload,
                    )
                }
            }
        }
        item { UpdateBlock(vm) }
        item {
            SettingBlock(stringResource(R.string.about)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.app_name), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME), fontSize = 12.sp, color = c.text2)
                    }
                    TextButton(onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Updater.repoUrl)))
                    }) {
                        Text(stringResource(R.string.github), color = c.accent)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (editName) {
        var text by remember { mutableStateOf(settings.name) }
        AlertDialog(
            onDismissRequest = { editName = false },
            containerColor = c.sheet,
            title = { Text(stringResource(R.string.your_name)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(30) },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.name_hint), color = c.text3) },
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = fieldColors(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setName(text)
                    editName = false
                }) { Text(stringResource(R.string.save), color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = { editName = false }) { Text(stringResource(R.string.cancel), color = c.text2) }
            },
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            containerColor = c.sheet,
            title = { Text(stringResource(R.string.import_confirm_title)) },
            text = { Text(stringResource(R.string.import_confirm_body), color = c.text2) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                }) { Text(stringResource(R.string.import_data), color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text(stringResource(R.string.cancel), color = c.text2) }
            },
        )
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier = Modifier) {
    val c = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, fontSize = 12.sp, color = c.text2, maxLines = 1)
    }
}

@Composable
private fun SettingBlock(title: String, content: @Composable () -> Unit) {
    Panel(Modifier.padding(bottom = 12.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun NotificationsBlock() {
    val c = LocalPalette.current
    val ctx = LocalContext.current
    var allowed by remember { mutableStateOf(Reminders.canNotify(ctx)) }
    LifecycleResumeEffect(Unit) {
        allowed = Reminders.canNotify(ctx)
        onPauseOrDispose { }
    }
    val openSettings = {
        ctx.startActivity(
            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = Reminders.canNotify(ctx)
        if (!granted) openSettings()
    }
    SettingBlock(stringResource(R.string.notifications)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (allowed) R.string.notif_on else R.string.notif_off),
                fontSize = 13.sp,
                color = c.text2,
                modifier = Modifier.weight(1f),
            )
            if (!allowed) {
                Spacer(Modifier.width(10.dp))
                SecondaryButton(stringResource(R.string.allow), {
                    if (Build.VERSION.SDK_INT >= 33) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openSettings()
                    }
                })
            }
        }
    }
}

@Composable
private fun UpdateBlock(vm: AppViewModel) {
    val c = LocalPalette.current
    val state by vm.update.collectAsStateWithLifecycle()
    SettingBlock(stringResource(R.string.updates)) {
        Text(stringResource(R.string.version_label, BuildConfig.VERSION_NAME), fontSize = 13.sp, color = c.text2)
        Spacer(Modifier.height(10.dp))
        when (val st = state) {
            is UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = c.accent, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.checking), fontSize = 14.sp)
            }
            is UpdateState.Available -> {
                Text(
                    stringResource(R.string.update_available, st.release.version),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.accent,
                )
                if (st.release.notes.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(st.release.notes, fontSize = 13.sp, color = c.text2, maxLines = 8)
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    stringResource(R.string.update_now),
                    { vm.downloadUpdate(st.release) },
                    Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Download,
                )
            }
            is UpdateState.Downloading -> {
                Text(stringResource(R.string.downloading, st.percent), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { st.percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = c.accent,
                    trackColor = c.track,
                )
            }
            is UpdateState.Ready -> PrimaryButton(
                stringResource(R.string.install),
                vm::installUpdate,
                Modifier.fillMaxWidth(),
                icon = Icons.Rounded.SystemUpdate,
            )
            else -> {
                when (st) {
                    is UpdateState.UpToDate -> Text(stringResource(R.string.up_to_date), fontSize = 14.sp, color = c.accent)
                    is UpdateState.Failed -> Text(stringResource(st.message), fontSize = 13.sp, color = c.danger)
                    else -> Unit
                }
                if (st !is UpdateState.Idle) Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    stringResource(R.string.check_updates),
                    { vm.checkForUpdates() },
                    Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.SystemUpdate,
                )
            }
        }
    }
}
