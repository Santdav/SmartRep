# STATE (updated 2026-10-20)

Package: `com.santda.smartrep` (DB file `smart_rep.db`). Spec: `HANDOFF.md`. Checklist: `MILESTONES.md`.

## Status
- Phase 0 done.
- M1 done so far: schema v1 (all entities, `ExerciseDao`, `WorkoutDao`, `AppDatabase`) and the **seed objective** (`seed_exercises.json` with 219 exercises, `SeedExercise`/`SeedParser`, `SeedLoader`, `SeedDataTest`, `SeedLoaderTest`). Seed data was checked against a real Hevy export: all 100 CSV titles resolve by name or alias.
- Not started in M1: repositories (active workout, finish/discard), unit converters, UI screens (exercise list, active workout, history).
- Known gap: `SeedLoader.loadIfNeeded()` is not called anywhere yet (no `Application` class / Hilt setup). Wire it at startup when that exists.
- Next objective: active-workout repository (single active workout, write-through mutations, finish/discard), using `WorkoutDao`.

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
// SeedExercise(seedKey, name, aliases = [], equipment, exerciseType, primary, secondary = [])  // aliases are JSON-only, not in Room
```

## Known issues / TODO
- `SeedLoader` is not wired into app startup yet (needs the `Application` class + Hilt).
- `@Relation` lists are unordered: sort by `position` in repositories.
- `finishWorkout` transaction lives in `WorkoutDao`; the warning dialog logic stays in the repository/ViewModel.

## Hevy CSV facts for M4 (from a real export)
- Columns: title, start_time, end_time, description, exercise_title, superset_id, exercise_notes, set_index, set_type, weight_kg, reps, distance_km, duration_seconds, rpe.
- Dates like `30 Sep 2026, 07:10`: no seconds, no timezone. Import them as-is (`Locale.ENGLISH`, device zone, store epoch millis); any timezone handling is for display only and can change later.
- Weight is already kg; distance is **km** (x1000 for meters); rpe was empty in the sample; set_type seen: normal, warmup, failure (verify the drop-set string on a real export).
- `superset_id` is per workout: different workouts reuse the same ids, so key it by workout.
- Start a new `workout_exercise` when the title changes OR `set_index` does not increase (same exercise can repeat in separate blocks, or back-to-back with set_index back at 0).
- Match each `exercise_title` case-insensitively against seed `name` OR `aliases`, resolve to the row via `seedKey`; unmatched titles become custom exercises.

## Gotchas
- Seed names: "Exercise (Equipment)" for equipment exercises, plain name for bodyweight (`NONE`), "Weighted X" for weighted bodyweight. Spelling: "Push Up" (no hyphen), "Bicep Curl", "Farmers Walk". Hevy's own titles live in `aliases`. Never rename a `seedKey` once shipped (they were regenerated from the names before first release).
- `exercise_muscle` PK is (exerciseId, muscle): a muscle cannot be both primary and secondary.