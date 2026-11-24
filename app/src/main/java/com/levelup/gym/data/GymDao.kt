package com.levelup.gym.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // Users
    @Query("SELECT * FROM users ORDER BY id LIMIT 1")
    suspend fun firstUser(): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun user(userId: Long): Flow<UserEntity?>

    @Insert
    suspend fun insertUser(user: UserEntity): Long

    @Query("UPDATE users SET name = :name WHERE id = :userId")
    suspend fun renameUser(userId: Long, name: String)

    // Exercises
    @Query("SELECT * FROM exercises WHERE userId = :userId ORDER BY muscleGroup, name COLLATE NOCASE")
    fun exercises(userId: Long): Flow<List<ExerciseEntity>>

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("SELECT COUNT(*) FROM sets WHERE exerciseId = :exerciseId")
    suspend fun setCountForExercise(exerciseId: Long): Int

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExercise(exerciseId: Long)

    // Workouts
    @Query("SELECT * FROM workouts WHERE userId = :userId AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun activeWorkout(userId: Long): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun workout(workoutId: Long): Flow<WorkoutEntity?>

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("UPDATE workouts SET finishedAt = :finishedAt, bonusXp = :bonusXp WHERE id = :workoutId")
    suspend fun finishWorkout(workoutId: Long, finishedAt: Long, bonusXp: Int)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkout(workoutId: Long)

    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId AND finishedAt IS NOT NULL AND startedAt >= :since")
    fun finishedWorkoutCountSince(userId: Long, since: Long): Flow<Int>

    @Query("SELECT planDayId FROM workouts WHERE userId = :userId AND finishedAt IS NOT NULL AND planDayId IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    fun lastPlanDayId(userId: Long): Flow<String?>

    // Sets
    @Insert
    suspend fun insertSet(set: SetEntity): Long

    @Query("DELETE FROM sets WHERE id = :setId")
    suspend fun deleteSet(setId: Long)

    @Query("SELECT COUNT(*) FROM sets WHERE workoutId = :workoutId")
    suspend fun setCountForWorkout(workoutId: Long): Int

    @Query(
        """SELECT s.*, e.name AS exerciseName, e.kind AS exerciseKind FROM sets s
           JOIN exercises e ON e.id = s.exerciseId
           WHERE s.workoutId = :workoutId ORDER BY s.createdAt"""
    )
    fun setsForWorkout(workoutId: Long): Flow<List<SetWithExercise>>

    @Query("SELECT MAX(weightKg) FROM sets WHERE exerciseId = :exerciseId")
    suspend fun bestWeight(exerciseId: Long): Double?

    @Query(
        """SELECT s.* FROM sets s JOIN workouts w ON w.id = s.workoutId
           WHERE s.exerciseId = :exerciseId AND w.userId = :userId
           ORDER BY s.createdAt DESC LIMIT 1"""
    )
    suspend fun lastSetForExercise(userId: Long, exerciseId: Long): SetEntity?

    // Progress
    @Query(
        """SELECT
             (SELECT COALESCE(SUM(s.xp), 0) FROM sets s JOIN workouts w ON w.id = s.workoutId WHERE w.userId = :userId)
           + (SELECT COALESCE(SUM(bonusXp), 0) FROM workouts WHERE userId = :userId)"""
    )
    fun totalXp(userId: Long): Flow<Int>

    @Query(
        """SELECT e.muscleGroup AS muscleGroup, COALESCE(SUM(s.xp), 0) AS xp
           FROM sets s JOIN exercises e ON e.id = s.exerciseId JOIN workouts w ON w.id = s.workoutId
           WHERE w.userId = :userId GROUP BY e.muscleGroup"""
    )
    fun skillXp(userId: Long): Flow<List<SkillXp>>

    @Query(
        """SELECT
             (SELECT COUNT(*) FROM workouts WHERE userId = :userId AND finishedAt IS NOT NULL) AS workoutCount,
             (SELECT COUNT(*) FROM sets s JOIN workouts w ON w.id = s.workoutId WHERE w.userId = :userId) AS setCount,
             (SELECT COALESCE(SUM(s.reps * s.weightKg), 0) FROM sets s JOIN workouts w ON w.id = s.workoutId WHERE w.userId = :userId) AS volumeKg,
             (SELECT COALESCE(SUM(s.durationSec), 0) FROM sets s JOIN workouts w ON w.id = s.workoutId WHERE w.userId = :userId) AS cardioSec"""
    )
    fun stats(userId: Long): Flow<UserStats>

    @Query(
        """SELECT w.id, w.startedAt, w.finishedAt, w.planDayId,
                  COUNT(s.id) AS setCount,
                  COUNT(DISTINCT s.exerciseId) AS exerciseCount,
                  COALESCE(SUM(s.reps * s.weightKg), 0) AS volumeKg,
                  COALESCE(SUM(s.durationSec), 0) AS cardioSec,
                  COALESCE(SUM(s.xp), 0) + w.bonusXp AS xp
           FROM workouts w LEFT JOIN sets s ON s.workoutId = w.id
           WHERE w.userId = :userId AND w.finishedAt IS NOT NULL
           GROUP BY w.id ORDER BY w.startedAt DESC"""
    )
    fun history(userId: Long): Flow<List<WorkoutSummary>>

    // Body weight
    @Insert
    suspend fun insertBodyWeight(entry: BodyWeightEntity): Long

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun deleteBodyWeight(id: Long)

    @Query("SELECT * FROM body_weights WHERE userId = :userId AND loggedAt >= :since ORDER BY loggedAt")
    fun bodyWeightsSince(userId: Long, since: Long): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weights WHERE userId = :userId ORDER BY loggedAt DESC LIMIT 1")
    fun latestBodyWeight(userId: Long): Flow<BodyWeightEntity?>
}
