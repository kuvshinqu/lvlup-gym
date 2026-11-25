package com.levelup.gym.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Every piece of data belongs to a user (the "hero"), so more people can be added later,
 * each with their own exercises, workouts and XP. v1 uses a single local hero.
 * All weights are stored in kg and distances in metres; the UI converts for display.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class ExerciseKind { STRENGTH, CARDIO }

@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    /** Also the hero skill this exercise trains (see game/Skills.kt). */
    val muscleGroup: String,
    val kind: ExerciseKind = ExerciseKind.STRENGTH,
)

@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val startedAt: Long = System.currentTimeMillis(),
    /** null while the workout is still in progress. */
    val finishedAt: Long? = null,
    /** XP granted for finishing the workout (and the weekly quest, if it completed it). */
    val bonusXp: Int = 0,
    /** Which program day this workout follows, e.g. "upper_a". null for a free workout. */
    val planDayId: String? = null,
)

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(WorkoutEntity::class, ["id"], ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    /** Strength sets */
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    /** Cardio sets */
    val durationSec: Int = 0,
    val distanceM: Double = 0.0,
    val xp: Int,
    val isPersonalRecord: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class SetWithExercise(
    @Embedded val set: SetEntity,
    @ColumnInfo(name = "exerciseName") val exerciseName: String,
    @ColumnInfo(name = "exerciseKind") val exerciseKind: ExerciseKind,
)

data class WorkoutSummary(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long?,
    val planDayId: String?,
    val setCount: Int,
    val exerciseCount: Int,
    val volumeKg: Double,
    val cardioSec: Int,
    val xp: Int,
)

/** One logged set with its workout, for the history export. */
data class ExportRow(
    val workoutId: Long,
    val startedAt: Long,
    val finishedAt: Long?,
    val planDayId: String?,
    val bonusXp: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val exerciseKind: ExerciseKind,
    val reps: Int,
    val weightKg: Double,
    val durationSec: Int,
    val distanceM: Double,
    val xp: Int,
    val isPersonalRecord: Boolean,
    val createdAt: Long,
)

data class UserStats(
    val workoutCount: Int,
    val setCount: Int,
    val volumeKg: Double,
    val cardioSec: Int,
)

data class SkillXp(
    val muscleGroup: String,
    val xp: Int,
)

/** Body weight weigh-ins. Shown on the hero screen; never affects XP. */
@Entity(
    tableName = "body_weights",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val weightKg: Double,
    val loggedAt: Long = System.currentTimeMillis(),
)

/**
 * A loot chest. Earned by finishing workouts, the weekly quest and level ups, or bought with gold.
 * Opening it fills [itemKey] (see game/Loot.kt); a duplicate item is melted into gold ([duplicate]).
 */
@Entity(
    tableName = "chests",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class ChestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    /** ChestTier name. */
    val tier: String,
    /** Why it dropped: workout, weekly, level, forge. */
    val source: String,
    val earnedAt: Long = System.currentTimeMillis(),
    val openedAt: Long? = null,
    val itemKey: String? = null,
    val duplicate: Boolean = false,
    /** Gold paid for it at the forge. */
    val cost: Int = 0,
)

/** Which item the hero wears in each slot. */
@Entity(
    tableName = "equipped",
    primaryKeys = ["userId", "slot"],
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
)
data class EquippedEntity(
    val userId: Long,
    val slot: String,
    val itemKey: String,
)

/** A badge the hero has earned. Kept so its chest is only handed out once. */
@Entity(
    tableName = "badges",
    primaryKeys = ["userId", "badgeKey"],
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
)
data class BadgeEntity(
    val userId: Long,
    val badgeKey: String,
    val earnedAt: Long = System.currentTimeMillis(),
)

/** A streak shield bought at the forge. Shields from milestones are worked out from the streak itself. */
@Entity(
    tableName = "shields",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class ShieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val boughtAt: Long = System.currentTimeMillis(),
    val cost: Int,
)
