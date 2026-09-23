# DoomSQL Question Generator Tools

This directory contains standalone Python tools to create, validate, and procedurally generate questions for the **DoomSQL** Android app.

---

## Tools Included

### 1. `generate_random_questions.py` (Fully Automatic Generator)
Procedurally generates realistic, ready-to-use SQL interview and practice questions across EASY, MEDIUM, and HARD tiers.
- **Auto-generates:** Schemas, realistic sample data, problem prompts, tags, and solution queries.
- **Auto-computes:** Executes the solution in an in-memory SQLite sandbox and generates exact `expectedOutput` columns and rows.
- **Auto-registers:** Saves into `app/src/main/assets/questions/` and registers in `index.json`.

```bash
# Preview 1 random question on console
python3 generate_sql_ques/generate_random_questions.py --count 1 --preview

# Automatically generate 3 questions and insert them into the app
python3 generate_sql_ques/generate_random_questions.py --count 3 --add-to-app

# Generate specifically HARD or MEDIUM questions
python3 generate_sql_ques/generate_random_questions.py --count 2 --difficulty HARD --add-to-app
```

---

### 2. `add_question.py` (Custom / Interactive Question Builder)
Used when you have a **specific custom question** you want to add:
- Prompts you for question metadata, table schemas, sample rows, and solution query.
- Automatically tests the query against in-memory SQLite, computes `expectedOutput`, writes the JSON file, and updates `index.json`.

```bash
# Interactive step-by-step wizard
python3 generate_sql_ques/add_question.py --interactive

# From a draft JSON file
python3 generate_sql_ques/add_question.py --file my_question.json
```

---

### 3. `validate_questions.py` (Comprehensive Test Suite)
Validates the entire catalog in `app/src/main/assets/questions/` by:
- Verifying JSON syntax and required properties.
- Spinning up an in-memory SQLite instance for every question.
- Executing `solutionQuery` and asserting that actual outputs match `expectedOutput` 100%.

```bash
python3 generate_sql_ques/validate_questions.py
```

---

### 4. `question_template.json`
A boilerplate JSON template for writing questions manually by hand.
