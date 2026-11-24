package com.levelup.gym.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.levelup.gym.GymApp
import com.levelup.gym.data.BodyWeightEntity
import com.levelup.gym.data.DEFAULT_EXERCISES
import com.levelup.gym.data.ExerciseEntity
import com.levelup.gym.data.SetEntity
import com.levelup.gym.data.SetWithExercise
import com.levelup.gym.data.UserEntity
import com.levelup.gym.data.UserStats
import com.levelup.gym.data.WorkoutEntity
import com.levelup.gym.data.WorkoutSummary
import com.levelup.gym.data.ExerciseKind
import com.levelup.gym.data.Units
import com.levelup.gym.game.Leveling
import com.levelup.gym.game.PlanDay
import com.levelup.gym.game.Skill
import com.levelup.gym.game.Skills
import com.levelup.gym.game.Programs
import com.levelup.gym.game.Program
import com.levelup.gym.game.ProgramStore
import com.levelup.gym.game.ExerciseLibrary
import com.levelup.gym.game.LibraryExercise
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GameEvent {
    data class XpGained(val xp: Int, val label: String, val personalRecord: Boolean = false) : GameEvent
    data class LevelUp(val level: Int, val title: String) : GameEvent
    data class Message(val text: String) : GameEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class GymViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as GymApp).db.dao()
    private val settings = (app as GymApp).settings
    val units: StateFlow<Units> = settings.units
    fun setUnits(units: Units) = settings.setUnits(units)

    /** The current user. Only one for now; a user picker can switch this later. */
    private val userId = MutableStateFlow<Long?>(null)
    private val uid = userId.filterNotNull()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events

    init {
        viewModelScope.launch {
            val user = dao.firstUser() ?: run {
                val id = dao.insertUser(UserEntity(name = "Hero"))
                dao.insertExercises(DEFAULT_EXERCISES.map {
                    ExerciseEntity(userId = id, name = it.name, muscleGroup = it.muscleGroup, kind = it.kind)
                })
                UserEntity(id = id, name = "Hero")
            }
            userId.value = user.id
        }
    }

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val progress: StateFlow<Leveling.Progress> =
        uid.flatMapLatest { dao.totalXp(it) }.map { Leveling.progress(it) }.state(Leveling.progress(0))

    val heroName: StateFlow<String> =
        uid.flatMapLatest { dao.user(it) }.map { it?.name ?: "Hero" }.state("Hero")

    fun renameHero(name: String) {
        val user = userId.value ?: return
        val clean = name.trim().take(24)
        if (clean.isNotEmpty()) viewModelScope.launch { dao.renameUser(user, clean) }
    }

    data class SkillProgress(val skill: Skill, val progress: Leveling.Progress)

    val skills: StateFlow<List<SkillProgress>> =
        uid.flatMapLatest { dao.skillXp(it) }.map { rows ->
            val byGroup = rows.associate { it.muscleGroup to it.xp }
            val known = Skills.all.map { SkillProgress(it, Leveling.progress(byGroup[it.muscleGroup] ?: 0, Leveling.SKILL_STEP)) }
            val custom = rows.filter { r -> Skills.all.none { it.muscleGroup == r.muscleGroup } }
                .map { SkillProgress(Skills.forGroup(it.muscleGroup), Leveling.progress(it.xp, Leveling.SKILL_STEP)) }
            known + custom
        }.state(Skills.all.map { SkillProgress(it, Leveling.progress(0, Leveling.SKILL_STEP)) })

    private fun weekStartMillis(): Long =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Finished workouts since Monday, for the weekly quest. */
    val workoutsThisWeek: StateFlow<Int> =
        uid.flatMapLatest { dao.finishedWorkoutCountSince(it, weekStartMillis()) }.state(0)

    /** The hero's own programs. */
    val customPrograms: StateFlow<List<Program>> =
        settings.customProgramsJson.map { ProgramStore.decode(it) }.state(ProgramStore.decode(settings.customProgramsJson.value))

    /** The quest line the hero follows. */
    val activeProgram: StateFlow<Program> =
        kotlinx.coroutines.flow.combine(settings.programId, customPrograms) { id, custom ->
            (Programs.builtIn + custom).firstOrNull { it.id == id } ?: Programs.default
        }.state(Programs.default)

    val quizDone: StateFlow<Boolean> = settings.quizDone

    fun chooseProgram(id: String) = settings.setProgramId(id)

    fun finishQuiz(programId: String) {
        settings.setQuizDone()
        chooseProgram(programId)
    }

    fun skipQuiz() = settings.setQuizDone()

    fun planDay(id: String?): PlanDay? = Programs.day(id, customPrograms.value)

    fun saveCustomProgram(program: Program) {
        val list = customPrograms.value.filter { it.id != program.id } + program.copy(custom = true)
        settings.setCustomProgramsJson(ProgramStore.encode(list))
        viewModelScope.launch { ensureExercises(program.days.flatMap { d -> d.exercises.map { it.exerciseName } }) }
    }

    fun deleteCustomProgram(id: String) {
        settings.setCustomProgramsJson(ProgramStore.encode(customPrograms.value.filter { it.id != id }))
        if (settings.programId.value == id) settings.setProgramId(Programs.default.id)
    }

    val nextPlanDay: StateFlow<PlanDay> =
        kotlinx.coroutines.flow.combine(uid.flatMapLatest { dao.lastPlanDayId(it) }, activeProgram) { last, program ->
            program.nextDay(last)
        }.state(Programs.default.days.first())

    /** Adds any named exercises the hero doesn't have yet, taking details from the library. */
    private suspend fun ensureExercises(names: List<String>) {
        val user = userId.value ?: return
        val have = dao.exercises(user).first().map { it.name.lowercase() }.toSet()
        val missing = names.distinct().filter { it.lowercase() !in have }
        if (missing.isEmpty()) return
        dao.insertExercises(missing.map { n ->
            val lib = ExerciseLibrary.find(n)
            ExerciseEntity(userId = user, name = lib?.name ?: n, muscleGroup = lib?.muscleGroup ?: "Other", kind = lib?.kind ?: ExerciseKind.STRENGTH)
        })
    }

    fun addFromLibrary(entry: LibraryExercise) {
        viewModelScope.launch { ensureExercises(listOf(entry.name)) }
    }

    /** Weigh-ins from the last 30 days, oldest first, for the trend line. */
    val bodyWeights30d: StateFlow<List<BodyWeightEntity>> =
        uid.flatMapLatest { dao.bodyWeightsSince(it, System.currentTimeMillis() - 30L * 24 * 3600 * 1000) }.state(emptyList())

    val latestBodyWeight: StateFlow<BodyWeightEntity?> =
        uid.flatMapLatest { dao.latestBodyWeight(it) }.state(null)

    fun logBodyWeight(weightKg: Double) {
        val user = userId.value ?: return
        if (weightKg <= 0) return
        viewModelScope.launch { dao.insertBodyWeight(BodyWeightEntity(userId = user, weightKg = weightKg)) }
    }

    fun deleteBodyWeight(id: Long) {
        viewModelScope.launch { dao.deleteBodyWeight(id) }
    }

    suspend fun bestWeight(exerciseId: Long): Double? = dao.bestWeight(exerciseId)

    val stats: StateFlow<UserStats> =
        uid.flatMapLatest { dao.stats(it) }.state(UserStats(0, 0, 0.0, 0))

    val exercises: StateFlow<List<ExerciseEntity>> =
        uid.flatMapLatest { dao.exercises(it) }.state(emptyList())

    val history: StateFlow<List<WorkoutSummary>> =
        uid.flatMapLatest { dao.history(it) }.state(emptyList())

    val activeWorkout: StateFlow<WorkoutEntity?> =
        uid.flatMapLatest { dao.activeWorkout(it) }.state(null)

    val activeSets: StateFlow<List<SetWithExercise>> =
        activeWorkout.flatMapLatest { w -> if (w == null) flowOf(emptyList()) else dao.setsForWorkout(w.id) }
            .state(emptyList())

    fun workout(id: Long): Flow<WorkoutEntity?> = dao.workout(id)
    fun setsFor(workoutId: Long): Flow<List<SetWithExercise>> = dao.setsForWorkout(workoutId)

    /** Starts a workout, optionally following a program day. */
    fun startWorkout(planDayId: String? = null) {
        val user = userId.value ?: return
        viewModelScope.launch {
            planDay(planDayId)?.let { day -> ensureExercises(day.exercises.map { it.exerciseName }) }
            if (activeWorkout.value == null) dao.insertWorkout(WorkoutEntity(userId = user, planDayId = planDayId))
        }
    }

    suspend fun lastSet(exerciseId: Long): SetEntity? =
        userId.value?.let { dao.lastSetForExercise(it, exerciseId) }

    fun logSet(exercise: ExerciseEntity, reps: Int, weightKg: Double) {
        val workout = activeWorkout.value ?: return
        viewModelScope.launch {
            val levelBefore = currentLevel()
            val pr = Leveling.isPersonalRecord(weightKg, dao.bestWeight(exercise.id))
            val xp = Leveling.xpForSet(reps, weightKg, pr)
            dao.insertSet(
                SetEntity(
                    workoutId = workout.id, exerciseId = exercise.id,
                    reps = reps, weightKg = weightKg, xp = xp, isPersonalRecord = pr,
                )
            )
            _events.emit(GameEvent.XpGained(xp, exercise.name, pr))
            checkLevelUp(levelBefore)
        }
    }

    fun logCardio(exercise: ExerciseEntity, durationSec: Int, distanceM: Double) {
        val workout = activeWorkout.value ?: return
        viewModelScope.launch {
            val levelBefore = currentLevel()
            val xp = Leveling.xpForCardio(durationSec, distanceM)
            dao.insertSet(
                SetEntity(
                    workoutId = workout.id, exerciseId = exercise.id,
                    durationSec = durationSec, distanceM = distanceM, xp = xp,
                )
            )
            _events.emit(GameEvent.XpGained(xp, exercise.name))
            checkLevelUp(levelBefore)
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch { dao.deleteSet(setId) }
    }

    fun finishWorkout() {
        val workout = activeWorkout.value ?: return
        viewModelScope.launch {
            if (dao.setCountForWorkout(workout.id) == 0) {
                dao.deleteWorkout(workout.id)
                _events.emit(GameEvent.Message("Empty workout discarded"))
                return@launch
            }
            val levelBefore = currentLevel()
            val weekCount = dao.finishedWorkoutCountSince(workout.userId, weekStartMillis()).first() +
                if (workout.startedAt >= weekStartMillis()) 1 else 0
            val bonus = Leveling.finishBonus(weekCount)
            dao.finishWorkout(workout.id, System.currentTimeMillis(), bonus)
            _events.emit(
                GameEvent.XpGained(
                    bonus,
                    if (bonus > Leveling.FINISH_WORKOUT_XP) "Weekly quest complete!" else "Quest complete",
                )
            )
            checkLevelUp(levelBefore)
        }
    }

    fun discardWorkout() {
        val workout = activeWorkout.value ?: return
        viewModelScope.launch { dao.deleteWorkout(workout.id) }
    }

    fun deleteWorkout(workoutId: Long) {
        viewModelScope.launch { dao.deleteWorkout(workoutId) }
    }

    fun addExercise(name: String, muscleGroup: String, kind: ExerciseKind) {
        val user = userId.value ?: return
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            dao.insertExercise(ExerciseEntity(userId = user, name = clean, muscleGroup = muscleGroup, kind = kind))
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            if (dao.setCountForExercise(exercise.id) > 0) {
                _events.emit(GameEvent.Message("${exercise.name} has logged sets, so it's kept"))
            } else {
                dao.deleteExercise(exercise.id)
            }
        }
    }

    private suspend fun currentLevel(): Int {
        val user = userId.value ?: return 1
        return Leveling.levelFor(dao.totalXp(user).first())
    }

    private suspend fun checkLevelUp(levelBefore: Int) {
        val now = currentLevel()
        if (now > levelBefore) _events.emit(GameEvent.LevelUp(now, Leveling.title(now)))
    }
}
