# DoomSQL — Remote Question Publishing Guide

This document describes how to add, edit, verify, and publish new SQL practice questions for DoomSQL without needing to release an app update or wait for Google Play Store review.

---

## 1. How It Works (Architecture Overview)

When you publish questions to your public GitHub content repository, DoomSQL fetches them automatically via the fast, globally distributed **jsDelivr CDN**:

```text
You write/edit questions in GitHub repo
   ↓
Run python tools/build_manifest.py (verifies every solution against SQLite)
   ↓
git commit & push to GitHub (main branch)
   ↓
jsDelivr CDN caches & serves:
https://cdn.jsdelivr.net/gh/<USER>/doomsql-content@main/questions/manifest.json
   ↓
User's Android Device:
1. Periodic 24h background check on app launch (silent, non-blocking)
   OR manual tap in Settings > "Check for new questions"
2. Downloads manifest.json with hourly cache-buster query (?v=<hourly>)
3. For new or updated questions:
   - Downloads JSON
   - Validates SHA-256 hash against manifest
   - Validates JSON schema & row/column arity
   - Validates SQL safety (SELECT/WITH only, no ATTACH/PRAGMA)
   - Runs in-memory SQLite sandbox self-check (solutionQuery == expectedOutput)
   - Atomically saves to internal storage: filesDir/questions/<id>.json
   - Updates Room searchable index
4. New questions immediately appear in the app with a "NEW" badge!
```

### Storage Layers (Offline-First Guarantee)

| Layer | Location | Purpose |
|---|---|---|
| **Layer 1** | `filesDir/questions/` (device disk) | Downloaded & updated remote questions. Stored permanently on device. Works offline forever in Airplane Mode. |
| **Layer 2** | `assets/questions/` (inside APK) | Offline baseline questions bundled at build time. Never removed. |
| **Index & State** | Room Database | Searchable question index, solve records, streaks, and user query drafts. **Remote sync never touches progress tables.** |

---

## 2. One-Time Setup: Create Your Content Repository

1. Create a **public** repository on GitHub, for example: `doomsql-content`
   *(Must be public so jsDelivr can serve it without API keys or tokens).*
2. Set up the following directory structure in `doomsql-content`:
   ```text
   doomsql-content/
   ├── questions/
   │   ├── manifest.json
   │   ├── sql_001.json
   │   ├── sql_002.json
   │   └── ...
   ├── tools/
   │   └── build_manifest.py
   ├── .github/
   │   └── workflows/
   │       └── verify.yml
   └── README.md
   ```
3. Copy `tools/build_manifest.py` and `.github/workflows/verify.yml` from this project into your content repository.
4. In `RemoteQuestionConfig.kt` (in DoomSQL Android app), verify `DEFAULT_REPO` matches your GitHub username and repository name:
   ```kotlin
   const val DEFAULT_REPO = "<YOUR_GITHUB_USERNAME>/doomsql-content"
   ```

---

## 3. Step-by-Step: Adding a New Question

### Step 1: Create the Question JSON File
In `questions/`, create a new file named `sql_<number>.json` (e.g. `questions/sql_013.json`):

```json
{
  "id": "sql_013",
  "contentVersion": 1,
  "title": "Monthly Revenue Growth Rate",
  "difficulty": "MEDIUM",
  "sqlDialect": "SQLITE",
  "tags": ["WINDOW FUNCTIONS", "LAG", "ROUND"],
  "description": "Calculate the month-over-month revenue growth percentage for each month.",
  "orderSensitive": true,
  "tables": [
    {
      "name": "monthly_sales",
      "columns": [
        { "name": "sale_month", "type": "TEXT", "primaryKey": true },
        { "name": "revenue", "type": "REAL", "nullable": false }
      ],
      "rows": [
        ["2026-01", 10000.0],
        ["2026-02", 12500.0],
        ["2026-03", 15000.0]
      ]
    }
  ],
  "expectedOutput": {
    "columns": ["sale_month", "growth_pct"],
    "rows": [
      ["2026-01", null],
      ["2026-02", 25.0],
      ["2026-03", 20.0]
    ]
  },
  "solutionQuery": "SELECT sale_month, ROUND((revenue - LAG(revenue) OVER (ORDER BY sale_month)) * 100.0 / LAG(revenue) OVER (ORDER BY sale_month), 2) AS growth_pct FROM monthly_sales ORDER BY sale_month;",
  "explanation": "Use LAG() to access the previous month revenue and compute the percentage difference.",
  "addedAt": "2026-09-28",
  "minAppVersionCode": 1
}
```

### Step 2: Run the Manifest Generator & Verification Script
Run the automated build script from the root of your content repository:

```bash
python3 tools/build_manifest.py
```

**What this script does:**
1. Spins up an in-memory SQLite database for every question file.
2. Creates the declared tables and inserts the sample rows.
3. Executes your `solutionQuery`.
4. Compares the result against `expectedOutput` using DoomSQL's exact comparison rules.
5. If there is **any mismatch, syntax error, or duplicate ID**, the script **aborts immediately** and outputs the exact problem.
6. If all questions pass, it computes SHA-256 hashes, auto-increments `manifestVersion`, and writes `questions/manifest.json`.

Output:
```text
OK  13 questions verified
OK  manifest.json written at version 4
```

> ⚠️ **Important:** Do NOT edit `manifest.json` by hand. Always run `python3 tools/build_manifest.py`.

