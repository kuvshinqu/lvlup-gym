package com.levelup.gym.game

/** Badge tiers. Each badge comes with a chest of its tier. */
enum class BadgeTier(val label: String, val chest: ChestTier) {
    BRONZE("Bronze", ChestTier.WOODEN), SILVER("Silver", ChestTier.IRON), GOLD("Gold", ChestTier.GOLDEN),
}

enum class BadgeKind { STREAK, WORKOUTS, RECORDS, LEVEL, VOLUME, CARDIO, EARLY, LATE }

data class Badge(
    val key: String,
    val name: String,
    val description: String,
    val kind: BadgeKind,
    val goal: Int,
    val tier: BadgeTier,
)

/** What the badges are measured against. */
data class Deeds(
    val longestStreak: Int = 0,
    val workouts: Int = 0,
    val records: Int = 0,
    val level: Int = 1,
    val volumeKg: Double = 0.0,
    val cardioSec: Int = 0,
    val earlyStarts: Int = 0,
    val lateStarts: Int = 0,
) {
    fun value(kind: BadgeKind): Int = when (kind) {
        BadgeKind.STREAK -> longestStreak
        BadgeKind.WORKOUTS -> workouts
        BadgeKind.RECORDS -> records
        BadgeKind.LEVEL -> level
        BadgeKind.VOLUME -> (volumeKg / 1000).toInt()
        BadgeKind.CARDIO -> cardioSec / 60
        BadgeKind.EARLY -> earlyStarts
        BadgeKind.LATE -> lateStarts
    }
}

object Badges {
    /** A workout counts as early if it starts before 7:00, late if it starts at 21:00 or after. */
    const val EARLY_HOUR = 7
    const val LATE_HOUR = 21

    private val streakNames = mapOf(
        3 to "Kindled", 7 to "Week of Iron", 14 to "Fortnight Knight", 30 to "Moon Oath",
        60 to "Iron Will", 100 to "Centurion", 180 to "Half-Year Hero", 365 to "Year of Legend",
    )

    val all: List<Badge> = Streak.MILESTONES.map { d ->
        Badge(
            "streak_$d", streakNames.getValue(d), "Keep a $d day streak.", BadgeKind.STREAK, d,
            when { d <= 3 -> BadgeTier.BRONZE; d <= 14 -> BadgeTier.SILVER; else -> BadgeTier.GOLD },
        )
    } + listOf(
        Badge("workouts_1", "First Quest", "Finish your first workout.", BadgeKind.WORKOUTS, 1, BadgeTier.BRONZE),
        Badge("workouts_10", "Sworn Squire", "Finish 10 workouts.", BadgeKind.WORKOUTS, 10, BadgeTier.SILVER),
        Badge("workouts_50", "Veteran", "Finish 50 workouts.", BadgeKind.WORKOUTS, 50, BadgeTier.GOLD),
        Badge("workouts_100", "Champion", "Finish 100 workouts.", BadgeKind.WORKOUTS, 100, BadgeTier.GOLD),
        Badge("records_1", "Breakthrough", "Set your first new best.", BadgeKind.RECORDS, 1, BadgeTier.BRONZE),
        Badge("records_10", "Record Breaker", "Set 10 new bests.", BadgeKind.RECORDS, 10, BadgeTier.SILVER),
        Badge("records_50", "Limit Breaker", "Set 50 new bests.", BadgeKind.RECORDS, 50, BadgeTier.GOLD),
        Badge("level_5", "Knighted", "Reach hero level 5.", BadgeKind.LEVEL, 5, BadgeTier.SILVER),
        Badge("level_10", "Lord of Iron", "Reach hero level 10.", BadgeKind.LEVEL, 10, BadgeTier.GOLD),
        Badge("volume_10", "Mountain Mover", "Lift 10 tonnes in total.", BadgeKind.VOLUME, 10, BadgeTier.SILVER),
        Badge("volume_100", "Titan", "Lift 100 tonnes in total.", BadgeKind.VOLUME, 100, BadgeTier.GOLD),
        Badge("cardio_600", "Long Road", "Do 10 hours of cardio.", BadgeKind.CARDIO, 600, BadgeTier.SILVER),
        Badge("early_1", "Dawn Patrol", "Start a workout before 7:00.", BadgeKind.EARLY, 1, BadgeTier.BRONZE),
        Badge("late_1", "Night Watch", "Start a workout at 21:00 or later.", BadgeKind.LATE, 1, BadgeTier.BRONZE),
    )

    fun byKey(key: String): Badge? = all.firstOrNull { it.key == key }

    fun earned(deeds: Deeds): List<Badge> = all.filter { deeds.value(it.kind) >= it.goal }
}
