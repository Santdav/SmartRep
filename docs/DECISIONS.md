# Architectural & Domain Decisions

## 1. Equipment, Muscle, ExerciseType and SetType Enums (Section 4.1, updated to match code)
- **Decision:**
  - Equipment: `NONE`, `BARBELL`, `DUMBBELL`, `KETTLEBELL`, `CABLE`, `PLATE`, `MACHINE`, `RESISTANCE_BAND`, `OTHER`. Bodyweight exercises are `NONE`.
  - Muscle: `ABDOMINAL`, `ABDUCTORS`, `ADDUCTORS`, `BICEPS`, `CALVES`, `CARDIO`, `CHEST`, `FOREARMS`, `FULL_BODY`, `GLUTES`, `HAMSTRINGS`, `LATS`, `LOWER_BACK`, `NECK`, `QUADRICEPS`, `SHOULDERS`, `TRAPS`, `TRICEPS`, `UPPER_BACK`, `OTHER`.
  - ExerciseType: 8 values (the spec's `WEIGHT` and `DISTANCE` are merged into `WEIGHT_DISTANCE`).
  - SetType: `NORMAL`, `WARMUP`, `FAILURE`, `DROP` (spec said `WARM_UP`).
- **Alternatives:** Lookup tables in Room; the original spec lists (BODYWEIGHT, ABS, OBLIQUES).
- **Reason:** Fixed code enums stored as text. The implemented lists follow Hevy's categories (machine/other, abdominals, neck, traps). 

## 2. Nullable `rpe` Column on `workout_set`, Kept on Import (Sections 5, 8, 13)
- **Decision:** Add `rpe: Float? = null` to `workout_set` in schema v1. Hidden in the v1 UI. The Hevy CSV import maps the CSV `rpe` column into it (empty becomes `null`; an invalid number becomes `null` with a warning).
- **Alternatives:** Omit `rpe` and ignore the column on import, or add it through a migration later.
- **Reason:** Preserves historical RPE data from the Milestone 4 import without a later schema migration when RPE UI is added.

## 3. `body_weight` Table in Schema v1, Unused in v1 (Sections 4.8, 5)
- **Decision:** Include a `body_weight` entity (`id: Long`, `recordedAt: Long`, `weightKg: Double`) in schema v1 as a table only. No DAO, repository, UI, or calculation uses it in v1. Consequences for volume:
  - `BODYWEIGHT_REPS` and `ASSISTED_BODYWEIGHT` have no volume and no estimated 1RM in v1; they show reps-based records only.
  - `WEIGHTED_BODYWEIGHT` volume is `added weight x reps`; body weight is not included.
  - A later version may compute effective load (body weight plus or minus set weight) from this table.
- **Alternatives:** Defer the table entirely, or build body weight logging and effective-load volume in v1.
- **Reason:** Establishes the schema foundation now so bodyweight and assisted volume can be added later without a structural migration, while keeping v1 scope small.

## 4. Non-Rep Routine Targets Deferred (Sections 4.5, 13)
- **Decision:** Keep `routine_exercise` limited to `targetSets`, `repMin`, and `repMax` in schema v1. Defer duration and distance targets to a later milestone.
- **Alternatives:** Add `targetDurationSec` and `targetDistanceM` columns now.
- **Reason:** Matches the Section 4.5 v1 rules and keeps routine creation UI and schema simple for the initial thin slice.

## 5. Finishing a Workout with Incomplete Sets (Section 7.1)
- **Decision:** Finishing with incomplete sets is not allowed silently. Show a warning dialog with the number of incomplete sets. The user can go back (no changes) or confirm "Finish anyway". On confirm, delete all incomplete sets, remove any `workout_exercise` left with no sets, and set `endedAt`, all in one transaction. If no completed sets remain, delete the workout instead of saving it empty.
- **Alternatives:** Block finishing until every set is completed or removed manually; offer "complete all" as an option; keep incomplete sets in history.
- **Reason:** Keeps history clean and gives the user an explicit, reversible-until-confirmed choice. The handling of empty exercises and empty workouts is an assumption (cheap to change).

## 6. Seed Exercise Data (Section 9)
- **Decision:** 174 exercises in `assets/seed_exercises.json`, written from general knowledge, named "Name (Equipment)". The loader inserts only missing `seedKey`s, one transaction per exercise, and never updates or un-archives existing rows. `seedKey`s are permanent.
- **Alternatives:** Plain names without equipment suffix; one big transaction; overwriting rows on app updates.
- **Reason:** The suffix convention lets the M4 Hevy import match seed rows by name instead of creating duplicates. Per-exercise transactions plus the unique index make a crash mid-seed harmless: the next launch fills the gaps.
