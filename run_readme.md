# DoomSQL - Android Studio Run, Build, Setup & Debug Guide

This document provides a step-by-step guide to importing, configuring, building, running, and debugging **DoomSQL** using **Android Studio** and the Gradle command-line interface.

---

## 1. Prerequisites

Before opening the project, ensure your workstation has:
- **Android Studio:** Android Studio Hedgehog, Iguana, Jellyfish, Koala, Ladybug, or newer.
- **Java Development Kit (JDK):** **JDK 17** or **JDK 21** (bundled with Android Studio by default in `jbr`).
- **Android SDK:**
  - `compileSdk = 36`
  - `targetSdk = 36`
  - `minSdk = 26` (Android 8.0 Oreo or higher)
  - Installed via **Android Studio > Settings > Languages & Frameworks > Android SDK > SDK Platforms**.

---

## 2. Opening & Importing into Android Studio

1. **Launch Android Studio**.
2. Click **Open** (or go to **File > Open...**).
3. Browse to the root folder of this project (the folder containing `settings.gradle.kts`, `build.gradle.kts`, and the `app/` folder) and click **OK**.
4. When prompted:
   - Choose **Trust Project**.
   - Android Studio will start an automatic **Gradle Sync**.
5. **Verify JDK configuration**:
   - Go to **File > Settings** (or **Android Studio > Settings** on macOS).
   - Navigate to **Build, Execution, Deployment > Build Tools > Gradle**.
   - Under **Gradle JDK**, make sure it points to **Embedded JDK (Java 17/21)**.
6. Once the Gradle sync status at the bottom right shows `BUILD SUCCESSFUL`, the project is fully indexed and ready.

---

## 3. Running the App

### Option A: From Android Studio GUI (Recommended)
1. In the top toolbar, ensure the run configuration dropdown says **`app`**.
2. In the device selection dropdown, select:
   - A physical Android device connected via USB or Wi-Fi (with **USB Debugging** enabled in Developer Options).
   - Or an Android Emulator (Pixel 7 / 8 / 9 with API 31+ recommended).
3. Click the green **Run (▶)** button or press `Shift + F10` (`Control + R` on macOS).
4. Android Studio will build the APK, install it on the device, and launch `MainActivity`.

### Option B: From the Terminal / Command Line
Open the terminal at the root of the project:

#### 1. Install & Launch Directly on Connected Device / Emulator
```bash
./gradlew installDebug
```
*Tip: On Windows PowerShell, use `.\gradlew.bat installDebug`.*

#### 2. Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

#### 3. Build Production Release App Bundle (AAB for Google Play Store)
```bash
./gradlew bundleRelease
```
The production bundle will be located at:
```
app/build/outputs/bundle/release/app-release.aab
```

#### 4. Run Local Unit Tests
```bash
./gradlew testDebugUnitTest
```

#### 5. Clean Build Cache
If you ever encounter stale caches after moving files or editing assets:
```bash
./gradlew clean
```

---

## 4. Debugging & Inspection in Android Studio

### 1. Setting Breakpoints
- You can place breakpoints in any `.kt` file (e.g. `SqlSandboxEngine.kt`, `QuestionDetailViewModel.kt`, `SignInViewModel.kt`) by clicking the line number in the editor gutter.
- Click the **Debug (🐞)** button (`Shift + F9`) instead of Run.
- The debugger pauses execution when queries are processed or when UI events trigger coroutine dispatches.

### 2. Live Database Inspector (App Inspection)
DoomSQL stores progress, drafts, and streaks locally in **Room (SQLite)**. You can inspect and modify this database in real time:
1. Run the app on an emulator or a device running Android 8.0+.
2. At the bottom toolbar of Android Studio, open the **App Inspection** tab.
3. Select **Database Inspector**.
4. You will see `doomsql.db`. You can:
   - View tables: `problem_stats`, `daily_activities`, `query_drafts`.
   - Run live SQL queries directly against the device's Room database.
   - Toggle **Live updates** to watch solve stats and streak records change in real-time as you solve problems.

### 3. Logcat Output
To monitor application logs, SQLite execution feedback, and authentication status:
1. Open the **Logcat** tab at the bottom of Android Studio (`Alt + 6` / `Cmd + 6`).
2. Filter by package:
   ```
   package:com.manish.doomsql
   ```
   or search for specific tags like `DoomSql` or `FirebaseAuthRepository`.

---

## 5. SQL Question Generator: Automatic vs Manual Clarification

> **Frequently Asked Question:**
> *"Will the generate sql questions python program automatically generate random questions or should I put them manually? Does it only test it and put it in the app?"*

We have provided tools for **BOTH** workflows in the `generate_sql_ques/` folder:

### 1. Fully Automatic Generation (`generate_random_questions.py`)
**Yes!** The tool can procedurally generate complete, realistic SQL questions automatically without you typing them manually.
- It contains built-in templates and blueprints across E-commerce, HR/Salaries, Healthcare, Racing, Streaming, and Banking.
- It creates the table schemas, populates sample rows, generates the question prompt and tags, runs the reference SQL in SQLite, and computes the exact expected output.
- **To automatically generate and insert 3 questions into the app:**
  ```bash
  python3 generate_sql_ques/generate_random_questions.py --count 3 --add-to-app
  ```
- **To generate questions filtered by difficulty (EASY, MEDIUM, HARD):**
  ```bash
  python3 generate_sql_ques/generate_random_questions.py --count 1 --difficulty MEDIUM --add-to-app
  ```
- **To preview without saving:**
  ```bash
  python3 generate_sql_ques/generate_random_questions.py --count 1 --preview
  ```

### 2. Manual / Custom Question Tool (`add_question.py`)
If you have a **specific, custom question** you want to add:
- You do **not** need to manually compute outputs or edit `index.json`.
- Run the interactive wizard:
  ```bash
  python3 generate_sql_ques/add_question.py --interactive
  ```
- You type your problem title, table columns, and solution query. The script will test it in SQLite, compute the expected rows/columns, save the JSON to `app/src/main/assets/questions/`, and register it in `index.json`.

### 3. Test Suite Validator (`validate_questions.py`)
Whenever questions are added (either automatically or manually), run:
```bash
python3 generate_sql_ques/validate_questions.py
```
This tests every single question in the app by running its solution query in an in-memory SQLite sandbox and asserting that the outputs match 100%.

---

## 6. Firebase & Offline Behavior

- **No Setup Needed for Local Testing:** DoomSQL is **100% offline-first**. You can compile and test the app immediately without adding `google-services.json` or configuring Firebase.
- **Enabling Accounts:** When you are ready to enable Google Sign-In and Email authentication, follow the step-by-step instructions in [account_conf.md](account_conf.md).
