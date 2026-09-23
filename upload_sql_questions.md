# Guide: How to Add & Upload New SQL Questions to DoomSQL

This guide explains how SQL questions are stored in DoomSQL, how to manually create and edit them, and how to use the automated Python tools in `generate_sql_ques/` to generate, test, and register questions effortlessly.

---

## 1. Where Questions Are Stored

All questions in DoomSQL are bundled as individual JSON files in the Android assets directory:

```
app/src/main/assets/questions/
├── index.json          <-- Manifest listing question filenames in display order
├── sql_001.json        <-- Question 1
├── sql_002.json        <-- Question 2
├── ...
└── sql_012.json        <-- Question 12
```

Whenever the app launches or loads the question catalog, it reads `index.json` to know which files to load and in what order.

---

## 2. Question JSON Structure

Each question file (e.g., `sql_013.json`) must follow this exact schema:

```json
{
  "id": "sql_013",
  "contentVersion": 1,
  "title": "High Earners in Engineering",
  "difficulty": "EASY",
  "sqlDialect": "SQLITE",
  "tags": ["select", "where", "filter"],
  "description": "Find the names of all employees in department 10 who earn more than 60000. Return a single column named name.",
  "orderSensitive": false,
  "tables": [
    {
      "name": "Employee",
      "columns": [
        { "name": "id", "type": "INTEGER", "nullable": false, "primaryKey": true },
        { "name": "name", "type": "TEXT", "nullable": false, "primaryKey": false },
        { "name": "salary", "type": "REAL", "nullable": true, "primaryKey": false },
        { "name": "department_id", "type": "INTEGER", "nullable": true, "primaryKey": false }
      ],
      "rows": [
        [1, "John", 60000, 10],
        [2, "Alice", 75000, 10],
        [3, "David", 55000, 10],
        [4, "Emma", 82000, 10],
        [5, "Michael", 90000, 20]
      ]
    }
  ],
  "expectedOutput": {
    "columns": ["name"],
    "rows": [
      ["Alice"],
      ["Emma"]
    ]
  },
  "solutionQuery": "SELECT name FROM Employee WHERE department_id = 10 AND salary > 60000;",
  "explanation": "Filter rows where department_id is 10 and salary is strictly greater than 60000 using WHERE with AND."
}
```

### Field Breakdown:
| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique identifier (e.g. `"sql_013"`). |
| `contentVersion` | `Int` | Integer version (start with `1`). Incremented if you revise a question. |
| `title` | `String` | Short, engaging problem title shown in the list. |
| `difficulty` | `String` | Must be `"EASY"`, `"MEDIUM"`, or `"HARD"`. |
| `sqlDialect` | `String` | Use `"SQLITE"`. |
| `tags` | `List<String>` | Topic categories (e.g. `["join", "group by", "aggregate", "subquery", "window"]`). |
| `description` | `String` | Clear statement of the problem, required column names, and conditions. |
| `orderSensitive` | `Boolean` | `false` if row order doesn't matter (default). `true` if `ORDER BY` is required. |
| `tables` | `List<Table>` | Initial database schema and sample data loaded into the in-memory SQLite sandbox. |
| `tables[].columns` | `List<Col>` | `name`, `type` (`INTEGER`, `TEXT`, `REAL`), `nullable` (boolean), `primaryKey` (boolean). |
| `tables[].rows` | `List<List>` | List of rows matching the column order and types. |
| `expectedOutput` | `Object` | Contains `columns` (list of column names) and `rows` (expected result set). |
| `solutionQuery` | `String` | The reference SQL query that solves the problem. |
| `explanation` | `String` | Practical explanation revealed when the user clicks "View Solution". |

---

## 3. Manual Step-by-Step Procedure

If you prefer to add a question by hand:

1. **Pick the next ID**: Look at the last entry in `app/src/main/assets/questions/index.json`. For example, if `sql_012.json` is the last, your new question will be `sql_013.json`.
2. **Copy the template**: Copy `generate_sql_ques/question_template.json` to `app/src/main/assets/questions/sql_013.json`.
3. **Fill in details**:
   - Write your `title`, `difficulty`, `tags`, and `description`.
   - Define the `tables` with sample `rows`.
   - Write the `solutionQuery`.
   - Compute the `expectedOutput` columns and rows.
   - Write an `explanation`.
4. **Register in `index.json`**:
   Open `app/src/main/assets/questions/index.json` and add `"sql_013.json"` to the array:
   ```json
   [
     "sql_001.json",
     ...
     "sql_012.json",
     "sql_013.json"
   ]
   ```
5. **Verify**: Run the validation script to make sure there are no typos or SQL syntax errors:
   ```bash
   python3 generate_sql_ques/validate_questions.py
   ```

---

## 4. Automated Addition Using Python Scripts (Recommended)

To avoid manually calculating the `expectedOutput` or editing `index.json`, use the scripts in `generate_sql_ques/`.

### Method A: Interactive CLI Wizard
Run the interactive wizard from your terminal:
```bash
python3 generate_sql_ques/add_question.py --interactive
```
1. It prompts you for the question title, difficulty, description, and tags.
2. It prompts you for table definitions and sample rows.
3. You enter the `solutionQuery`.
4. The script **automatically boots an in-memory SQLite sandbox, executes your query, computes the exact `expectedOutput`, saves the JSON file, and updates `index.json`**!

### Method B: From a Draft JSON File
Create a draft file (e.g. `my_draft.json`) with your tables and `solutionQuery` (you can leave `expectedOutput` empty):
```bash
python3 generate_sql_ques/add_question.py --file my_draft.json
```
The script will compute the `expectedOutput`, format the file, save it to `app/src/main/assets/questions/`, and add it to `index.json`.

---

## 5. Validating Your Question Suite

Always run the test suite after adding or modifying questions:
```bash
python3 generate_sql_ques/validate_questions.py
```
This script:
- Verifies every question listed in `index.json` exists.
- Validates the JSON syntax and required keys.
- Spins up an in-memory SQLite database for each question, executes its `solutionQuery`, and asserts that the query result matches `expectedOutput` down to data types and values.
