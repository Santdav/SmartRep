# Handoff: Offline Workout Tracker (Android)

This document is the specification for building the app. It is written for a coding agent working in an existing Android Studio project (Kotlin, Jetpack Compose).

## 0. How to work

1. Read this whole document before writing code.
2. Build one milestone at a time (Section 10). Each milestone must end with a runnable app and passing tests.
3. Do not add features, libraries, or permissions that are not in this document. If something is ambiguous or missing, choose the simplest option consistent with this document, note the assumption in `docs/DECISIONS.md`, and continue. Ask only when a wrong guess would be expensive to undo (database schema, data-loss risks).
4. Add dependencies (Room, Hilt, DataStore, etc.) when the milestone that needs them begins, not before.
5. Use a Gradle version catalog (`libs.versions.toml`) and KSP (not kapt).
6. Keep a short `docs/DECISIONS.md` log: decision, alternatives, reason, 3 to 5 lines each.
7. Use the package name and app name already in the project. The app name in docs is a working title.

## 1. Product summary

A personal, Android-only, fully offline clone of Hevy's core workout logging. No accounts, no social features, no paywall limits. It keeps Hevy's responsive logging feel.

## 2. Hard constraints

- Fully offline. **The merged manifest must not contain the `INTERNET` permission.** No network libraries (no Retrofit/OkHttp), no analytics, no Firebase.
- No signup or login. No social features (posts, follows, feeds, likes, comments).
- No Google or Apple integrations.
- Unlimited routines, folders, and custom exercises.
- Android only. Single module.
- User data must never be lost to a schema change: export Room schemas, write real migrations, **never use `fallbackToDestructiveMigration`**.

## 3. Tech stack

| Area | Choice |
|---|---|
| Language/UI | Kotlin, Jetpack Compose, Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| Async/state | Coroutines, Flow, `StateFlow` in ViewModels |
| Database | Room with KSP |
| DI | Hilt |
| Settings | DataStore (Preferences) |
| Serialization | kotlinx.serialization (backup file, seed asset) |
| CSV | kotlin-csv or Apache Commons CSV (never split on commas by hand) |
| Charts | Vico (Compose) |
| Lists | Paging 3 for history, only once needed |
| Tests | JUnit, Turbine, Room in-memory tests, Compose UI tests |

Do not use: Retrofit/OkHttp, Firebase, KMP/Compose Multiplatform, multi-module setup, kapt.

## 4. Domain rules

### 4.1 Exercise

Each exercise has:
- `name`
- `equipment` (enum)
- `exerciseType` (enum, Section 4.2)
- primary muscle groups and secondary muscle groups (enum values, zero or more secondary)
- `isCustom` flag, `isArchived` flag

Equipment and muscle groups are fixed enums in code, stored as text. Do not create lookup tables for them in v1. Choose a sensible equipment list (for example: none, barbell, dumbbell, machine, cable, kettlebell, resistance band, bodyweight/other) and a sensible muscle list (for example: chest, upper back, lats, shoulders, biceps, triceps, forearms, abs, obliques, lower back, glutes, quadriceps, hamstrings, calves, adductors, abductors, cardio/full body) and record it in `DECISIONS.md`.

Exercises are **soft-deleted** (`isArchived = true`). History must never lose its exercise.

### 4.2 Exercise types and the fields they use

| Type | Fields used per set |
|---|---|
| `WEIGHT_REPS` | weight, reps |
| `BODYWEIGHT_REPS` | reps |
| `WEIGHTED_BODYWEIGHT` | added weight, reps |
| `ASSISTED_BODYWEIGHT` | assist weight, reps |
| `DURATION` | duration |
| `DURATION_WEIGHT` | weight, duration |
| `DISTANCE_DURATION` | distance, duration |
| `WEIGHT` | weight |
| `DISTANCE` | distance |

The exercise type decides which input fields the logging UI shows and which are required to mark a set complete.

Assisted weight is stored as a **positive** number; the type gives it its meaning. Never store negative weights.

