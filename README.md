# Five By Five Lifting — Native Android App (Kotlin + Jetpack Compose + Room SQLite)

A 100% offline-first Native Android strength tracker replicating the **Five By Five Lifting** workout program, including "Pro" features (Visual Barbell Plate Calculator, Progressive Warm-Up Calculator, Progress Charts, Workout Calendar, and 3-Layer Zero-Cost Cloud Sync).

## Features Included
1. **Alternating Workout A & Workout B (3x/week):**
   - **Workout A:** Squat (`5×5`), Bench Press (`5×5`), Barbell Row (`5×5`)
   - **Workout B:** Squat (`5×5`), Overhead Press (`5×5`), Deadlift (`1×5`)
2. **Signature Tap-to-Log 5×5 Set Bubbles:**
   - 1st tap marks `5` reps completed; subsequent taps decrement `4 → 3 → 2 → 1 → 0 → Clear`.
3. **Smart Context-Aware Rest Timer:**
   - Automatically starts when a working set bubble is tapped:
     - **1:30** on a completed 5-rep set (with 1-tap **3:00** button if the set felt heavy)
     - **5:00** automatically if a rep was missed (`< 5` reps)
4. **Automated Linear Progression & 3-Strike 10% Deload Engine:**
   - **+5 lb (+2.5 kg)** per successful `5×5` session on Squat, Bench, OHP, and Barbell Row.
   - **+10 lb (+5.0 kg)** per successful `1×5` session on Deadlift (transitions to `+5 lb` at `225 lb+`).
   - **3-Strike Deload:** 3 consecutive failed sessions on an exercise automatically reduce working weight by **10%** (rounded down to the nearest `5 lb` / `2.5 kg`).
5. **Visual Barbell Plate Calculator & Progressive Warm-Up Generator:**
   - Shows color-coded Olympic plates per side (`lb` and `kg`) and calculates warm-up sets from the empty bar (`2×5 @ 45 lb / 20 kg`) up to the working weight.
6. **Zero-Cost Cloud Sync (Survives Switching Phones for $0/month):**
   - **Layer 1 — Android OS Auto-Backup (`backup_rules.xml` & `data_extraction_rules.xml`):** Automatically backs up the local `five_by_five_lifting.db` Room SQLite database and DataStore settings to your Google Account and restores them on any new Android phone.
   - **Layer 2 — Automatic Post-Workout Google Drive Sync (`CloudSyncManager.kt`):** Link a file in your personal Google Drive from the **Settings** tab; every completed workout automatically uploads your full workout history to Google Drive, and **Restore / Merge from Google Drive** combines workouts across devices without duplicates.
   - **Layer 3 — CSV & JSON Export:** Export full workout logs to `.csv` for Excel/Google Sheets.

## How to Open & Run
1. Open **Android Studio** (Ladybug / Koala or newer).
2. Select **Open** and choose `/Users/sunilkalva/.gemini/jetski/scratch/five-by-five-lifting-android`.
3. Wait for Gradle Sync to complete, then click **Run (`▶`)** on an Android Emulator or physical Android device (API 26+).
4. Run unit tests from Android Studio or terminal:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
