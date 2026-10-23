package io.github.samirwattar.happit.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.samirwattar.happit.R
import io.github.samirwattar.happit.logic.Achievement
import io.github.samirwattar.happit.logic.Leveling
import io.github.samirwattar.happit.logic.Rank
import io.github.samirwattar.happit.logic.Summary
import io.github.samirwattar.happit.ui.theme.LocalPalette
import java.text.NumberFormat

@Composable
fun RanksScreen(
    s: Summary,
    unlocks: Map<String, Long>,
    equipped: String?,
    onEquip: (String?) -> Unit,
) {
    val c = LocalPalette.current
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.ranks_title)) }
        item { RankHero(s) }
        item { SectionLabel(stringResource(R.string.ranks_title)) }
        items(Rank.entries) { rank -> RankRow(rank, s) }
        item {
            SectionLabel(stringResource(R.string.titles))
            Text(stringResource(R.string.titles_hint), fontSize = 13.sp, color = c.text2)
            Spacer(Modifier.height(12.dp))
        }
        items(Achievement.entries.chunked(2)) { pair ->
            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { a ->
                    val unlocked = a.name in unlocks
                    AchievementCard(
                        a,
                        unlocked = unlocked,
                        equipped = equipped == a.name,
                        onClick = { if (unlocked) onEquip(if (equipped == a.name) null else a.name) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Panel(Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.how_xp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Rule(Icons.Rounded.CheckCircle, c.accent, stringResource(R.string.xp_rule_habit))
                Rule(Icons.Rounded.LocalFireDepartment, c.flame, stringResource(R.string.xp_rule_streak))
                Rule(Icons.Rounded.AutoAwesome, c.gold, stringResource(R.string.xp_rule_perfect))
                Rule(Icons.Rounded.AcUnit, c.ice, stringResource(R.string.freezes_body))
                Rule(Icons.Rounded.Favorite, c.text2, stringResource(R.string.xp_rule_keep))
            }
        }
    }
}

@Composable
private fun RankHero(s: Summary) {
    val c = LocalPalette.current
    val locale = appLocale()
    val (bg, fg) = tintColors(s.rank.tintIndex)
    val next = s.rank.next
    Panel(color = c.card) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(78.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                Icon(s.rank.icon, contentDescription = null, tint = fg, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(s.rank.label), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.level_n, s.level.level), fontSize = 14.sp, color = c.text2)
            Spacer(Modifier.height(14.dp))
            XpBar(s.level.progress, height = 8.dp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.xp_progress, s.level.into, s.level.needed),
                    fontSize = 12.sp, color = c.text2, modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.total_xp, NumberFormat.getIntegerInstance(locale).format(s.totalXp)),
                    fontSize = 12.sp, color = c.text2,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (next != null) {
                    stringResource(
                        R.string.xp_to_next,
                        NumberFormat.getIntegerInstance(locale).format(Leveling.totalFor(next.minLevel) - s.totalXp),
                        stringResource(next.label),
                    )
                } else {
                    stringResource(R.string.max_rank)
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = c.accent,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RankRow(rank: Rank, s: Summary) {
    val c = LocalPalette.current
    val unlocked = s.level.level >= rank.minLevel
    val current = rank == s.rank
    val (bg, fg) = if (unlocked) tintColors(rank.tintIndex) else c.divider to c.text3
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(rank.icon, bg, fg, 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(rank.label),
                fontSize = 15.sp,
                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium,
                color = if (unlocked) c.text else c.text3,
            )
            Text(stringResource(R.string.rank_level_req, rank.minLevel), fontSize = 12.sp, color = c.text2)
        }
        when {
            current -> Pill(stringResource(R.string.current), c.accentSoft, c.accentText)
            unlocked -> Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
            else -> Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.text3, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AchievementCard(
    a: Achievement,
    unlocked: Boolean,
    equipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalPalette.current
    Panel(
        modifier,
        color = if (equipped) c.accentSoft else c.card,
        onClick = onClick,
        padding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                if (unlocked) a.icon else Icons.Rounded.Lock,
                if (unlocked) c.goldSoft else c.divider,
                if (unlocked) c.gold else c.text3,
                36.dp,
            )
            Spacer(Modifier.weight(1f))
            if (equipped) Pill(stringResource(R.string.equipped), c.accent, c.onAccent)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(a.label),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (unlocked) c.text else c.text2,
            maxLines = 1,
        )
        Text(stringResource(a.description), fontSize = 12.sp, color = c.text2, minLines = 2, maxLines = 2)
    }
}

@Composable
private fun Rule(icon: ImageVector, tint: Color, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = LocalPalette.current.text2)
    }
}
