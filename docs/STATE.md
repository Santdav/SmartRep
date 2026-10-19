# STATE (updated 2026-10-19)

Package: `com.santda.smartrep` (DB file `smart_rep.db`). Spec: `HANDOFF.md`. Checklist: `MILESTONES.md`.

## Status
- Phase 0 done. M1: schema v1 entities + `ExerciseDao` + `WorkoutDao` + `AppDatabase` done.
- M1 seed: `seed_exercises.json` (174 exercises), `SeedLoader`, tests written; not yet wired into app startup.
- Next: wire `SeedLoader.loadIfNeeded()` at startup (Hilt), then repositories (active workout) and UI.

## Deviations from HANDOFF.md (code wins; see DECISIONS.md #1)
- `SetType.WARMUP` (spec says `WARM_UP`).
- `Equipment`: has PLATE and MACHINE; no BODYWEIGHT (bodyweight uses `NONE`).
- `Muscle`: `ABDOMINAL` (spec: ABS), no OBLIQUES, added NECK, TRAPS, OTHER.
- `ExerciseType`: spec's `WEIGHT` and `DISTANCE` are replaced by a single `WEIGHT_DISTANCE` (8 types total).

## Contracts (package `com.santda.smartrep.domain.model`)
```kotlin
enum class Equipment { NONE, BARBELL, DUMBBELL, KETTLEBELL, CABLE, PLATE, MACHINE, RESISTANCE_BAND, OTHER }
enum class ExerciseType { WEIGHT_REPS, BODYWEIGHT_REPS, WEIGHTED_BODYWEIGHT, ASSISTED_BODYWEIGHT,
    DURATION, DURATION_WEIGHT, DISTANCE_DURATION, WEIGHT_DISTANCE }
enum class Muscle { ABDOMINAL, ABDUCTORS, ADDUCTORS, BICEPS, CALVES, CARDIO, CHEST, FOREARMS, FULL_BODY,
    GLUTES, HAMSTRINGS, LATS, LOWER_BACK, NECK, QUADRICEPS, SHOULDERS, TRAPS, TRICEPS, UPPER_BACK, OTHER }
enum class MuscleRole { PRIMARY, SECONDARY }
enum class SetType { NORMAL, WARMUP, FAILURE, DROP }
```
Entities (`com.santda.smartrep.data.local`): fields exactly as HANDOFF section 5, enums stored by name, no TypeConverters.
Extra constraints: unique index `exercise(seedKey)`, unique index `workout(startedAt, name)`,
`routine_exercise`/`workout_exercise` -> `exercise` is `RESTRICT`.

```kotlin
abstract class ExerciseDao {   // writes
    suspend fun insert(e: ExerciseEntity): Long                      // IGNORE, -1 if duplicate
    suspend fun insertWithMuscles(e: ExerciseEntity, m: List<ExerciseMuscleEntity>): Long  // @Transaction, -1 if ignored
    suspend fun updateWithMuscles(e: ExerciseEntity, m: List<ExerciseMuscleEntity>)
    suspend fun setArchived(exerciseId: Long, archived: Boolean)
    // reads
    fun observeActive(query: String = ""): Flow<List<ExerciseWithMuscles>>
    fun observeById(id: Long): Flow<ExerciseWithMuscles?>
    suspend fun getById(id: Long): ExerciseEntity?
    suspend fun getAllSeedKeys(): List<String>
    suspend fun findByName(name: String): ExerciseEntity?            // NOCASE, includes archived
}
abstract class WorkoutDao {    // key members
    suspend fun insertWorkout(w: WorkoutEntity): Long
    suspend fun insertWorkoutOrIgnore(w: WorkoutEntity): Long        // Hevy import, -1 if exists
    suspend fun countActive(): Int
    fun observeActive(): Flow<WorkoutWithExercises?>
    fun observeById(id: Long): Flow<WorkoutWithExercises?>
    fun observeHistory(): Flow<List<WorkoutEntity>>
    suspend fun countIncompleteSets(workoutId: Long): Int
    suspend fun finishWorkout(workoutId: Long, endedAt: Long): FinishResult  // SAVED | DELETED_EMPTY
}
class SeedLoader { suspend fun loadIfNeeded(): Int }   // data/seed, inserts only new seedKeys
```

## Known issues / TODO
- `@Relation` lists are unordered: sort by `position` in repositories.
- `finishWorkout` transaction lives in `WorkoutDao`; the warning dialog logic stays in the repository/ViewModel.

## Gotchas
- Seed names follow "Name (Equipment)" so Hevy import (case-insensitive match) can hit seed rows. Never rename a `seedKey`.
- `exercise_muscle` PK is (exerciseId, muscle): a muscle cannot be both primary and secondary.
