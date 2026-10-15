\# Architectural \& Domain Decisions



\## 1. Equipment \& Muscle Group Enums (Section 4.1)

\- \*\*Decision:\*\* Equipment: `NONE`, `BARBELL`, `DUMBBELL`, `MACHINE`, `CABLE`, `KETTLEBELL`, `RESISTANCE\\\_BAND`, `BODYWEIGHT`, `OTHER`. Muscles: `CHEST`, `UPPER\\\_BACK`, `LATS`, `SHOULDERS`, `BICEPS`, `TRICEPS`, `FOREARMS`, `ABS`, `OBLIQUES`, `LOWER\\\_BACK`, `GLUTES`, `QUADRICEPS`, `HAMSTRINGS`, `CALVES`, `ADDUCTORS`, `ABDUCTORS`, `CARDIO`, `FULL\\\_BODY`.

\- \*\*Alternatives:\*\* Lookup tables in Room or finer-grained anatomical splits (e.g., front/lateral/rear delts).

\- \*\*Reason:\*\* Spec mandates fixed code enums stored as text for v1; these cover standard gym equipment and Hevy's muscle categories without overcomplicating filtering.



\## 2. Nullable `rpe` Column on `workout\\\_set` (Section 5 \& 13.2)

\- \*\*Decision:\*\* Add `rpe: Float? = null` to the `workout\\\_set` table in Schema v1, while keeping it hidden in the v1 UI.

\- \*\*Alternatives:\*\* Omit `rpe` completely and drop the column during Hevy CSV import, or add it via migration later.

\- \*\*Reason:\*\* Preserves historical RPE data during Milestone 4 Hevy CSV imports without requiring a schema migration later when RPE UI support is added.



\## 3. `body\\\_weight` Table Included in Schema v1 (Section 13.1)

\- \*\*Decision:\*\* Include a `body\\\_weight` entity (`id: Long`, `recordedAt: Long`, `weightKg: Double`) in Schema v1.

\- \*\*Alternatives:\*\* Track bodyweight/assisted exercises by reps only and defer the table to a future version.

\- \*\*Reason:\*\* Establishes the schema foundation early so bodyweight/assisted volume calculations can be wired up without a structural migration.



\## 4. Non-Rep Routine Targets Deferred (Section 4.5 \& 13.3)

\- \*\*Decision:\*\* Keep `routine\\\_exercise` limited to `targetSets`, `repMin`, and `repMax` in Schema v1; defer duration and distance targets to a later milestone.

\- \*\*Alternatives:\*\* Add `targetDurationSec` and `targetDistanceM` columns now.

\- \*\*Reason:\*\* Matches Section 4.5 v1 rules and keeps routine creation UI and schema simple for the initial thin slice.

