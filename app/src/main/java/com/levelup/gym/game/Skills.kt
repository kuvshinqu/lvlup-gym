package com.levelup.gym.game

/**
 * The hero's skills. Each muscle group an exercise trains feeds one skill,
 * and every set's XP goes to both the hero level and that skill.
 * Icons are drawn in ui/SkillIcon.kt.
 */
data class Skill(
    val muscleGroup: String,
    val name: String,
    val flavor: String,
)

object Skills {
    val all = listOf(
        Skill("Chest", "Shield Wall", "Chest pressing power"),
        Skill("Back", "Bowstring", "Pulling strength"),
        Skill("Legs", "Earthshaker", "Leg strength and stability"),
        Skill("Shoulders", "Skybreaker", "Overhead strength"),
        Skill("Arms", "Iron Grip", "Arm strength"),
        Skill("Core", "Stone Core", "Core stability"),
        Skill("Cardio", "Endurance", "Stamina from cardio"),
    )

    fun forGroup(muscleGroup: String): Skill =
        all.firstOrNull { it.muscleGroup == muscleGroup } ?: Skill(muscleGroup, muscleGroup, "Custom skill")
}
