# DoomSQL Question Generator Tools

This directory contains standalone Python tools to create, validate, and manage questions for the **DoomSQL** Android app.

---

## Files

1. **`add_question.py`**:
   - Automated CLI script that takes question metadata and table definitions, runs the query in an in-memory SQLite sandbox, automatically computes the exact `expectedOutput` columns and rows, saves the JSON in `app/src/main/assets/questions/`, and updates `index.json`.

2. **`validate_questions.py`**:
   - Automated test suite that scans every question in the app, spins up in-memory SQLite tables, executes the `solutionQuery`, and compares the result against `expectedOutput` using the same precision and comparison rules as DoomSQL.

3. **`question_template.json`**:
   - A boilerplate JSON file showing every required field for creating a new question manually.

---

## Quick Usage

### 1. Validate All Questions in the App
```bash
python3 generate_sql_ques/validate_questions.py
```

### 2. Add a Question Interactively
```bash
python3 generate_sql_ques/add_question.py --interactive
```
The wizard will guide you through the title, difficulty, tables, schema, sample rows, solution SQL, and explanation.

### 3. Add a Question from a JSON File
```bash
python3 generate_sql_ques/add_question.py --file my_new_question.json
```
This automatically tests the query against the database tables, generates `expectedOutput`, writes it to `app/src/main/assets/questions/`, and registers it in `index.json`.