### 4.3 Units

Store canonical units: weight in **kg**, distance in **meters**, duration in **seconds**. Convert only for display, based on a unit setting (kg/lb, and km/mi) in DataStore.

### 4.4 Set types

Enum `SetType`: `NORMAL`, `WARM_UP`, `FAILURE`, `DROP`. Each is just a flag on the set.

**Warm-up sets are included in volume and set counts.** Keep the filter in one place (a single function or query parameter) so the rule is easy to change later.

### 4.5 Routine targets

Each routine exercise has `targetSets` (N) and a rep range `repMin`-`repMax` (X-Y), the same model Hevy uses. Targets are not per set. The rep range applies to rep-based types only; for other types a routine stores only the set count in v1.

### 4.6 Supersets

- `supersetGroup` is a nullable integer on both `routine_exercise` and `workout_exercise`. Exercises sharing the same value within a routine or workout are one superset.
- **Invariant:** members of a group are adjacent in `position` order. The repository layer enforces it (when reordering, removing, or linking), so the UI can never create a broken group.
- **UI:** each exercise has an "add to superset" action that links it with the exercise above or below. Removing an exercise from a group of two dissolves the group.
- **Logging:** the screen alternates between the group's exercises. The rest timer (if enabled) starts after the last exercise of each round.

### 4.7 Rest timer (optional)

- `restSeconds` is nullable on `routine_exercise` and `workout_exercise`; a global default lives in DataStore. `null` means no timer.
- Store the timer's **end timestamp** (epoch millis), not a countdown. The UI recomputes remaining time, so it survives backgrounding and process death.
- Signal completion with a notification. Choose between a foreground service and an alarm-based approach in Milestone 3 after a short spike, and record the choice. Request the notification permission at runtime on Android 13+. This is not a network permission.

### 4.8 Volume and personal records (Milestone 5)

- Volume per set = weight x reps for `WEIGHT_REPS` and `WEIGHTED_BODYWEIGHT` (added weight). Other types show reps, duration, or distance totals instead. Warm-up sets are included.
- Estimated 1RM (Epley: weight x (1 + reps / 30)) for `WEIGHT_REPS`.
- Per-exercise records: heaviest weight, best estimated 1RM, best single-set volume, most reps.
- Bodyweight and assisted volume are not computed in v1 (no body weight data).

## 5. Data model

All IDs are auto-generated `Long`. All timestamps are epoch millis. Enums are stored as text.

**exercise**
`id`, `seedKey` (nullable, unique; stable key for bundled exercises), `name`, `equipment`, `exerciseType`, `isCustom`, `isArchived`

**exercise_muscle**
`exerciseId`, `muscle`, `role` (`PRIMARY` | `SECONDARY`). Composite primary key (`exerciseId`, `muscle`).

**routine_folder**
`id`, `name`, `sortOrder`

**routine**
`id`, `folderId` (nullable), `name`, `notes`, `sortOrder`, `createdAt`, `updatedAt`

**routine_exercise**
`id`, `routineId`, `exerciseId`, `position`, `targetSets`, `repMin` (nullable), `repMax` (nullable), `restSeconds` (nullable), `supersetGroup` (nullable), `notes`

**workout**
`id`, `routineId` (nullable, set null on routine delete), `name`, `notes`, `startedAt`, `endedAt` (nullable)

**workout_exercise**
`id`, `workoutId`, `exerciseId`, `position`, `supersetGroup` (nullable), `restSeconds` (nullable), `notes`

**workout_set**
`id`, `workoutExerciseId`, `position`, `setType`, `weightKg` (nullable), `reps` (nullable), `durationSec` (nullable), `distanceM` (nullable), `completed`, `completedAt` (nullable)

Notes:
- One flat `workout_set` table with nullable value columns. The exercise type, not the schema, decides which columns are used.
- Foreign keys with cascade deletes from workout down to sets. Exercises are never hard-deleted.
- Add indexes on foreign keys and on `workout.startedAt`.

