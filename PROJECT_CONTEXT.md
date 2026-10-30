# Five By Five Lifting Native Android App — Project Context & Handoff Guide

## 1. Overview & Locations
- **Local Project Directory:** `/Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android`
- **GitHub Repository:** `https://github.com/sunil2kalva/five-by-five-lifting-android`
- **GitHub Username:** `sunil2kalva`
- **Previous Conversation ID:** `a77fd8aa-2ecf-428a-b6e5-e845f56f4da8`
- **Previous Conversation Transcript:** `/Users/sunilkalva/.gemini/jetski/brain/a77fd8aa-2ecf-428a-b6e5-e845f56f4da8/.system_generated/logs/transcript.jsonl`
- **Android Package Name (`applicationId`):** `com.fivebyfivelifting.app`
- **Persistent Signing Keystore:** [`app/signing/fivebyfive.keystore`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/signing/fivebyfive.keystore)
- **Signing Certificate SHA-1:** `B6:CF:F6:5F:A3:A6:CA:2B:2E:F1:DF:46:3D:14:0B:45:47:DD:24:A3`
- **Signing Certificate SHA-256:** `15:B0:D3:61:58:63:8D:3C:83:41:9C:B4:5A:F6:7B:6E:6C:2F:F4:F2:09:19:02:80:5E:51:9F:50:95:5D:D1:0C`
- **GCP OAuth 2.0 Client ID:** `862259162609-m0kjfftsp4crj8o2kp3nrduhjjl5ripd.apps.googleusercontent.com`
- **Latest Released Version:** `v1.6.0` (`versionCode = 7`, `versionName = "1.6.0"`)
- **Latest Local APK Binary:** `/Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/FiveByFiveLifting-s26plus-debug.apk`

---

## 2. Critical Environment & Corporate Laptop Constraints
1. **Local Java Execution is Blocked (Google Santa):**
   - This corporate macOS machine blocks local execution of `java` / `javac` binaries via Google Santa.
   - **Never attempt to run `./gradlew` locally.**
2. **Cloud Build & Test Pipeline via GitHub Actions:**
   - All unit tests (`./gradlew testDebugUnitTest`) and APK builds (`./gradlew assembleDebug`) run automatically on GitHub Actions via [`.github/workflows/build-apk.yml`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/.github/workflows/build-apk.yml).
   - Pushing to `main` triggers the workflow, uploads the `FiveByFiveLifting-s26plus-debug` artifact, and publishes a GitHub Release (`v1.6.0`, etc.) with `FiveByFiveLifting-s26plus-debug.apk` attached.
3. **Providing Download Links to the User:**
   - **Always wrap APK download URLs in plain-text fenced code blocks (` ```text ... ``` `)** so the user can copy-paste them on macOS without the browser auto-downloading the file on click.
4. **GitHub API Push via `scripts/wait_and_download_apk.py`:**
   - Use `/usr/local/bin/python3 -u scripts/wait_and_download_apk.py` with `GH_TOKEN` set to push tracked files via the GitHub Git Data REST API, poll the workflow run, and download the compiled `.apk`.

---

## 3. Architecture & Key Source Files
- **Tech Stack:** Kotlin 2.0.20, Android Gradle Plugin 8.5.2, Jetpack Compose (Material 3), Room SQLite (`version = 2`), DataStore Preferences, Google Play Services Auth (`21.2.0`), AndroidX Health Connect (`1.1.0-alpha07`), Kotlinx Serialization JSON, JUnit 4.
- **Target Device:** Samsung Galaxy S26+ (`minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`).

