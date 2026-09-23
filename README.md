# DoomSQL - Interactive SQL Learning & Practice App

DoomSQL is a modern, high-performance Android application built with Kotlin and Jetpack Compose for mastering SQL through hands-on practice.

---

## 🌟 Key Features

### 1. In-Memory Isolated SQLite Sandbox
- Every query execution runs inside an isolated in-memory SQLite sandbox (`androidx.sqlite.driver.bundled`).
- Tables and sample rows are initialized fresh for each run, guaranteeing zero corruption or side effects across executions.
- Support for complex queries: aggregations, joins, subqueries, `CASE WHEN`, window functions, and recursive CTEs.

### 2. Intelligent Query Result Comparator
- **Diff & Precision:** Compares student query output against reference solutions.
- **Order-Aware:** Automatically distinguishes between order-sensitive problems (`ORDER BY`) and unordered result sets.
- **Type-Safe Tolerance:** Handles integer vs floating-point representations (`100` vs `100.0`) seamlessly.
- **Diagnostic Feedback:** Highlights column header mismatches, row count discrepancies, and specific row data differences.

### 3. Developer-First SQL Editor
- **Keyword Auto-Complete:** Fast-tap chips for essential SQL keywords (`SELECT`, `FROM`, `WHERE`, `JOIN`, `GROUP BY`, etc.).
- **Smart Formatting:** Auto-indents and formats SQL queries with a single tap.
- **Persistent Drafts:** Saves in-progress queries automatically per question so work is never lost.
- **Interactive Tools:** Undo/redo controls, query clear, and schema drawer integration.

### 4. Interactive Schema Inspector
- Side drawer and tabbed view to inspect problem tables, column data types (`INTEGER`, `TEXT`, `REAL`), nullability, and sample rows.
- Detailed reference solution modal with spoiler prevention confirmation.

### 5. Gamification, Streaks & Progress Tracking
- **Daily Activity Heatmap:** Records dates and solved counts with streak calculations.
- **Weekly Practice Goals:** Configurable weekly target with progress visualization.
- **Difficulty Badges:** Categorized by EASY, MEDIUM, and HARD.
- **Local Persistence with Room:** All user progress, streaks, and drafts are saved locally on the device using SQLite/Room.

### 6. Optional Cloud Accounts (Firebase Auth)
- **100% Offline-First:** The app runs completely offline without requiring any login or network connection.
- **Google Sign-In:** Built with modern Android Credential Manager (`androidx.credentials` + `googleid`), strictly avoiding deprecated libraries.
- **Email & Password:** Includes password validation, email verification notices, and password reset flows.
- **Google Play Compliant:** Supports both in-app account deletion (with optional local progress preservation) and external web deletion.

---

## 📂 Project Structure

```
app/src/main/
├── assets/
│   └── questions/                 # Question catalog and index.json
├── java/com/manish/doomsql/
│   ├── config/                    # Global constants and app web links
│   ├── data/
│   │   ├── engine/                # In-memory SQLite sandbox engine & diff comparator
│   │   ├── local/                 # Room database, DAOs, entities, and migrations
│   │   ├── model/                 # Question, Table, Column, AuthUser models
│   │   └── repository/            # QuestionRepository, AuthRepository, FirebaseAuthRepository
│   ├── di/                        # AppContainer and dependency injection
│   ├── ui/
│   │   ├── components/            # Reusable Compose widgets (EditorSpeedDial, TopBar, etc.)
│   │   ├── navigation/            # Type-safe navigation routes (Home, Questions, Detail, Settings, SignIn)
│   │   ├── screens/               # Screen composables and ViewModels
│   │   │   ├── auth/              # SignInScreen and SignInViewModel
│   │   │   ├── detail/            # QuestionDetailScreen and QuestionDetailViewModel
│   │   │   ├── home/              # HomeScreen
│   │   │   ├── progress/          # ProgressScreen
│   │   │   ├── questions/         # QuestionsListScreen and QuestionsViewModel
│   │   │   └── settings/          # SettingsScreen
│   │   └── theme/                 # Material 3 ColorScheme, Typography, Shapes
│   ├── util/                      # SQL formatters, validators, syntax highlighters
│   ├── DoomSqlApplication.kt      # Application class initializing AppContainer
│   └── MainActivity.kt            # Entry activity with edge-to-edge Scaffold and NavHost
└── res/
    ├── drawable/                  # Vector icons and custom launcher foreground
    ├── mipmap-*/                  # Adaptive launcher icon definitions
    └── values/                    # strings.xml, colors.xml, themes.xml
```

---

## 🛠️ Adding New SQL Questions

We provide full automation scripts to generate, test, and insert questions.
- Refer to [upload_sql_questions.md](upload_sql_questions.md) for full instructions.
- Tools are located in `generate_sql_ques/`:
  - `python3 generate_sql_ques/validate_questions.py` — Validates all questions against SQLite.
  - `python3 generate_sql_ques/add_question.py --interactive` — CLI wizard to create new questions.

---

## 🔐 Firebase & Cloud Account Setup

Refer to [account_conf.md](account_conf.md) for step-by-step instructions on:
- Creating your Firebase project.
- Adding package `com.manish.doomsql` with debug and release SHA certificates.
- Placing `google-services.json` in `app/`.
- Enabling Google and Email/Password authentication.

---

## 🚀 Building & Testing

### Build APK
```bash
gradle assembleDebug
```

### Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Validate Questions
```bash
python3 generate_sql_ques/validate_questions.py
```
