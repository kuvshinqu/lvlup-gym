# Level Up Gym: notes for Claude Code

Android gym tracker where the user is a hero who levels up. Kotlin 2.0, Jetpack Compose (BOM 2024.12.01),
Room 2.6 (DB v4, schemas exported to `app/schemas/`), AGP 8.13, JDK 17, compile/target SDK 36, minSdk 26.
Single activity, one `GymViewModel`, manual DI via `GymApp`. All data stays on the phone (no INTERNET permission).

## Commands
- Debug APK: `./gradlew assembleDebug` (installs as `com.levelupgym.app.debug` next to the Play version)
- Install on a plugged-in phone: `./gradlew installDebug`
- Release bundle: `./gradlew bundleRelease` (needs `keystore.properties` + the upload `.jks`, both untracked)
- Tests: `./gradlew testDebugUnitTest`. `AppFlowTest` clicks through the real app with Robolectric and writes
  screenshots to `app/build/screenshots/`; look at them after UI changes.
- One test class: `./gradlew testDebugUnitTest --tests 'com.levelup.gym.game.StreakTest'`

## Where things live
- Game rules (pure Kotlin, unit tested) in `game/`: `Leveling.kt` (XP, levels), `Loot.kt` (chests, gear),
  `Streak.kt` (daily streak, rest days, shields), `Badges.kt`, `Program.kt` (built-in programs), `Quiz.kt`,
  `WorkoutDraft.kt` (planned/added sets of the running workout), `HistoryExport.kt` (CSV).
- Data in `data/`: entities, `GymDao`, migrations in `GymDatabase.kt` (add a `MIGRATION_x_y` and extend
  `MigrationTest` for every schema change; take the SQL from the exported schema JSON).
- UI in `ui/`: house style kit in `Kit.kt` (`chunkyBlock`, `GameCard`, `GameButton`, `OutlinedText`, `XpPill`...),
  colours/fonts in `Theme.kt`, code-drawn exercise figures in `ui/figure/`.

## Conventions
- Look: bold medieval mobile-game style. Fonts Lilita One (titles, usually via `OutlinedText`) + Nunito (body).
  No emojis anywhere; icons are the `ic_*` vector drawables or Canvas drawings.
- Weights stored in kg and distances in metres; convert with `UnitsFormat` for display (user can pick lb/mi).
- `Button`/`OutlinedButton`/`FilledTonalButton` in `ui` are house-style shims; don't import Material's.
- Bump `versionCode` in `app/build.gradle.kts` for every Play upload.
- Never commit `keystore.properties` or `*.jks`.

## Test quirks (Robolectric)
- A screen with an open dialog never idles: set `compose.mainClock.autoAdvance = false` and step with
  `advanceTimeBy`, then turn it back on.
- When the same text appears more than once, use `onAllNodesWithText(...)[0]`.
- Workout set fields have test tags like `weight-Bench Press-1`.