### File Map
- **App Entry & Navigation:**
  - [`app/src/main/java/com/fivebyfivelifting/app/MainActivity.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/MainActivity.kt) — Bottom navigation bar (`Home`, `Workout`, `History`, `Graphs`, `Settings`) using deterministic `navigateToTab(tab)` with `popUpTo(0) { inclusive = true }` and a ~3.5-second multi-pulse gym timer chime & haptic alert (`playRestFinishedAlert()`).
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/StrengthViewModel.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/StrengthViewModel.kt) — Central reactive `StateFlow` state holder for workouts, custom workout templates (`A`–`E`), configurable rest timer (default `180s` / `3:00`), Health Connect sync, graphs, and Google Drive / local backup.
- **UI Screens & Components:**
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/HomeScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/HomeScreen.kt) — Next workout overview, pull-down workout selector (`Workout A ▾`..`E ▾`), custom workout builder access, and Start Workout CTA.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/WorkoutScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/WorkoutScreen.kt) — Compact single-screen 3-exercise workout logger with an inline HUD circular rest timer pill + 3dp full-width progress bar in the header, compact `44.dp` set bubbles, inline warm-up toggle pill, and docked `"Finish Workout X"` button.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/components/WorkoutDropdownAndBuilder.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/components/WorkoutDropdownAndBuilder.kt) — Pull-down workout switcher (`Workout A`–`E`), custom workout template builder, and custom lift editor (name, sets, reps, weight, increment).
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/HistoryScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/HistoryScreen.kt) — Completed workout log history with delete & detail cards.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/GraphsScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/GraphsScreen.kt) — Canvas progression charts + Epley 1RM estimates + standalone bodyweight logging.
  - [`app/src/main/java/com/fivebyfivelifting/app/ui/screens/SettingsScreen.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/ui/screens/SettingsScreen.kt) — Native Google Sign-In (`GoogleAuthManager`), Google Drive backup status & file picker, configurable Rest Timer Between Sets (`3:00` default), Android Health Connect Sync toggle, unit system (`lb`/`kg`), barbell weight, and working weight adjustments.
- **Domain & Data Layer:**
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/HealthConnectManager.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/HealthConnectManager.kt) — Android Health Connect integration (`ExerciseSessionRecord` with `EXERCISE_TYPE_WEIGHTLIFTING`, start/end timestamps, and exercise/volume summary).
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/GoogleAuthManager.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/GoogleAuthManager.kt) — Native Google Sign-In via `GoogleSignInClient` and GCP OAuth 2.0 Client ID.
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/ProgressionEngine.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/ProgressionEngine.kt) — Linear progression, strike counting, and automatic 10% deload after 3 consecutive failures.
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/PlateCalculator.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/PlateCalculator.kt) — Per-side barbell plate breakdown (`lb` & `kg`) and warm-up ramp calculator.
  - [`app/src/main/java/com/fivebyfivelifting/app/domain/CloudSyncManager.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/domain/CloudSyncManager.kt) — Google Account chooser intent, Google Drive SAF root URI builder, direct Google Drive app share intent, and persistent JSON auto-sync.
  - [`app/src/main/java/com/fivebyfivelifting/app/data/local/AppDatabase.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/data/local/AppDatabase.kt) & [`WorkoutRepository.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/data/repository/WorkoutRepository.kt) — Room SQLite database.
  - [`app/src/main/java/com/fivebyfivelifting/app/data/preferences/UserPreferencesRepository.kt`](file:///Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android/app/src/main/java/com/fivebyfivelifting/app/data/preferences/UserPreferencesRepository.kt) — DataStore persistence for settings, rest timer duration (`180s` default), Health Connect sync toggle, Google Sign-In profile, and cloud backup URI.

---

## 4. Release History
- **`v1.0.0`:** Initial Native Android app (`5x5` Workout A/B, rest timer, plate calculator, warm-up sets, progression & auto-deload, Room SQLite, graphs, SAF cloud backup).
- **`v1.1.0`:** Added pull-down workout switcher (`Workout A`–`E`) on both Home and Active Workout screens + Custom Workout & Exercise Builder modal (`17` exercises + custom lift overrides).
- **`v1.2.0`:** Fixed bottom navigation `Home` tab bug + added Android native Google Account picker and direct "Save to Google Drive App" folder picker.
- **`v1.3.0`:** Redesigned Google Drive Backup status card in Settings (`● Configured` state, removed `"Link Drive Auto-Sync File"`, conditional `"Restore / Merge"` button).
- **`v1.4.0`:** Rebranded to **Five By Five Lifting** (`com.fivebyfivelifting.app`) and generated persistent Android signing keystore (`app/signing/fivebyfive.keystore`).
- **`v1.5.0`:** Added native **Sign in with Google** (`com.google.android.gms:play-services-auth:21.2.0`) configured with GCP OAuth 2.0 Client ID.
- **`v1.6.0`:** Added configurable Rest Timer in Settings (default `3:00` / `180s`), ~3.5-second multi-pulse timer elapsed chime + vibration, Android Health Connect workout sync toggle & integration, inline HUD header rest timer pill, removed Bodyweight & Notes card from Workout screen, simplified `"Finish Workout X"` button, and compacted Workout screen layout so 3-exercise workouts fit on one screen without scrolling.

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
