# Project Milestones & Execution Checklist

Each milestone must end with a runnable app, passing tests, zero `INTERNET` permissions in the merged manifest, and exported Room schemas. Add external dependencies only when the milestone that requires them begins.

---

## Phase 0: Guardrails & Foundation
- [x] Create `docs/DECISIONS.md` and record initial schema and enum choices (Equipment, Muscles, nullable `rpe`, `body_weight` table, deferred non-rep routine targets, incomplete-set finish behavior).
- [x] Configure `gradle/libs.versions.toml` with KSP (no `kapt`) and Milestone 1 dependencies (Room, Hilt, DataStore, Navigation Compose, `kotlinx.serialization`, JUnit, Turbine).
- [x] Configure Room schema export (`$projectDir/schemas`) in `app/build.gradle.kts` and add schemas to `androidTest` assets.
- [x] Create package structure (`data`, `domain`, `ui`, `di`, `importer`).
- [x] Add `ManifestPermissionTest` in `androidTest` to verify `android.permission.INTERNET` is never present.

---

## Milestone 1: Thin Slice (Core Logging & Persistence)
**Goal:** Ship a working, crash-resilient workout logger backed by Room and bundled seed exercises.

### Tasks
- [x] **Domain Models & Units (`domain/`):**
  - [x] Define enums: `Equipment`, `Muscle`, `MuscleRole` (`PRIMARY`, `SECONDARY`), `ExerciseType` (all 9 types), and `SetType` (`NORMAL`, `WARM_UP`, `FAILURE`, `DROP`).
  - [x] Implement canonical unit converters (`kg <-> lb`, `meters <-> km/mi`, `seconds <-> duration`) and unit tests.
- [ ] **Database Schema v1 (`data/local/`):**
  - [ ] Create Room entities: `exercise`, `exercise_muscle`, `routine_folder`, `routine`, `routine_exercise`, `workout`, `workout_exercise`, `workout_set` (with nullable `Float` `rpe`), and `body_weight` (table only: no DAO, repository, UI, or calculation uses it in v1).
  - [ ] Configure foreign keys, cascade deletes (workout -> workout_exercise -> workout_set), and indexes on foreign keys + `workout.startedAt`.
  - [ ] Build `AppDatabase`, `ExerciseDao`, and `WorkoutDao` (never use `fallbackToDestructiveMigration`).
  - [ ] Export Schema `1.json`.
- [ ] **Seed Loader (`data/seed/`):**
  - [ ] Create `assets/seed_exercises.json` with 100–200 common exercises across all `ExerciseType`s.
  - [ ] Implement idempotent `SeedLoader` (inserts on first launch; adds only new `seedKey`s on updates without overwriting).
- [ ] **Active Workout & Repositories (`data/repository/`):**
  - [ ] Enforce the single active workout invariant (at most one workout with `endedAt == null`).
  - [ ] Write every workout mutation (add exercise, edit field, complete set) immediately to Room.
  - [ ] Handle workout finish (`endedAt` timestamp). If incomplete sets exist, finishing is blocked behind a warning dialog (go back, or "Finish anyway"). On "Finish anyway", delete the incomplete sets, remove any exercise left with no sets, and set `endedAt` in one transaction. If no completed sets remain, delete the workout instead of saving it empty.
  - [ ] Handle discarding a whole workout as a separate action, with confirmation.
- [ ] **UI Screens (`ui/`):**
  - [ ] Exercise List screen with search and details.
  - [ ] Active Workout screen displaying dynamic inputs (`weight`, `reps`, `duration`, `distance`) per `ExerciseType`.
  - [ ] History List and Workout Detail screens.

### Acceptance Criteria
- [ ] A workout can be logged end-to-end and appears in History.
- [ ] Force-killing the app or rotating the device mid-workout restores the exact active workout state upon reopening.
- [ ] Finishing with incomplete sets shows the warning; confirming deletes exactly the incomplete sets (and empty exercises); going back changes nothing (verified by automated tests).
- [ ] Unit tests for unit conversion and in-memory Room DAO tests pass.
- [ ] Compose UI tests for the active workout screen pass.
- [ ] Merged manifest has no `INTERNET` permission.

---

## Milestone 2: Routines & Session Prefill
**Goal:** Support unlimited routines and folders, starting workouts from routines, and automatic set prefill from previous sessions.

### Tasks
- [ ] **Routine Management (`data/` & `ui/routines/`):**
  - [ ] Implement `RoutineDao` and `RoutineRepository`.
  - [ ] Build UI to create, edit, and delete routine folders and routines.
  - [ ] Support `targetSets` (N) and `repMin`–`repMax` (X–Y) per routine exercise.
- [ ] **Workout Copy & Prefill (`domain/` & `data/repository/`):**
  - [ ] Deep-copy routine structure into `workout`, `workout_exercise`, and `workout_set` rows when starting a routine workout.
  - [ ] Query the last completed session for each added exercise to prefill weight, reps, and set count; fall back to empty inputs with `repMin`–`repMax` placeholders if no history exists.

### Acceptance Criteria
- [ ] Editing or deleting a routine never alters past completed workouts (verified by automated tests).
- [ ] Adding an exercise or starting a routine prefills values from the latest completed session of that exercise.

---

## Milestone 3: Custom Exercises, Set Types, Supersets & Rest Timer
**Goal:** Add full exercise customization, set type tagging, contiguous superset groups, and a background-safe rest timer.