### History immutability

Starting a workout from a routine **copies** the routine's structure into `workout`, `workout_exercise`, and `workout_set` rows. Editing or deleting a routine later must never change past workouts.

## 6. Architecture

- Single Gradle module, packages by feature.
- Compose screens -> ViewModels (expose `StateFlow`) -> repositories -> Room DAOs.
- Unidirectional data flow: UI sends events up, observes state down.
- No separate use-case layer, except pure-Kotlin logic in `domain/` where it has real rules: volume, estimated 1RM, PRs, superset invariants, unit conversion.
- CSV import and the seed loader live in their own testable packages.

Suggested layout:

```
data/      entities, DAOs, AppDatabase, repositories, seed loader
domain/    pure Kotlin models and logic
ui/        screens + ViewModels per feature (workout, routines, exercises, history, settings)
importer/  Hevy CSV import
di/        Hilt modules
```

## 7. Key behaviors

### 7.1 Active workout (the most important requirement)

- A workout with `endedAt == null` is the active workout. **At most one may exist**; the repository enforces this.
- Write every change (add exercise, edit a field, complete a set) to Room immediately. Never hold workout state only in memory. The screen observes the database as a `Flow`.
- After app kill, rotation, or process death, reopening the app returns the user to the active workout exactly as it was.
- Finishing sets `endedAt`. If incomplete sets exist, ask the user to either complete them or discard them (proposed default). Discarding a workout deletes it after confirmation.

### 7.2 Prefill

When an exercise is added to a workout, prefill its sets from the last completed session of that exercise (weight, reps, set count). If none exists, start empty and show the routine's rep range as a placeholder.

### 7.3 Responsiveness

- No spinners for local data, and no blocking work on the main thread.
- Numeric keyboards for weight, reps, distance; large touch targets for set rows and the complete checkbox.
- Lists stay smooth with years of history (lazy lists, paging when needed).
- Setting changes (units, default rest) apply immediately.

## 8. Hevy CSV import (Milestone 4)

**First step: inspect a real export file** supplied by the user and confirm the column layout before coding the mapper. The columns below are expected, not guaranteed; treat them as assumptions to verify.

Expected columns: `title`, `start_time`, `end_time`, `description`, `exercise_title`, `superset_id`, `exercise_notes`, `set_index`, `set_type`, a weight column (kg or lbs), `reps`, a distance column (km or miles), `duration_seconds`, `rpe`.

Rules:
- Each CSV row is one set. Group rows into workouts by (`title`, `start_time`), and into workout exercises by (`exercise_title`, order of appearance).
- Parse dates according to the real file's format; store epoch millis.
- Convert weights to kg and distances to meters regardless of the file's units.
- Map `set_type` to `SetType` (normal, warm-up, failure, drop). Unknown values become `NORMAL` and are reported.
- Map `superset_id` to `supersetGroup` (renumber per workout; enforce adjacency).
- Match exercises by case-insensitive name against the library. If no match, create a custom exercise, inferring `exerciseType` from which columns are populated (for example weight + reps -> `WEIGHT_REPS`, only reps -> `BODYWEIGHT_REPS`, only duration -> `DURATION`). Equipment and muscles for newly created exercises are left unspecified for the user to edit.
- Columns with no home in the schema (for example `rpe`) are ignored and listed in the import report.
- **Dry-run preview first**: show counts of workouts, exercises (matched vs. new), sets, and any warnings, then require confirmation.
- **Idempotent**: importing the same file again must not duplicate workouts. Use (`startedAt`, `name`) as the natural key.
- Run the import in a single database transaction and off the main thread, with progress feedback.

## 9. Seed exercise data

- Ship a JSON asset (parsed with kotlinx.serialization) containing exercises with a stable `seedKey`, name, equipment, type, and primary/secondary muscles.
- On first launch, insert the seed. On later app versions, add only new `seedKey`s and never overwrite or duplicate existing ones.
- Write the list yourself from general knowledge of common exercises. Do not copy Hevy's proprietary exercise library or media.
- Aim for a useful starting set (roughly 100 to 200 common exercises covering all types), not exhaustive coverage.

