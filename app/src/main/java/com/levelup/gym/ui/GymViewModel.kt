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
import com.levelup.gym.game.ChestTier
import com.levelup.gym.game.Gear
import com.levelup.gym.game.Loot
import com.levelup.gym.game.Slot
import com.levelup.gym.data.ChestEntity
import com.levelup.gym.data.EquippedEntity
import com.levelup.gym.ui.figure.HeroLook
import com.levelup.gym.game.HistoryExport
import com.levelup.gym.game.Badge
import com.levelup.gym.game.Badges
import com.levelup.gym.game.Deeds
import com.levelup.gym.game.Streak
import com.levelup.gym.data.BadgeEntity
import com.levelup.gym.data.ShieldEntity
import java.time.Instant
import com.levelup.gym.game.WorkoutDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    data class ChestFound(val tier: ChestTier, val reason: String) : GameEvent
}

/** The hero's treasure: chests, items found, gold and what is worn. */
data class Treasury(
    val unopened: List<ChestEntity> = emptyList(),
    val opened: List<ChestEntity> = emptyList(),
    val owned: List<Gear> = emptyList(),
    val equipped: Map<Slot, Gear> = emptyMap(),
    val gold: Int = 0,
) {
    val gearScore: Int get() = Loot.gearScore(equipped.values)
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
            // Rest days pass while the app is closed, so streak milestones can be reached on opening it.
            checkBadges()
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

    // ---------------------------------------------------------------- Loot

    val treasury: StateFlow<Treasury> =
        uid.flatMapLatest { user ->
            kotlinx.coroutines.flow.combine(dao.chests(user), dao.equipped(user), dao.shields(user)) { chests, worn, shields ->
                buildTreasury(chests, worn, shields.sumOf { it.cost })
            }
        }.state(Treasury())

    /** The hero's colours from the gear they wear. */
    val heroLook: StateFlow<HeroLook> = treasury.map { GearLooks.lookFor(it.equipped) }.state(HeroLook.Default)

    private fun buildTreasury(chests: List<ChestEntity>, worn: List<EquippedEntity>, spentOnShields: Int): Treasury {
        val opened = chests.filter { it.openedAt != null }
        val owned = opened.filter { !it.duplicate }.mapNotNull { Loot.gear(it.itemKey) }.distinctBy { it.key }
            .sortedWith(compareBy<Gear>({ it.slot.ordinal }, { -it.rarity.ordinal }))
        val earned = opened.filter { it.duplicate }.sumOf { Loot.gear(it.itemKey)?.rarity?.salvageGold ?: 0 }
        val gold = earned - chests.sumOf { it.cost } - spentOnShields
        val equipped = worn.mapNotNull { e -> Loot.gear(e.itemKey)?.let { g -> g.slot to g } }.toMap()
        return Treasury(chests.filter { it.openedAt == null }, opened, owned, equipped, gold)
    }

    private suspend fun dropChest(tier: ChestTier, source: String, reason: String) {
        val user = userId.value ?: return
        dao.insertChest(ChestEntity(userId = user, tier = tier.name, source = source))
        _events.emit(GameEvent.ChestFound(tier, reason))
    }

    /** Opens a chest and returns the item; a duplicate is melted into gold. New items go on if the slot is empty or weaker. */
    suspend fun openChest(chestId: Long): Pair<Gear, Boolean>? {
        val user = userId.value ?: return null
        val chest = dao.chest(chestId) ?: return null
        if (chest.openedAt != null) return Loot.gear(chest.itemKey)?.let { it to chest.duplicate }
        val gear = Loot.roll(ChestTier.valueOf(chest.tier))
        val ownedKeys = dao.chests(user).first().filter { it.openedAt != null && !it.duplicate }.mapNotNull { it.itemKey }.toSet()
        val duplicate = gear.key in ownedKeys
        dao.openChest(chestId, System.currentTimeMillis(), gear.key, duplicate)
        if (!duplicate) {
            val current = dao.equipped(user).first().firstOrNull { it.slot == gear.slot.name }?.let { Loot.gear(it.itemKey) }
            if (current == null || current.rarity < gear.rarity) dao.equip(EquippedEntity(user, gear.slot.name, gear.key))
        }
        return gear to duplicate
    }

    fun equip(gear: Gear) {
        val user = userId.value ?: return
        viewModelScope.launch { dao.equip(EquippedEntity(user, gear.slot.name, gear.key)) }
    }

    fun buyChest() {
        val user = userId.value ?: return
        viewModelScope.launch {
            if (treasury.value.gold < Loot.CHEST_PRICE) return@launch
            dao.insertChest(ChestEntity(userId = user, tier = ChestTier.WOODEN.name, source = "forge", cost = Loot.CHEST_PRICE))
        }
    }

    // ---------------------------------------------------------------- Streak and badges

    private fun programNow(): Program =
        (Programs.builtIn + customPrograms.value).firstOrNull { it.id == settings.programId.value } ?: Programs.default

    private fun streakOf(starts: List<Long>, shields: List<ShieldEntity>, program: Program): Streak.State {
        val zone = ZoneId.systemDefault()
        return Streak.compute(
            starts.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet(),
            shields.map { Instant.ofEpochMilli(it.boughtAt).atZone(zone).toLocalDate() },
            Streak.restDaysAllowed(program.daysPerWeek),
        )
    }

    val streak: StateFlow<Streak.State> =
        uid.flatMapLatest { user ->
            kotlinx.coroutines.flow.combine(dao.finishedStarts(user), dao.shields(user), activeProgram) { starts, shields, program ->
                streakOf(starts, shields, program)
            }
        }.state(Streak.State())

    private fun deedsOf(stats: UserStats, records: Int, totalXp: Int, starts: List<Long>, longestStreak: Int): Deeds {
        val zone = ZoneId.systemDefault()
        val hours = starts.map { Instant.ofEpochMilli(it).atZone(zone).hour }
        return Deeds(
            longestStreak = longestStreak, workouts = stats.workoutCount, records = records,
            level = Leveling.levelFor(totalXp), volumeKg = stats.volumeKg, cardioSec = stats.cardioSec,
            earlyStarts = hours.count { it in 4 until Badges.EARLY_HOUR },
            lateStarts = hours.count { it >= Badges.LATE_HOUR || it < 4 },
        )
    }

    data class BadgeProgress(val badge: Badge, val earnedAt: Long?, val value: Int) {
        val earned: Boolean get() = earnedAt != null
    }

    val badges: StateFlow<List<BadgeProgress>> =
        uid.flatMapLatest { user ->
            val deeds = kotlinx.coroutines.flow.combine(
                dao.stats(user), dao.recordCount(user), dao.totalXp(user), dao.finishedStarts(user), streak,
            ) { stats, records, xp, starts, st -> deedsOf(stats, records, xp, starts, st.longestEver) }
            kotlinx.coroutines.flow.combine(dao.badges(user), deeds) { earned, d ->
                val at = earned.associate { it.badgeKey to it.earnedAt }
                Badges.all.map { BadgeProgress(it, at[it.key], d.value(it.kind)) }
            }
        }.state(Badges.all.map { BadgeProgress(it, null, 0) })

    /** Hands out any badge the hero has newly earned, each with a chest of its tier. */
    private suspend fun checkBadges() {
        val user = userId.value ?: return
        val starts = dao.finishedStarts(user).first()
        val st = streakOf(starts, dao.shields(user).first(), programNow())
        val deeds = deedsOf(dao.stats(user).first(), dao.recordCount(user).first(), dao.totalXp(user).first(), starts, st.longestEver)
        val have = dao.badges(user).first().map { it.badgeKey }.toSet()
        Badges.earned(deeds).filter { it.key !in have }.forEach { b ->
            if (dao.insertBadge(BadgeEntity(user, b.key)) != -1L) dropChest(b.tier.chest, "badge", "Badge earned: ${b.name}")
        }
    }

    fun buyShield() {
        val user = userId.value ?: return
        viewModelScope.launch {
            if (treasury.value.gold < Streak.SHIELD_PRICE || streak.value.shields >= Streak.MAX_SHIELDS) return@launch
            dao.insertShield(ShieldEntity(userId = user, cost = Streak.SHIELD_PRICE))
            _events.emit(GameEvent.Message("Streak shield forged. It guards one missed day."))
        }
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

    /** The rows on the workout card: planned exercises and sets, plus what the hero added. */
    val draft: StateFlow<WorkoutDraft?> =
        kotlinx.coroutines.flow.combine(activeWorkout, settings.workoutDraftJson) { w, json ->
            if (w == null) null
            else WorkoutDraft.decode(json)?.takeIf { it.workoutId == w.id } ?: WorkoutDraft.fromPlan(w.id, planDay(w.planDayId))
        }.state(null)

    private fun editDraft(change: (WorkoutDraft) -> WorkoutDraft) {
        val d = draft.value ?: return
        settings.setWorkoutDraftJson(change(d).encode())
    }

    fun addSetTo(exerciseName: String, logged: Int) = editDraft { it.addSet(exerciseName, logged) }
    fun removeSetFrom(exerciseName: String, logged: Int) = editDraft { it.removeSet(exerciseName, logged) }
    fun removeExerciseFromWorkout(exerciseName: String) = editDraft { it.removeExercise(exerciseName) }
    fun addExerciseToWorkout(exercise: ExerciseEntity) =
        editDraft { it.addExercise(exercise.name, if (exercise.kind == ExerciseKind.CARDIO) 1 else 3) }

    /** Moves a workout's start and end. A null end keeps a running workout running. */
    fun updateWorkoutTimes(workoutId: Long, startedAt: Long, finishedAt: Long?) {
        viewModelScope.launch {
            dao.updateWorkoutTimes(workoutId, startedAt, finishedAt)
            checkBadges()
        }
    }

    /** "Upper A · Siege of the Upper Keep", for the export. */
    fun workoutLabel(planDayId: String?): String = planDay(planDayId)?.let { "${it.name} · ${it.questName}" } ?: "Free workout"

    /** Writes every set of every finished workout to [uri] as CSV. */
    fun exportHistory(uri: android.net.Uri) {
        val user = userId.value ?: return
        viewModelScope.launch {
            val rows = dao.exportRows(user)
            val csv = HistoryExport.csv(rows, units.value, ::workoutLabel)
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(csv.toByteArray(Charsets.UTF_8)) } != null
                }.getOrDefault(false)
            }
            val workouts = rows.map { it.workoutId }.distinct().size
            _events.emit(GameEvent.Message(if (ok) "Chronicle exported: $workouts workouts, ${rows.size} sets" else "Couldn't write the file"))
        }
    }

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

    /** Best estimated 1RM and best bodyweight reps on an exercise, for the effort preview. */
    suspend fun bestFor(exerciseId: Long): Pair<Double?, Int?> = dao.bestE1rm(exerciseId) to dao.bestBodyweightReps(exerciseId)

    private suspend fun gearBonus(muscleGroup: String): Double {
        val user = userId.value ?: return 0.0
        return Loot.bonusFor(muscleGroup, dao.equipped(user).first().mapNotNull { Loot.gear(it.itemKey) })
    }

    fun logSet(exercise: ExerciseEntity, reps: Int, weightKg: Double) {
        val workout = activeWorkout.value ?: return
        viewModelScope.launch {
            val levelBefore = currentLevel()
            val score = Leveling.scoreSet(
                reps, weightKg,
                bestE1rm = dao.bestE1rm(exercise.id),
                bestReps = dao.bestBodyweightReps(exercise.id),
                setsBeforeInWorkout = dao.setCountForWorkout(workout.id),
                gearBonus = gearBonus(exercise.muscleGroup),
            )
            dao.insertSet(
                SetEntity(
                    workoutId = workout.id, exerciseId = exercise.id,
                    reps = reps, weightKg = weightKg, xp = score.xp, isPersonalRecord = score.personalRecord,
                )
            )
            _events.emit(
                GameEvent.XpGained(
                    score.xp,
                    exercise.name + when {
                        score.warmup -> " · warm-up"
                        else -> " · ${(score.effort.coerceAtMost(1.0) * 100).toInt()}% effort"
                    },
                    score.personalRecord,
                )
            )
            checkLevelUp(levelBefore)
        }
    }

    fun logCardio(exercise: ExerciseEntity, durationSec: Int, distanceM: Double) {
        val workout = activeWorkout.value ?: return
        viewModelScope.launch {
            val levelBefore = currentLevel()
            val xp = Leveling.xpForCardio(durationSec, distanceM, gearBonus(exercise.muscleGroup))
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
            settings.setWorkoutDraftJson(null)
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
            val prs = dao.prCountForWorkout(workout.id)
            if (prs > 0) dropChest(ChestTier.IRON, "workout", "New best in this quest")
            else dropChest(ChestTier.WOODEN, "workout", "Quest complete")
            if (bonus > Leveling.FINISH_WORKOUT_XP) dropChest(ChestTier.GOLDEN, "weekly", "Weekly quest complete")
            checkLevelUp(levelBefore)
        }
    }

    fun discardWorkout() {
        val workout = activeWorkout.value ?: return
        settings.setWorkoutDraftJson(null)
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
        if (now > levelBefore) {
            _events.emit(GameEvent.LevelUp(now, Leveling.title(now)))
            dropChest(ChestTier.IRON, "level", "Reached level $now")
        }
        checkBadges()
    }
}