### Step 3: Commit and Push to GitHub
```bash
git add questions/ tools/ .github/
git commit -m "Add question sql_013: Monthly Revenue Growth Rate"
git push origin main
```

### Step 4: Verify in the App
1. Open DoomSQL on your device.
2. Go to **Settings > About & Support**.
3. Tap **"Check for new questions"**.
4. You will see:
   - Inline status changes to *"Checking…"*.
   - Within seconds: *"Added 1 new question"* (or *"You're up to date"*).
   - "Question pack version" bumps to `v4`.
5. Open the **Questions** tab:
   - You will see a banner: *"✨ 1 new question added"*.
   - The question card displays a cyan **"NEW"** badge.

---

## 4. Editing an Existing Question

If you need to fix a typo, update a hint, or improve sample data:

1. Open the existing `sql_<id>.json` file.
2. **Increment `contentVersion` by 1** (e.g. from `1` to `2`).
   *(The app compares local vs remote `contentVersion` to decide whether to download updates).*
3. Update the description, tables, or solution.
4. Run the build script to verify and regenerate the manifest:
   ```bash
   python3 tools/build_manifest.py
   ```
5. Commit and push:
   ```bash
   git commit -am "Update sql_005 explanation and sample data"
   git push origin main
   ```
6. When the app syncs, it automatically replaces the old version on disk without wiping any user solve history.

---

## 5. Security & Google Play Compliance

- **Device & Network Abuse Policy Compliant:**
  DoomSQL downloads **only static JSON content**, never executable code (`.dex`, `.jar`, `.so`, or dynamic reflection scripts).
- **Four-Step Verification Gate:**
  Before saving any downloaded question to disk, the app performs 4 strict checks:
  1. **SHA-256 Check:** Byte-level integrity check against the manifest hash.
  2. **Schema & Arity Check:** Verifies JSON structure and table column/row parity.
  3. **SQL Validator:** Enforces read-only query standards (`SELECT`, `WITH`, `VALUES` only; blocks `ATTACH`, `DETACH`, `PRAGMA`, `VACUUM`).
  4. **Sandbox Self-Check:** Executes `solutionQuery` in a fresh, ephemeral SQLite database with `PRAGMA query_only = ON` and verifies that the output matches `expectedOutput`.
- **Atomic File Writing:** Downloaded files are saved to `tmp_<id>.json` and only renamed to `<id>.json` after all checks pass. A network drop mid-download never corrupts existing questions.

---

## 6. Useful Commands & Automated Tools Cheat Sheet

### 1. Generating & Adding Questions to Remote Content Repo Automatically
You do not need to create questions manually if you want quick additions:
```bash
# Generate 1 random question and add directly to remote content repository (questions/):
python3 generate_sql_ques/generate_random_questions.py --count 1 --add-to-remote

# Generate 3 MEDIUM or HARD questions and add directly to remote repo:
python3 generate_sql_ques/generate_random_questions.py --count 3 --difficulty MEDIUM --add-to-remote
python3 generate_sql_ques/generate_random_questions.py --count 2 --difficulty HARD --add-to-remote
```
*(This automatically creates the JSON file, tests the query in SQLite, updates `manifest.json`, increments `manifestVersion`, and displays git commands).*

### 2. Interactive Wizard for Custom Questions
```bash
# Launch the interactive CLI wizard and save to remote content repo:
python3 generate_sql_ques/add_question.py --interactive --remote

# Or import an existing draft JSON into remote questions:
python3 generate_sql_ques/add_question.py --file my_question.json --remote
```

### 3. Validating Questions & Rebuilding Manifest
```bash
# Validate all remote questions in SQLite:
python3 generate_sql_ques/validate_questions.py --remote

# Verify questions and rebuild manifest.json:
python3 tools/build_manifest.py
```

### 4. Publishing to GitHub (No App Update Needed)
```bash
git add questions/
git commit -m "Publish new SQL practice questions"
git push origin main
```

### 5. In-App Auto-Update Behavior
- **Auto-Sync on Internet Connection:** The app monitors device connectivity via Android `ConnectivityManager.NetworkCallback`. Whenever the device connects to Wi-Fi or mobile data, or when the app is opened, it automatically checks the manifest.
- **Instant Reactive UI:** As soon as questions are downloaded and verified against the SQLite sandbox, `questionsFlow` emits and the questions list updates immediately without restarting the app.
- **Manual Check:** In **Settings > About & Support > "Check for new questions"**, users can tap anytime to force an immediate refresh.

| Task | Command |
|---|---|
| Auto-generate random remote questions | `python3 generate_sql_ques/generate_random_questions.py --count 1 --add-to-remote` |
| Interactive custom question builder | `python3 generate_sql_ques/add_question.py --interactive --remote` |
| Validate remote questions | `python3 generate_sql_ques/validate_questions.py --remote` |
| Verify questions & build manifest | `python3 tools/build_manifest.py` |
| Push to GitHub | `git add questions/ && git commit -m "..." && git push origin main` |
| Check manifest in browser | `https://cdn.jsdelivr.net/gh/<USER>/<REPO>@main/questions/manifest.json` |
| View question in browser | `https://cdn.jsdelivr.net/gh/<USER>/<REPO>@main/questions/sql_001.json` |
| Force fresh cache on jsDelivr | Append `?v=<timestamp>` query parameter to the URL |
