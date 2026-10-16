# Architectural & Domain Decisions

## 1. Equipment & Muscle Group Enums (Section 4.1)
- **Decision:** Equipment: `NONE`, `BARBELL`, `DUMBBELL`, `MACHINE`, `CABLE`, `KETTLEBELL`, `RESISTANCE_BAND`, `BODYWEIGHT`, `OTHER`. Muscles: `CHEST`, `UPPER_BACK`, `LATS`, `SHOULDERS`, `BICEPS`, `TRICEPS`, `FOREARMS`, `ABS`, `OBLIQUES`, `LOWER_BACK`, `GLUTES`, `QUADRICEPS`, `HAMSTRINGS`, `CALVES`, `ADDUCTORS`, `ABDUCTORS`, `CARDIO`, `FULL_BODY`.
- **Alternatives:** Lookup tables in Room, or finer-grained anatomical splits (for example front/lateral/rear delts).
- **Reason:** The spec mandates fixed code enums stored as text for v1. These cover standard gym equipment and Hevy's muscle categories without overcomplicating filtering.

## 2. Nullable `rpe` Column on `workout_set`, Kept on Import (Sections 5, 8, 13)
- **Decision:** Add `rpe: Float? = null` to the `workout_set` table in schema v1. Keep it hidden in the v1 UI. The Hevy CSV import maps the CSV `rpe` column into it (empty becomes `null`; an invalid number becomes `null` with a warning).
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
