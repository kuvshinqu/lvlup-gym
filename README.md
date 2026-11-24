# Level Up Gym

An Android gym tracker where you are a hero who levels up. Kotlin + Jetpack Compose, data stored on the phone (Room/SQLite).

## What it does
- **Hero** (drawn skill icons, no emoji): hero level, rank title and XP bar, a weekly quest (4 workouts, Mon to Sun) and 7 skills that level up on their own: Shield Wall (chest), Bowstring (back), Earthshaker (legs), Skybreaker (shoulders), Iron Grip (arms), Stone Core (core), Endurance (cardio).
- **Program**: starter 4-day Upper/Lower split (Mon Upper A, Tue Lower A, Thu Upper B, Fri Lower B). Each day is a "quest" with objectives (sets × reps). The app suggests the next day in the rotation.
- **Workout**: pick an exercise, log reps and weight (or minutes and distance for cardio), see XP per set, new-best badges, level-up popup.
- **History**: finished workouts with sets, volume, cardio time and XP; open one to see every set, or delete it.
- **Armory (exercises)**: 29 starter exercises (incl. 5 cardio). Each has a drawn armoured hero that animates from start (A) to finish (B) with the worked muscles glowing, plus short form notes (tap one for "How to"). Add your own and choose which skill it trains.
- **Body weight**: weigh-ins on the hero screen with the latest value and a 30-day trend line. Never affects XP. Bodyweight exercises (push-up, pull-up, plank, hanging leg raise) show "× bodyweight", plus any added weight.
- **Settings**: hero name, kg/km or lb/mi. Data is always stored in kg and metres.

## XP rules (`app/src/main/java/com/levelup/gym/game/Leveling.kt`)
- Strength set: 10 + 1 per rep (max 20) + 1 per 50 kg of volume (max 40)
- Cardio: 10 + 1 per minute (max 60) + 2 per km (max 30)
- New best weight on an exercise: +25
- Finish a workout: +50; 4th workout of the week: +100 more
- Hero level L→L+1 costs 100·L XP; skills cost 50·L

## Build
Needs JDK 17+ and the Android SDK (platform 35).

    ./gradlew assembleDebug          # APK in app/build/outputs/apk/debug/
    ./gradlew testDebugUnitTest      # rules tests + a click-through of the real app

The click-through test (`AppFlowTest`) saves screenshots to `app/build/screenshots/`.

## Ready for more users
Every exercise, workout and set belongs to a user (`users` table). v1 creates one hero; adding a user picker or accounts later doesn't need a data migration.

## Exercise drawings
All figures are drawn in code (`ui/figure/`): a small 2D rig where hands and feet are placed and elbows/knees are solved, so poses animate smoothly. Poses and props per exercise are in `ExerciseArt.kt`. `FigureGalleryTest` renders contact sheets of every start/finish pose to `app/build/screenshots/figures_*.png`.

## Signing
`app/debug.keystore` is the shared debug key, so a new build installs over the old one without losing data.
