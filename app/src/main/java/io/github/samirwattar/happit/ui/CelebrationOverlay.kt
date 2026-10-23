package io.github.samirwattar.happit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.samirwattar.happit.ui.theme.LocalPalette

@Composable
fun BoxScope.CelebrationOverlay(
    celebration: Celebration?,
    onEquip: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalPalette.current
    var shown by remember { mutableStateOf<Celebration?>(null) }
    if (celebration != null) shown = celebration
    val visible = celebration != null

    BackHandler(enabled = visible, onBack = onDismiss)

    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    val current = shown
    if (visible && current !is Celebration.FreezeEarned) Confetti(current ?: Unit)

    AnimatedVisibility(
        visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(c.sheet)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (current) {
                is Celebration.LevelUp -> {
                    Pill(stringResource(R.string.level_up), c.accentSoft, c.accentText)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.level_n, current.level), fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
                    val rank = current.rank
                    if (rank != null) {
                        Spacer(Modifier.height(14.dp))
                        val (bg, fg) = tintColors(rank.tintIndex)
                        Badge(rank.icon, bg, fg)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.you_are_now), fontSize = 16.sp, color = c.text2)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(rank.label), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.rank_reward), fontSize = 13.sp, color = c.ice)
                    }
                    Spacer(Modifier.height(22.dp))
                    PrimaryButton(stringResource(R.string.continue_btn), onDismiss, Modifier.fillMaxWidth())
                }
                is Celebration.PerfectDay -> {
                    Badge(Icons.Rounded.AutoAwesome, c.goldSoft, c.gold)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.perfect_day), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.perfect_day_body), fontSize = 15.sp, color = c.text2, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    PrimaryButton(stringResource(R.string.continue_btn), onDismiss, Modifier.fillMaxWidth())
                }
                is Celebration.Unlocked -> {
                    Badge(current.achievement.icon, c.goldSoft, c.gold)
                    Spacer(Modifier.height(12.dp))
                    Pill(stringResource(R.string.title_unlocked), c.goldSoft, c.gold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(current.achievement.label), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(current.achievement.description), fontSize = 15.sp, color = c.text2, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    PrimaryButton(
                        stringResource(R.string.equip_title),
                        {
                            onEquip(current.achievement.name)
                            onDismiss()
                        },
                        Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.continue_btn), color = c.text2) }
                }
                is Celebration.FreezeEarned -> {
                    Badge(Icons.Rounded.AcUnit, c.ice.copy(alpha = 0.18f), c.ice)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.freeze_earned), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.freeze_earned_body), fontSize = 15.sp, color = c.text2, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    PrimaryButton(stringResource(R.string.continue_btn), onDismiss, Modifier.fillMaxWidth())
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun Badge(icon: ImageVector, bg: Color, fg: Color) {
    Box(Modifier.size(76.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(38.dp))
    }
}