### Tasks
- [ ] **Custom Exercises & Soft Delete:**
  - [ ] Create, edit, and soft-delete (`isArchived = true`) custom exercises with equipment, primary/secondary muscles, and `ExerciseType`.
- [ ] **Set Types:**
  - [ ] Allow selecting `NORMAL`, `WARM_UP`, `FAILURE`, or `DROP` per set.
  - [ ] Include warm-up sets in volume and set totals via a single centralized filter function/query parameter.
- [ ] **Supersets:**
  - [ ] Support `supersetGroup` linking in both routines and active workouts.
  - [ ] Enforce the adjacency invariant in the repository layer when adding, removing, or reordering exercises; dissolve 2-exercise groups when one is removed.
  - [ ] Alternate between grouped exercises during logging and trigger the rest timer after the last exercise of each round.
- [ ] **Rest Timer:**
  - [ ] Spike foreground service vs. alarm-based notification approach and record the choice in `docs/DECISIONS.md`.
  - [ ] Store the timer's target **end timestamp** (epoch millis) so remaining time is recomputed dynamically across backgrounding and process death.
  - [ ] Request `POST_NOTIFICATIONS` permission at runtime on Android 13+.

### Acceptance Criteria
- [ ] Superset adjacency invariant unit tests pass for add, remove, and reorder operations.
- [ ] Rest timer stays accurate across app backgrounding and process death, fires a completion notification, and does not run when `restSeconds` is `null`.

---

## Milestone 4: Hevy CSV Import
**Goal:** Import historical workouts from a Hevy CSV export safely and idempotently.

### Tasks
- [ ] **Pre-Flight Check:** Inspect a real user Hevy CSV export file to confirm exact column names and date formats before coding the mapper.
- [ ] **Dependencies:** Add `kotlin-csv` (or Apache Commons CSV) to `libs.versions.toml`.
- [ ] **CSV Importer (`importer/`):**
  - [ ] Group rows into workouts by `(title, start_time)` and workout exercises by `(exercise_title, order of appearance)`.
  - [ ] Map `rpe` to `workout_set.rpe` (empty -> `null`; invalid number -> `null` with a warning).
  - [ ] Convert weights to `kg` and distances to `meters`; map `set_type` to `SetType` (defaulting unknown values to `NORMAL` with a warning); map `superset_id` to contiguous `supersetGroup`s.
  - [ ] Match exercises case-insensitively against the library; auto-create unmatched custom exercises and infer `ExerciseType` from populated columns.
  - [ ] Build Dry-Run Preview screen showing counts (workouts, matched vs. new exercises, sets, warnings/ignored columns) before confirmation.
  - [ ] Execute import inside a single background database transaction using `(startedAt, name)` as the natural key for idempotency.

### Acceptance Criteria
- [ ] Real Hevy export file imports cleanly.
- [ ] Re-importing the exact same file creates zero duplicate workouts.
- [ ] RPE values from the CSV are stored in `workout_set.rpe`.
- [ ] Unit tests pass for grouping, unit conversion, exercise type inference, and set-type mapping on sample CSV data.

---

## Milestone 5: Progress Charts & Personal Records
**Goal:** Visualize per-exercise strength/volume progression and personal records.

### Tasks
- [ ] **Dependencies:** Add `Vico` Compose charts to `libs.versions.toml`.
- [ ] **Domain Calculators (`domain/`):**
  - [ ] Implement set volume (`weight * reps` for `WEIGHT_REPS` and `WEIGHTED_BODYWEIGHT`, where weight is the added weight only; else reps/duration/distance totals). `BODYWEIGHT_REPS` and `ASSISTED_BODYWEIGHT` have no volume and no estimated 1RM in v1; the `body_weight` table is not read.
  - [ ] Implement Epley Estimated 1RM (`weight * (1 + reps / 30)`) for `WEIGHT_REPS`.
  - [ ] Compute per-exercise PRs: heaviest weight, best estimated 1RM, best single-set volume, and most reps (routing warm-up inclusion through the single centralized rule).
- [ ] **Progress UI (`ui/exercises/`):**
  - [ ] Render Vico charts (Estimated 1RM, volume, best sets) and PR summary cards on the Exercise Detail screen.

### Acceptance Criteria
- [ ] Volume, Epley 1RM, and PR calculations match hand-checked test cases.
- [ ] Warm-up inclusion in PRs and volume is controlled from one central location.
- [ ] Bodyweight and assisted exercises show reps-based records only, with no volume or estimated 1RM.

---

## Milestone 6: Backup, Restore & Large-Data Polish
**Goal:** Full offline data portability, schema migration verification, and smooth performance with years of workout history.

### Tasks
- [ ] **Backup & Restore:**
  - [ ] Serialize full database state (including `workout_set.rpe` and the `body_weight` table) to a single JSON backup file and restore it transactionally.
- [ ] **Polish & UX:**
  - [ ] Complete workout and exercise notes, unit settings (`kg/lb`, `km/mi`), empty states, and accessibility labels.
- [ ] **Performance & Paging:**
  - [ ] Generate a synthetic dataset of several thousand workouts; add `Paging 3` to History if needed to maintain 60fps scrolling.
  - [ ] Verify migration tests exist for all schema versions and `./gradlew lint` is clean of new errors/warnings.

### Acceptance Criteria
- [ ] Export -> Wipe Database -> Restore round-trips 100% of user data accurately.
- [ ] Migration test suite passes for every schema version.
- [ ] App remains responsive and smooth with several thousand logged workouts.