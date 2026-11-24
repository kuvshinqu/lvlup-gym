package com.levelup.gym.game

import kotlin.random.Random

enum class Rarity(val label: String, val color: Long, val xpBonus: Double, val salvageGold: Int) {
    COMMON("Common", 0xFFB8BEC9, 0.02, 10),
    UNCOMMON("Uncommon", 0xFF6FCF6A, 0.03, 25),
    RARE("Rare", 0xFF4FA3FF, 0.05, 60),
    EPIC("Epic", 0xFFB46BFF, 0.07, 150),
    LEGENDARY("Legendary", 0xFFFFA931, 0.10, 400),
}

/** Each slot belongs to one skill: gear in it adds XP to that skill's sets. */
enum class Slot(val label: String, val muscleGroup: String) {
    HELM("Helm", "Core"),
    ARMOR("Armour", "Chest"),
    PAULDRONS("Pauldrons", "Shoulders"),
    GAUNTLETS("Gauntlets", "Arms"),
    CLOAK("Cloak", "Back"),
    BOOTS("Boots", "Legs"),
    AMULET("Amulet", "Cardio"),
}

data class Gear(val key: String, val name: String, val slot: Slot, val rarity: Rarity, val lore: String) {
    val bonusPercent: Int get() = (rarity.xpBonus * 100).toInt()
}

enum class ChestTier(val label: String, val weights: List<Int>) {
    WOODEN("Wooden chest", listOf(62, 28, 9, 1, 0)),
    IRON("Iron chest", listOf(25, 42, 24, 8, 1)),
    GOLDEN("Golden chest", listOf(0, 20, 45, 27, 8)),
}

/**
 * Loot rules.
 * - Every finished workout drops a Wooden chest, or an Iron chest if you set a new best in it.
 * - Completing the weekly quest adds a Golden chest; every level up adds an Iron chest.
 * - A duplicate item is melted down for gold; 120 gold buys a Wooden chest at the forge.
 */
object Loot {
    const val MAX_GEAR_BONUS = 0.25
    const val CHEST_PRICE = 120

    private val names: Map<Slot, List<Pair<String, String>>> = mapOf(
        Slot.HELM to listOf(
            "Leather Coif" to "Smells of old sweat and older victories.",
            "Iron Nasal Helm" to "Dented in all the right places.",
            "Knight's Bascinet" to "Forged for squires who never skip leg day.",
            "Shadowsteel Helm" to "Quenched in a moonless lake.",
            "Crown of the Dragon King" to "Only those who brace their core may wear it.",
        ),
        Slot.ARMOR to listOf(
            "Padded Gambeson" to "Quilted linen, stitched by a patient grandmother.",
            "Riveted Mail" to "Thirty thousand rings, every one closed by hand.",
            "Steel Breastplate" to "Polished until the bench press shows in it.",
            "Obsidian Plate" to "Black glass made iron by a mountain's fire.",
            "Aegis of Ashvale" to "Worn by the last champion of the burned city.",
        ),
        Slot.PAULDRONS to listOf(
            "Leather Spaulders" to "Boiled leather, harder than it looks.",
            "Iron Pauldrons" to "Heavy enough to count as a warm-up set.",
            "Lion Pauldrons" to "Two brass lions, both mid-roar.",
            "Stormforged Pauldrons" to "They hum before thunder.",
            "Atlas Mantle" to "The weight of the sky, made light.",
        ),
        Slot.GAUNTLETS to listOf(
            "Wrapped Hand Straps" to "Linen wraps that remember every rep.",
            "Iron Gauntlets" to "Grip the bar like you mean it.",
            "Knight's Gauntlets" to "Articulated fingers, chalk not included.",
            "Gauntlets of the Ox" to "Said to curl a cart wheel.",
            "Titan's Grip" to "Nothing slips from these hands.",
        ),
        Slot.CLOAK to listOf(
            "Traveller's Cloak" to "Patched, faded and faithful.",
            "Wolfpelt Cloak" to "The wolf lost a pull-up contest.",
            "Ranger's Mantle" to "Green as the deep wood.",
            "Cloak of the Night Owl" to "For heroes who train after dark.",
            "Wings of the Phoenix" to "It smoulders after every workout.",
        ),
        Slot.BOOTS to listOf(
            "Worn Boots" to "Soles thin from a thousand stairs.",
            "Iron-shod Boots" to "Every step rings on the stone.",
            "Strider's Greaves" to "Light, fast and built for lunges.",
            "Mountainbreaker Greaves" to "Squat deep enough and the earth shakes.",
            "Sabatons of the Colossus" to "Planted like roots of an ancient oak.",
        ),
        Slot.AMULET to listOf(
            "Bone Charm" to "A lucky knucklebone on a cord.",
            "Copper Amulet" to "Turns your skin green, your lungs stronger.",
            "Silver Wind Pendant" to "Cool air follows you uphill.",
            "Heart of the Stag" to "It beats a little slower than yours.",
            "Eternal Flame Amulet" to "Never goes out, never runs out of breath.",
        ),
    )

    val all: List<Gear> = Slot.entries.flatMap { slot ->
        names.getValue(slot).mapIndexed { i, (name, lore) ->
            Gear("${slot.name.lowercase()}_$i", name, slot, Rarity.entries[i], lore)
        }
    }

    fun gear(key: String?): Gear? = all.firstOrNull { it.key == key }

    fun rollRarity(tier: ChestTier, random: Random = Random): Rarity {
        var r = random.nextInt(tier.weights.sum())
        tier.weights.forEachIndexed { i, w -> if (r < w) return Rarity.entries[i]; r -= w }
        return Rarity.COMMON
    }

    fun roll(tier: ChestTier, random: Random = Random): Gear {
        val rarity = rollRarity(tier, random)
        val slot = Slot.entries[random.nextInt(Slot.entries.size)]
        return all.first { it.slot == slot && it.rarity == rarity }
    }

    /** XP bonus for a set of [muscleGroup] with [equipped] gear. */
    fun bonusFor(muscleGroup: String, equipped: Collection<Gear>): Double =
        equipped.filter { it.slot.muscleGroup == muscleGroup }.sumOf { it.rarity.xpBonus }.coerceAtMost(MAX_GEAR_BONUS)

    /** Gear score: a quick measure of the whole kit. */
    fun gearScore(equipped: Collection<Gear>): Int = equipped.sumOf { (it.rarity.ordinal + 1) * 10 }
}
