# Offline Workout Tracker

A personal, fully offline Android workout tracker inspired by Hevy. No account, no cloud, no social features, and no paid limits: unlimited routines and unlimited custom exercises.

> Working title. This is an independent personal project and is not affiliated with Hevy.

## Why this exists

Hevy's free tier caps routines and custom exercises and requires an account. This app keeps the part that works well (fast, responsive workout logging) and drops everything else.

## Features

**v1 (must-have)**
- Exercise library: bundled seed list plus unlimited custom exercises
- Exercise attributes: equipment, primary and secondary muscle groups, exercise type
- Routines and folders, with a target of N sets over an X-Y rep range per exercise
- Workout logging with set types (warm-up, normal, failure, drop)
- Finishing a workout with incomplete sets shows a warning; if you finish anyway, the incomplete sets are deleted
- Supersets, in both routines and active workouts
- Optional rest timer
- Workout history and per-exercise history
- Import of your existing data from Hevy's CSV export, including RPE (stored, not yet shown in the UI)

**Planned (should-have)**
- Progress charts and personal records
- Notes on workouts and exercises
- kg/lb unit setting
- Local backup and restore to a file

**Not included, by design**
- Accounts or login, feeds, follows, likes, comments
- Cloud sync
- Google or Apple integrations
- iOS, wearables

## Exercise types supported

| Type | Fields logged per set |
|---|---|
| Weight x reps | weight, reps |
| Bodyweight | reps |
| Weighted bodyweight (+) | added weight, reps |
| Assisted bodyweight (-) | assist weight, reps |
| Duration | duration |
| Duration x weight | weight, duration |
| Distance x duration | distance, duration |
| Weight | weight |
| Distance | distance |

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Navigation Compose
- Coroutines and Flow
- Room (with KSP) for the local database
- Hilt for dependency injection
- DataStore (Preferences) for settings
- kotlinx.serialization for backup files and the exercise seed asset
- kotlin-csv (or Apache Commons CSV) for the Hevy import
- Vico for charts
- Testing: JUnit, Turbine, Room in-memory tests, Compose UI tests

The app declares no `INTERNET` permission, so "fully offline" is enforced by the manifest.

## Getting started

**Requirements**
- Android Studio (current stable)
- JDK 17 or newer (the one bundled with Android Studio works)
- An Android device or emulator

**Build and run**
1. Clone the repository and open it in Android Studio.
2. Let Gradle sync finish.
3. Select a device and press Run, or from the command line:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

**Run the tests**

```bash
./gradlew testDebugUnitTest          # unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (device/emulator required)
```

## Importing your Hevy data

RPE values in your export are kept and stored with each set. The app does not display RPE yet.

1. In Hevy, export your workouts as CSV from the app's settings.
2. Copy the CSV file to your phone.
3. In this app, open Settings, choose Import from Hevy CSV, and pick the file.
4. Review the dry-run preview, then confirm.

Importing the same file twice does not create duplicates.

## Project structure

```
app/src/main/java/<your/package>/
  data/        Room entities, DAOs, database, repositories, seed loader
  domain/      Pure Kotlin models and logic (volume, PRs, superset rules)
  ui/          Compose screens and ViewModels, one package per feature
  importer/    Hevy CSV import
  di/          Hilt modules
docs/
  HANDOFF.md      Full specification
  MILESTONES.md   Milestone checklist and acceptance criteria
  DECISIONS.md    Architectural and domain decision log
```

## Documentation

- [`docs/HANDOFF.md`](docs/HANDOFF.md): product rules, data model, architecture, import spec, milestones with acceptance criteria.
- [`docs/MILESTONES.md`](docs/MILESTONES.md): execution checklist per milestone.
- [`docs/DECISIONS.md`](docs/DECISIONS.md): decision log (alternatives and reasons).

## Roadmap

- [ ] M1: Thin slice (schema, seed exercises, log a workout, see history)
- [ ] M2: Routines with rep ranges, start from routine, prefill from last session
- [ ] M3: Custom exercises, set types, supersets, optional rest timer
- [ ] M4: Hevy CSV import
- [ ] M5: Progress charts and personal records
- [ ] M6: Backup and restore, polish

## Data and privacy

All data lives in a local database on your device. Nothing is sent anywhere. Because there is no cloud copy, use the backup export regularly.

## License

To be decided.
