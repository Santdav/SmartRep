# Project Milestones & Execution Checklist

Each milestone must end with a runnable app, passing tests, zero `INTERNET` permissions in the merged manifest, and exported Room schemas. Add external dependencies only when the milestone that requires them begins[cite: 1].

---

## Phase 0: Guardrails & Foundation
- [x] Create `docs/DECISIONS.md` and record initial schema and enum choices (Equipment, Muscles, nullable `rpe`, `body_weight` table, deferred non-rep routine targets)[cite: 1].
- [ ] Configure `gradle/libs.versions.toml` with KSP (no `kapt`) and Milestone 1 dependencies (Room, Hilt, DataStore, Navigation Compose, `kotlinx.serialization`, JUnit, Turbine)[cite: 1].
- [ ] Configure Room schema export (`$projectDir/schemas`) in `app/build.gradle.kts` and add schemas to `androidTest` assets[cite: 1].
- [ ] Create package structure (`data`, `domain`, `ui`, `di`, `importer`)[cite: 1, 2].
- [x] Add `ManifestPermissionTest` in `androidTest` to verify `android.permission.INTERNET` is never present[cite: 1].

---

## Milestone 1: Thin Slice (Core Logging & Persistence)
**Goal:** Ship a working, crash-resilient workout logger backed by Room and bundled seed exercises[cite: 1, 2].

### Tasks
- [ ] **Domain Models & Units (`domain/`):**
  - [ ] Define enums: `Equipment`, `Muscle`, `MuscleRole` (`PRIMARY`, `SECONDARY`), `ExerciseType` (all 9 types), and `SetType` (`NORMAL`, `WARM_UP`, `FAILURE`, `DROP`)[cite: 1].
  - [ ] Implement canonical unit converters (`kg <-> lb`, `meters <-> km/mi`, `seconds <-> duration`) and unit tests[cite: 1].
- [ ] **Database Schema v1 (`data/local/`):**
  - [ ] Create Room entities: `exercise`, `exercise_muscle`, `routine_folder`, `routine`, `routine_exercise`, `workout`, `workout_exercise`, `workout_set` (with nullable `rpe`), and `body_weight`[cite: 1].
  - [ ] Configure foreign keys, cascade deletes (workout -> workout_exercise -> workout_set), and indexes on foreign keys + `workout.startedAt`[cite: 1].
  - [ ] Build `AppDatabase`, `ExerciseDao`, and `WorkoutDao` (never use `fallbackToDestructiveMigration`)[cite: 1].
  - [ ] Export Schema `1.json`[cite: 1].
- [ ] **Seed Loader (`data/seed/`):**
  - [ ] Create `assets/seed_exercises.json` with 100–200 common exercises across all `ExerciseType`s[cite: 1].
  - [ ] Implement idempotent `SeedLoader` (inserts on first launch; adds only new `seedKey`s on updates without overwriting)[cite: 1].
- [ ] **Active Workout & Repositories (`data/repository/`):**
  - [ ] Enforce the single active workout invariant (at most one workout with `endedAt == null`)[cite: 1].
  - [ ] Write every workout mutation (add exercise, edit field, complete set) immediately to Room[cite: 1].
  - [ ] Handle workout finish (`endedAt` timestamp) and incomplete sets prompt (complete vs. discard)[cite: 1].
- [ ] **UI Screens (`ui/`):**
  - [ ] Exercise List screen with search and details[cite: 1].
  - [ ] Active Workout screen displaying dynamic inputs (`weight`, `reps`, `duration`, `distance`) per `ExerciseType`[cite: 1, 2].
  - [ ] History List and Workout Detail screens[cite: 1].

### Acceptance Criteria
- [ ] A workout can be logged end-to-end and appears in History[cite: 1].
- [ ] Force-killing the app or rotating the device mid-workout restores the exact active workout state upon reopening[cite: 1].
- [ ] Unit tests for unit conversion and in-memory Room DAO tests pass[cite: 1].
- [ ] Compose UI tests for the active workout screen pass[cite: 1].
- [ ] Merged manifest has no `INTERNET` permission[cite: 1].

---

## Milestone 2: Routines & Session Prefill
**Goal:** Support unlimited routines and folders, starting workouts from routines, and automatic set prefill from previous sessions[cite: 1, 2].

### Tasks
- [ ] **Routine Management (`data/` & `ui/routines/`):**
  - [ ] Implement `RoutineDao` and `RoutineRepository`[cite: 1].
  - [ ] Build UI to create, edit, and delete routine folders and routines[cite: 1].
  - [ ] Support `targetSets` (N) and `repMin`–`repMax` (X–Y) per routine exercise[cite: 1, 2].
- [ ] **Workout Copy & Prefill (`domain/` & `data/repository/`):**
  - [ ] Deep-copy routine structure into `workout`, `workout_exercise`, and `workout_set` rows when starting a routine workout[cite: 1].
  - [ ] Query the last completed session for each added exercise to prefill weight, reps, and set count; fall back to empty inputs with `repMin`–`repMax` placeholders if no history exists[cite: 1].

### Acceptance Criteria
- [ ] Editing or deleting a routine never alters past completed workouts (verified by automated tests)[cite: 1].
- [ ] Adding an exercise or starting a routine prefills values from the latest completed session of that exercise[cite: 1].

---

## Milestone 3: Custom Exercises, Set Types, Supersets & Rest Timer
**Goal:** Add full exercise customization, set type tagging, contiguous superset groups, and a background-safe rest timer[cite: 1, 2].