## 10. Milestones and acceptance criteria

Each milestone must leave the app building, running, and tested.

### M1: Thin slice
- Dependencies added: Room, Hilt, DataStore as needed. Schema v1 exported.
- Seed loader populates the exercise library.
- Exercise list screen (search, view).
- Start an empty workout, add exercises, log sets using the correct fields per exercise type, mark sets complete, finish.
- History list and workout detail screens.
- **Acceptance:** a workout can be fully logged and appears in history; killing the app mid-workout and reopening restores it; unit tests for DAOs and unit conversion pass; manifest has no `INTERNET` permission.

### M2: Routines
- Create, edit, delete routines and folders. Per exercise: N sets and X-Y rep range.
- Start a workout from a routine (structure copied); prefill from last session.
- **Acceptance:** editing or deleting a routine does not alter past workouts (tested); prefill uses the latest completed session.

### M3: Custom exercises, set types, supersets, rest timer
- Create, edit, archive custom exercises (all attributes: equipment, primary and secondary muscles, type).
- Set types selectable per set; warm-up sets counted in totals.
- Supersets in routines and workouts, with the adjacency invariant enforced and tested.
- Optional rest timer using an end timestamp, with a completion notification.
- **Acceptance:** superset invariant tests pass for add, remove, and reorder; timer remaining time stays correct across backgrounding and process death; no timer runs when `restSeconds` is null.

### M4: Hevy CSV import
- Import flow with file picker, dry-run preview, confirmation, report.
- **Acceptance:** the user's real export imports correctly; re-importing creates no duplicates; unit tests cover grouping, unit conversion, and set-type mapping on sample files.

### M5: Progress
- Per-exercise charts (estimated 1RM, volume, best sets) and personal records.
- **Acceptance:** numbers match hand-checked examples in tests; PR queries have one easily changed place for the warm-up rule.

### M6: Backup, restore, polish
- Export all data to a single file and import it back. Notes on workouts and exercises, unit settings, empty states, accessibility labels, performance check with a large generated history.
- **Acceptance:** export -> wipe -> restore round-trips all data; a migration test exists for every schema version; app stays smooth with several thousand workouts.

## 11. Quality gates

- Unit tests for all `domain/` logic. DAO tests with an in-memory Room database. Migration tests using exported schemas.
- Compose UI tests for the active-workout screen only.
- A check (test or CI step) that fails if the merged manifest requests `INTERNET`.
- `./gradlew lint` clean of new errors; no new compiler warnings left unaddressed.

## 12. Conventions

- Kotlin official style; no wildcard imports.
- One ViewModel per screen, state exposed as an immutable data class via `StateFlow`.
- Repositories expose `Flow` for reads and `suspend` functions for writes.
- No business rules inside composables or DAOs; put them in `domain/` or repositories.
- Commit per logical change with clear messages.

## 13. Open questions (decide with the user; do not assume)

1. **Body weight log:** should the app track body weight? Without it, bodyweight and assisted exercises are tracked by reps only and have no volume or 1RM. If yes, add a small `body_weight` table (date, kg) and extend Section 4.8.
2. **RPE:** store and show it, or ignore it on import? Default: ignore.
3. **Routine targets for non-rep types** (duration, distance): v1 stores only the set count. Add duration or distance targets later?
4. **Rest timer completion mechanism:** foreground service vs. alarm-based; decided after the Milestone 3 spike.
5. **Finishing with incomplete sets:** the default in Section 7.1 is a prompt; confirm or change.

## 14. Out of scope

Accounts, cloud sync, social features, Google/Apple integrations, iOS, wearables, widgets, body measurements, calendar view, plate calculator. These may come later but must not influence v1 design beyond keeping the schema clean.
