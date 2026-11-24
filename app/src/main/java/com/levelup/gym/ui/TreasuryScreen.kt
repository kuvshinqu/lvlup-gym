package com.levelup.gym.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.levelup.gym.R
import com.levelup.gym.data.ChestEntity
import com.levelup.gym.game.ChestTier
import com.levelup.gym.game.Gear
import com.levelup.gym.game.Loot
import com.levelup.gym.game.Skills
import com.levelup.gym.game.Slot
import com.levelup.gym.ui.figure.ExerciseArts
import com.levelup.gym.ui.figure.HeroFigure
import kotlinx.coroutines.launch

@Composable
fun TreasuryScreen(vm: GymViewModel) {
    val t by vm.treasury.collectAsStateWithLifecycle()
    val look by vm.heroLook.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var opening by remember { mutableStateOf<ChestEntity?>(null) }
    var reveal by remember { mutableStateOf<Pair<Gear, Boolean>?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("TREASURY", style = MaterialTheme.typography.labelSmall, color = GameColors.Muted)
                    Text("Gear and loot", style = MaterialTheme.typography.headlineMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${t.gold} gold", color = GameColors.Gold, style = MaterialTheme.typography.titleMedium)
                    Text("Gear score ${t.gearScore}", color = GameColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Opening a chest
        opening?.let { chest ->
            item(key = "opening-${chest.id}") {
                ChestOpening(
                    chest, reveal,
                    onOpened = { scope.launch { reveal = vm.openChest(chest.id) } },
                    onDone = { opening = null; reveal = null },
                )
            }
        }

        // Chests waiting
        item { SectionLabel("Chests") }
        item {
            if (t.unopened.isEmpty()) {
                Text(
                    "No chests waiting. Finish a quest for a wooden chest, set a new best for an iron one, complete the weekly quest for a golden one.",
                    style = MaterialTheme.typography.bodyMedium, color = GameColors.Muted,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(t.unopened, key = { it.id }) { chest ->
                        val tier = ChestTier.valueOf(chest.tier)
                        Column(
                            Modifier.width(104.dp).clickable(enabled = opening == null) { opening = chest },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Canvas(Modifier.size(96.dp, 80.dp)) { drawChest(tier) }
                            Text(tier.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                            Text("Tap to open", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                        }
                    }
                }
            }
        }
        item {
            GameCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MedievalIcon(R.drawable.ic_loot_chest, 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("The forge", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Duplicate items are melted into gold. ${Loot.CHEST_PRICE} gold buys a wooden chest.",
                            style = MaterialTheme.typography.bodySmall, color = GameColors.Muted,
                        )
                    }
                    TextButton(onClick = vm::buyChest, enabled = t.gold >= Loot.CHEST_PRICE) { Text("Buy") }
                }
            }
        }

        // Equipment
        item { SectionLabel("Equipped") }
        item {
            GameCard(accent = GameColors.Gold) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeroFigure(ExerciseArts.idle, Modifier.size(width = 110.dp, height = 150.dp), t = 0f, look = look)
                    Spacer(Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Slot.entries.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { slot -> GearIcon(t.equipped[slot], slot, 46.dp) }
                            }
                        }
                    }
                }
            }
        }
        items(Slot.entries, key = { "slot-${it.name}" }) { slot ->
            val g = t.equipped[slot]
            Row(verticalAlignment = Alignment.CenterVertically) {
                GearIcon(g, slot, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(g?.name ?: "Empty ${slot.label.lowercase()} slot", style = MaterialTheme.typography.bodyLarge, color = if (g == null) GameColors.Muted else Metals.rarityColor(g.rarity))
                    Text(
                        "${slot.label} · " + (g?.let { "+${it.bonusPercent}% XP to ${Skills.forGroup(slot.muscleGroup).name}" } ?: "boosts ${Skills.forGroup(slot.muscleGroup).name}"),
                        style = MaterialTheme.typography.bodySmall, color = GameColors.Muted,
                    )
                }
            }
        }

        // Backpack
        val spare = t.owned.filter { t.equipped[it.slot]?.key != it.key }
        if (spare.isNotEmpty()) {
            item { SectionLabel("Backpack") }
            items(spare, key = { "bag-${it.key}" }) { g ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GearIcon(g, g.slot, 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, color = Metals.rarityColor(g.rarity), style = MaterialTheme.typography.bodyLarge)
                        Text("${g.rarity.label} ${g.slot.label.lowercase()} · +${g.bonusPercent}% XP", style = MaterialTheme.typography.bodySmall, color = GameColors.Muted)
                    }
                    TextButton(onClick = { vm.equip(g) }) { Text("Equip") }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ChestOpening(chest: ChestEntity, reveal: Pair<Gear, Boolean>?, onOpened: () -> Unit, onDone: () -> Unit) {
    val tier = ChestTier.valueOf(chest.tier)
    val lid = remember { Animatable(0f) }
    LaunchedEffect(chest.id) {
        lid.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        onOpened()
    }
    GameCard(accent = reveal?.first?.let { Metals.rarityColor(it.rarity) } ?: GameColors.Gold) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(200.dp, 150.dp)) { drawChest(tier, open = lid.value, glow = lid.value) }
            val r = reveal
            if (r == null) {
                Text("Opening the ${tier.label.lowercase()}...", color = GameColors.Muted)
            } else {
                val (g, dup) = r
                GearIcon(g, g.slot, 72.dp)
                Text(g.rarity.label.uppercase(), color = Metals.rarityColor(g.rarity), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(g.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(g.lore, style = MaterialTheme.typography.bodyMedium, color = GameColors.Muted, textAlign = TextAlign.Center)
                Text(
                    if (dup) "You already own this. Melted into ${g.rarity.salvageGold} gold."
                    else "${g.slot.label} · +${g.bonusPercent}% XP to ${Skills.forGroup(g.slot.muscleGroup).name}",
                    style = MaterialTheme.typography.bodyMedium, color = GameColors.Gold, textAlign = TextAlign.Center,
                )
                RpgButton("Claim", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
