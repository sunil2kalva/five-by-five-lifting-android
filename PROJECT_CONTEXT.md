# Five By Five Lifting Native Android App — Project Context & Handoff Guide

## 1. Overview & Locations
- **Local Project Directory:** `/Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android`
- **GitHub Repository:** `https://github.com/sunil2kalva/five-by-five-lifting-android`
- **GitHub Username:** `sunil2kalva`
- **Previous Conversation ID:** `a77fd8aa-2ecf-428a-b6e5-e845f56f4da8`
- **Previous Conversation Transcript:** `/Users/sunilkalva/.gemini/jetski/brain/a77fd8aa-2ecf-428a-b6e5-e845f56f4da8/.system_generated/logs/transcript.jsonl`
- **Latest Released Version:** `v1.2.0` (`versionCode = 3`, `versionName = "1.2.0"`)
- **Latest Local APK Binary:** `/Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/FiveByFiveLifting-s26plus-debug.apk`

---

## 2. Critical Environment & Corporate Laptop Constraints
1. **Local Java Execution is Blocked (Google Santa):**
   - This corporate macOS machine blocks local execution of `java` / `javac` binaries via Google Santa.
   - **Never attempt to run `./gradlew` locally.**
2. **Cloud Build & Test Pipeline via GitHub Actions:**
   - All unit tests (`./gradlew testDebugUnitTest`) and APK builds (`./gradlew assembleDebug`) run automatically on GitHub Actions via [`.github/workflows/build-apk.yml`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/.github/workflows/build-apk.yml).
   - Pushing to `main` triggers the workflow, uploads the `FiveByFiveLifting-s26plus-debug` artifact, and publishes a GitHub Release (`v1.2.0`, etc.) with `FiveByFiveLifting-s26plus-debug.apk` attached.
3. **Providing Download Links to the User:**
   - **Always wrap APK download URLs in plain-text fenced code blocks (` ```text ... ``` `)** so the user can copy-paste them on macOS without the browser auto-downloading the file on click.
4. **GitHub Personal Access Token (PAT) Handling:**
   - When pushing to GitHub, temporarily set the remote URL with the user's PAT, push, and **immediately strip the PAT** back to `https://github.com/sunil2kalva/five-by-five-lifting-android.git` so the token is never left in `.git/config`.

---

## 3. Architecture & Key Source Files
- **Tech Stack:** Kotlin 2.0.20, Android Gradle Plugin 8.5.2, Jetpack Compose (Material 3), Room SQLite (`version = 2`), DataStore Preferences, Kotlinx Serialization JSON, JUnit 4.
- **Target Device:** Samsung Galaxy S26+ (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`).

### File Map
- **App Entry & Navigation:**
  - [`app/src/main/java/com/fivebyfivelifting/app/MainActivity.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/MainActivity.kt) — Bottom navigation bar (`Home`, `Workout`, `History`, `Graphs`, `Settings`) using deterministic `navigateToTab(tab)` with `popUpTo(0) { inclusive = true }` so `Home` never gets stuck on `Settings`.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/StrengthViewModel.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/StrengthViewModel.kt) — Central reactive `StateFlow` state holder for workouts, custom workout templates (`A`–`E`), rest timer, graphs, and Google Drive / local backup.
- **UI Screens & Components:**
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/HomeScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/HomeScreen.kt) — Next workout overview, pull-down workout selector (`Workout A ▾`..`E ▾`), custom workout builder access, and Start Workout CTA.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/WorkoutScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/WorkoutScreen.kt) — Active workout logger (`5x5`, `1x5`, or custom sets/reps), pull-down selector to switch active workout on the fly, warm-up sets modal, plate calculator modal, and countdown rest timer bar.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/components/WorkoutDropdownAndBuilder.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/components/WorkoutDropdownAndBuilder.kt) — Pull-down workout switcher (`Workout A`–`E`), custom workout template builder, and custom lift editor (name, sets, reps, weight, increment).
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/HistoryScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/HistoryScreen.kt) — Completed workout log history with delete & detail cards.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/GraphsScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/GraphsScreen.kt) — Canvas progression charts + Epley 1RM estimates.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/SettingsScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/SettingsScreen.kt) — Google Account picker (`AccountManager`), Google Drive file system root targeting (`DocumentsContract`), direct "Save to Google Drive App" (`FileProvider`), unit system (`lb`/`kg`), barbell weight, and working weight adjustments.
- **Domain & Data Layer:**
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/ExerciseCatalog.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/ExerciseCatalog.kt) — 17 built-in and custom exercises + default workouts `A`–`E`.
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/ProgressionEngine.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/ProgressionEngine.kt) — Linear progression, strike counting, and automatic 10% deload after 3 consecutive failures.
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/PlateCalculator.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/PlateCalculator.kt) — Per-side barbell plate breakdown (`lb` & `kg`) and warm-up ramp calculator.
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/CloudSyncManager.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/CloudSyncManager.kt) — Google Account chooser intent, Google Drive SAF root URI builder, direct Google Drive app share intent, and persistent JSON auto-sync.
  - [`app/src/main/java/com/fivebyfivelifting/app/data/db/AppDatabase.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/data/db/AppDatabase.kt) & [`WorkoutRepository.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/data/repository/WorkoutRepository.kt) — Room SQLite database (`WorkoutSessionEntity`, `ExerciseLogEntity`, `SetLogEntity`, `ExerciseStateEntity`).
  - [`app/src/main/java/com/fivebyfivelifting/app/data/preferences/UserPreferencesRepository.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/data/preferences/UserPreferencesRepository.kt) — DataStore persistence for settings, custom workout templates, custom exercise overrides, selected Google account email, and cloud backup URI.

---

## 4. Release History
- **`v1.0.0`:** Initial Native Android app + PWA (`5x5` Workout A/B, rest timer, plate calculator, warm-up sets, progression & auto-deload, Room SQLite, graphs, SAF cloud backup).
- **`v1.1.0`:** Added pull-down workout switcher (`Workout A`–`E`) on both Home and Active Workout screens + Custom Workout & Exercise Builder modal (`17` exercises + custom lift overrides).
- **`v1.2.0`:** Fixed bottom navigation `Home` tab bug + added Android native Google Account picker (`AccountManager.newChooseAccountIntent`), account-specific Google Drive root pre-selection (`com.google.android.apps.docs.storage`), and direct "Save to Google Drive App" folder picker (`com.google.android.apps.docs`).

---

## 5. How to Build & Publish the Next Release (`v1.3.0+`)
1. Bump `versionCode` and `versionName` in [`app/build.gradle.kts`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/build.gradle.kts).
2. Update `tag_name` and `name` in [`.github/workflows/build-apk.yml`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/.github/workflows/build-apk.yml).
3. Commit, push to `main`, and poll the GitHub Actions run using [`scripts/wait_and_download_apk.py`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/scripts/wait_and_download_apk.py):
   ```bash
   export GH_TOKEN="<GITHUB_PAT>"
   git add .
   git commit -m "v1.3.0: <description>"
   git remote set-url origin "https://sunil2kalva:${GH_TOKEN}@github.com/sunil2kalva/five-by-five-lifting-android.git"
   git push origin main
   git remote set-url origin "https://github.com/sunil2kalva/five-by-five-lifting-android.git"
   sleep 4
   /usr/local/bin/python3 -u scripts/wait_and_download_apk.py
   ```