### Tasks
- [ ] **Custom Exercises & Soft Delete:**
  - [ ] Create, edit, and soft-delete (`isArchived = true`) custom exercises with equipment, primary/secondary muscles, and `ExerciseType`[cite: 1].
- [ ] **Set Types:**
  - [ ] Allow selecting `NORMAL`, `WARM_UP`, `FAILURE`, or `DROP` per set[cite: 1, 2].
  - [ ] Include warm-up sets in volume and set totals via a single centralized filter function/query parameter[cite: 1].
- [ ] **Supersets:**
  - [ ] Support `supersetGroup` linking in both routines and active workouts[cite: 1, 2].
  - [ ] Enforce the adjacency invariant in the repository layer when adding, removing, or reordering exercises; dissolve 2-exercise groups when one is removed[cite: 1].
  - [ ] Alternate between grouped exercises during logging and trigger the rest timer after the last exercise of each round[cite: 1].
- [ ] **Rest Timer:**
  - [ ] Spike foreground service vs. alarm-based notification approach and record the choice in `docs/DECISIONS.md`[cite: 1].
  - [ ] Store the timer's target **end timestamp** (epoch millis) so remaining time is recomputed dynamically across backgrounding and process death[cite: 1].
  - [ ] Request `POST_NOTIFICATIONS` permission at runtime on Android 13+[cite: 1].

### Acceptance Criteria
- [ ] Superset adjacency invariant unit tests pass for add, remove, and reorder operations[cite: 1].
- [ ] Rest timer stays accurate across app backgrounding and process death, fires a completion notification, and does not run when `restSeconds` is `null`[cite: 1].

---

## Milestone 4: Hevy CSV Import
**Goal:** Import historical workouts from a Hevy CSV export safely and idempotently[cite: 1, 2].

### Tasks
- [ ] **Pre-Flight Check:** Inspect a real user Hevy CSV export file to confirm exact column names and date formats before coding the mapper[cite: 1].
- [ ] **Dependencies:** Add `kotlin-csv` (or Apache Commons CSV) to `libs.versions.toml`[cite: 1, 2].
- [ ] **CSV Importer (`importer/`):**
  - [ ] Group rows into workouts by `(title, start_time)` and workout exercises by `(exercise_title, order of appearance)`[cite: 1].
  - [ ] Convert weights to `kg` and distances to `meters`; map `set_type` to `SetType` (defaulting unknown values to `NORMAL` with a warning); map `superset_id` to contiguous `supersetGroup`s[cite: 1].
  - [ ] Match exercises case-insensitively against the library; auto-create unmatched custom exercises and infer `ExerciseType` from populated columns[cite: 1].
  - [ ] Build Dry-Run Preview screen showing counts (workouts, matched vs. new exercises, sets, warnings/ignored columns) before confirmation[cite: 1].
  - [ ] Execute import inside a single background database transaction using `(startedAt, name)` as the natural key for idempotency[cite: 1].

### Acceptance Criteria
- [ ] Real Hevy export file imports cleanly[cite: 1].
- [ ] Re-importing the exact same file creates zero duplicate workouts[cite: 1, 2].
- [ ] Unit tests pass for grouping, unit conversion, exercise type inference, and set-type mapping on sample CSV data[cite: 1].

---

## Milestone 5: Progress Charts & Personal Records
**Goal:** Visualize per-exercise strength/volume progression and personal records[cite: 1, 2].

### Tasks
- [ ] **Dependencies:** Add `Vico` Compose charts to `libs.versions.toml`[cite: 1, 2].
- [ ] **Domain Calculators (`domain/`):**
  - [ ] Implement set volume (`weight * reps` for `WEIGHT_REPS` and `WEIGHTED_BODYWEIGHT`, else reps/duration/distance totals)[cite: 1].
  - [ ] Implement Epley Estimated 1RM (`weight * (1 + reps / 30)`) for `WEIGHT_REPS`[cite: 1].
  - [ ] Compute per-exercise PRs: heaviest weight, best estimated 1RM, best single-set volume, and most reps (routing warm-up inclusion through the single centralized rule)[cite: 1].
- [ ] **Progress UI (`ui/exercises/`):**
  - [ ] Render Vico charts (Estimated 1RM, volume, best sets) and PR summary cards on the Exercise Detail screen[cite: 1].

### Acceptance Criteria
- [ ] Volume, Epley 1RM, and PR calculations match hand-checked test cases[cite: 1].
- [ ] Warm-up inclusion in PRs and volume is controlled from one central location[cite: 1].

---

## Milestone 6: Backup, Restore & Large-Data Polish
**Goal:** Full offline data portability, schema migration verification, and smooth performance with years of workout history[cite: 1, 2].

### Tasks
- [ ] **Backup & Restore:**
  - [ ] Serialize full database state to a single JSON backup file and restore it transactionally[cite: 1].
- [ ] **Polish & UX:**
  - [ ] Complete workout and exercise notes, unit settings (`kg/lb`, `km/mi`), empty states, and accessibility labels[cite: 1, 2].
- [ ] **Performance & Paging:**
  - [ ] Generate a synthetic dataset of several thousand workouts; add `Paging 3` to History if needed to maintain 60fps scrolling[cite: 1].
  - [ ] Verify migration tests exist for all schema versions and `./gradlew lint` is clean of new errors/warnings[cite: 1].

### Acceptance Criteria
- [ ] Export -> Wipe Database -> Restore round-trips 100% of user data accurately[cite: 1].
- [ ] Migration test suite passes for every schema version[cite: 1].
- [ ] App remains responsive and smooth with several thousand logged workouts[cite: 1].