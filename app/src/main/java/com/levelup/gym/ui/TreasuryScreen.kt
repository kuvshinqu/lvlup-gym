package com.levelup.gym.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.data.ChestEntity
import com.levelup.gym.game.ChestTier
import com.levelup.gym.game.Gear
import com.levelup.gym.game.Loot
import com.levelup.gym.game.Skills
import com.levelup.gym.game.Slot
import com.levelup.gym.ui.figure.ExerciseArts
import com.levelup.gym.ui.figure.HeroFigure
import kotlinx.coroutines.delay

private const val CHEST_SLOTS = 4

@Composable
fun TreasuryScreen(vm: GymViewModel) {
    val t by vm.treasury.collectAsStateWithLifecycle()
    val look by vm.heroLook.collectAsStateWithLifecycle()
    var opening by remember { mutableStateOf<ChestEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedText("Treasury", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                    Column(horizontalAlignment = Alignment.End) {
                        ResourcePill("${t.gold}") { Coin(32.dp) }
                        Text("Gear score ${t.gearScore}", style = MaterialTheme.typography.labelMedium, color = GameColors.Muted, modifier = Modifier.padding(top = 2.dp, end = 4.dp))
                    }
                }
            }

            // Chest slots
            item {
                GameCard(accent = GameColors.Gold) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedText("Chests", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, outlineWidth = 3.dp)
                            val extra = t.unopened.size - CHEST_SLOTS
                            if (extra > 0) Text("+$extra waiting", style = MaterialTheme.typography.labelMedium, color = GameColors.Gold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(CHEST_SLOTS) { i ->
                                val chest = t.unopened.getOrNull(i)
                                ChestSlot(chest, Modifier.weight(1f)) { if (opening == null) opening = chest }
                            }
                        }
                        Text(
                            "Finish a quest: wooden chest. Set a new best: iron chest. Weekly quest: golden chest. Every level up: iron chest.",
                            style = MaterialTheme.typography.bodySmall, color = GameColors.Muted,
                        )
                    }
                }
            }

            // Paper doll
            item { SectionLabel("Equipped") }
            item {
                GameCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val leftSlots = listOf(Slot.HELM, Slot.ARMOR, Slot.PAULDRONS, Slot.CLOAK)
                        val rightSlots = listOf(Slot.GAUNTLETS, Slot.BOOTS, Slot.AMULET)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { leftSlots.forEach { GearIcon(t.equipped[it], it, 54.dp) } }
                        Box(Modifier.weight(1f).height(240.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(150.dp, 40.dp).align(Alignment.BottomCenter)) {
                                drawOval(androidx.compose.ui.graphics.Brush.radialGradient(listOf(GameColors.Gold.copy(alpha = 0.45f), Color.Transparent)))
                            }
                            HeroFigure(ExerciseArts.idle, Modifier.size(width = 130.dp, height = 230.dp), t = 0f, look = look)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rightSlots.forEach { GearIcon(t.equipped[it], it, 54.dp) }
                            Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                                OutlinedText("${t.equipped.size}/7", style = MaterialTheme.typography.titleMedium, outlineWidth = 3.dp)
                            }
                        }
                    }
                }
            }
            item {
                val lines = Slot.entries.mapNotNull { s -> t.equipped[s]?.let { "+${it.bonusPercent}% ${Skills.forGroup(s.muscleGroup).name}" } }
                ParchmentNote(
                    if (lines.isEmpty()) "Open chests to find gear. Each piece adds XP to the skill its slot belongs to."
                    else "Gear bonuses: " + lines.joinToString(" · ")
                )
            }

            // Forge
            item {
                GameCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Coin(40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            OutlinedText("The forge", style = MaterialTheme.typography.titleMedium, outlineWidth = 3.dp)
                            Text("Duplicates melt into gold. Spend it on a wooden chest.", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                        }
                        GameButton(onClick = vm::buyChest, enabled = t.gold >= Loot.CHEST_PRICE, style = ButtonStyle.GOLD, height = 46.dp) {
                            Text("Buy ${Loot.CHEST_PRICE}")
                        }
                    }
                }
            }

            // Backpack
            val spare = t.owned.filter { t.equipped[it.slot]?.key != it.key }
            if (spare.isNotEmpty()) {
                item { SectionLabel("Backpack") }
                items(spare.chunked(3), key = { row -> row.joinToString { it.key } }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { g -> ItemCard(g, Modifier.weight(1f).clickable { vm.equip(g) }) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item { Text("Tap an item to wear it.", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        opening?.let { chest -> ChestOpening(vm, chest, onDone = { opening = null }) }
    }
}

@Composable
private fun ChestSlot(chest: ChestEntity?, modifier: Modifier, onOpen: () -> Unit) {
    Box(
        modifier
            .height(122.dp)
            .drawBehind { chunkyBlock(Color(0xFF16264F), 12.dp.toPx(), outline = 2.dp.toPx(), shadow = 2.dp.toPx(), lip = 0f, gloss = 0f) }
            .clickable(enabled = chest != null, onClick = onOpen),
        contentAlignment = Alignment.Center,
    ) {
        if (chest == null) {
            Text("Empty", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted.copy(alpha = 0.6f))
        } else {
            val tier = ChestTier.valueOf(chest.tier)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
                Canvas(Modifier.size(68.dp, 56.dp)) { drawChest(tier) }
                Text(tier.label.substringBefore(" "), style = MaterialTheme.typography.labelMedium, color = Color.White)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.drawBehind { chunkyBlock(ButtonStyle.GREEN.base, 8.dp.toPx(), outline = 1.8.dp.toPx(), shadow = 2.dp.toPx(), lip = 2.dp.toPx(), gloss = 0.25f) }
                        .padding(start = 10.dp, end = 10.dp, top = 1.dp, bottom = 5.dp),
                ) {
                    OutlinedText("Open", style = MaterialTheme.typography.labelSmall, fontSize = 12.sp, outlineWidth = 2.dp)
                }
            }
        }
    }
}

/** The big moment: the chest drops in, shakes, bursts open and the item card pops out. */
@Composable
private fun ChestOpening(vm: GymViewModel, chest: ChestEntity, onDone: () -> Unit) {
    val tier = ChestTier.valueOf(chest.tier)
    val drop = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val lid = remember { Animatable(0f) }
    val card = remember { Animatable(0f) }
    var reveal by remember { mutableStateOf<Pair<Gear, Boolean>?>(null) }
    val spin by rememberInfiniteTransition(label = "rays").animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "spin")

    LaunchedEffect(chest.id) {
        drop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        repeat(3) {
            shake.animateTo(1f, tween(55)); shake.animateTo(-1f, tween(90)); shake.animateTo(0f, tween(55))
        }
        reveal = vm.openChest(chest.id)
        lid.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        delay(120)
        card.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xE6070D20)).clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            val r = reveal
            if (r != null && card.value > 0.01f) {
                val (g, dup) = r
                OutlinedText(
                    if (dup) "Duplicate!" else "New gear!", style = MaterialTheme.typography.displaySmall,
                    color = if (dup) GameColors.Gold else Metals.rarityColor(g.rarity), outlineWidth = 5.dp,
                    modifier = Modifier.graphicsLayer { alpha = card.value.coerceIn(0f, 1f) },
                )
                Spacer(Modifier.height(10.dp))
                ItemCard(
                    g,
                    Modifier.width(170.dp).graphicsLayer {
                        scaleX = card.value; scaleY = card.value
                        translationY = (1 - card.value) * 160f
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (dup) "You already own it. Melted into ${g.rarity.salvageGold} gold."
                    else "${g.slot.label} · +${g.bonusPercent}% XP to ${Skills.forGroup(g.slot.muscleGroup).name}",
                    style = MaterialTheme.typography.titleSmall, color = Color.White, textAlign = TextAlign.Center,
                )
                Text(g.lore, style = MaterialTheme.typography.bodyMedium, color = GameColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            } else {
                OutlinedText(tier.label, style = MaterialTheme.typography.displaySmall, outlineWidth = 5.dp)
            }
            Canvas(
                Modifier.size(240.dp, 190.dp).graphicsLayer {
                    val d = drop.value
                    scaleX = 0.4f + 0.6f * d; scaleY = 0.4f + 0.6f * d
                    translationY = (1 - d) * -300f
                    rotationZ = shake.value * 7f
                },
            ) { drawChest(tier, open = lid.value.coerceIn(0f, 1f), glow = lid.value.coerceIn(0f, 1f), rays = spin) }
            if (r != null && card.value > 0.9f) {
                Spacer(Modifier.height(4.dp))
                RpgButton("Claim", onClick = onDone, modifier = Modifier.fillMaxWidth(), color = GameColors.Xp)
            }
        }
    }
}
