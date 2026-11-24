package com.levelup.gym.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LootTest {
    @Test fun everySlotHasOneItemPerRarity() {
        assertEquals(Slot.entries.size * Rarity.entries.size, Loot.all.size)
        Slot.entries.forEach { s -> assertEquals(Rarity.entries.toSet(), Loot.all.filter { it.slot == s }.map { it.rarity }.toSet()) }
        assertEquals(Loot.all.size, Loot.all.map { it.key }.toSet().size)
    }

    @Test fun betterChestsDropBetterLoot() {
        fun avg(t: ChestTier): Double { val r = Random(1); return (1..4000).map { Loot.rollRarity(t, r).ordinal }.average() }
        assertTrue(avg(ChestTier.WOODEN) < avg(ChestTier.IRON))
        assertTrue(avg(ChestTier.IRON) < avg(ChestTier.GOLDEN))
        val r = Random(2)
        assertTrue((1..4000).none { Loot.rollRarity(ChestTier.WOODEN, r) == Rarity.LEGENDARY })
    }

    @Test fun gearBonusOnlyForItsSkill() {
        val helm = Loot.all.first { it.slot == Slot.HELM && it.rarity == Rarity.LEGENDARY }
        assertEquals(0.10, Loot.bonusFor("Core", listOf(helm)), 1e-9)
        assertEquals(0.0, Loot.bonusFor("Chest", listOf(helm)), 1e-9)
    }
}
