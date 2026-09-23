#!/usr/bin/env python3
"""
DoomSQL - Question Builder & Validator CLI
Automatically generates, tests, and inserts new SQL questions into the DoomSQL app assets.

Usage:
  python3 add_question.py --help
  python3 add_question.py --file my_new_question.json
  python3 add_question.py --interactive
"""

import os
import sys
import json
import sqlite3
import argparse

# Resolve paths
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
QUESTIONS_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "questions")
INDEX_PATH = os.path.join(QUESTIONS_DIR, "index.json")


def validate_and_compute_expected_output(question_data: dict) -> dict:
    """
    Spins up an in-memory SQLite database, creates the tables, inserts the rows,
    executes the solution query, and automatically populates `expectedOutput`.
    """
    tables = question_data.get("tables", [])
    solution_query = question_data.get("solutionQuery", "").strip()

    if not tables:
        raise ValueError("Question must have at least one table definition.")
    if not solution_query:
        raise ValueError("Question must have a non-empty 'solutionQuery'.")

    # Connect to in-memory SQLite sandbox
    conn = sqlite3.connect(":memory:")
    cursor = conn.cursor()

    try:
        # Create each table
        for table in tables:
            t_name = table["name"]
            cols_def = []
            for col in table["columns"]:
                pk_str = " PRIMARY KEY" if col.get("primaryKey") else ""
                not_null = " NOT NULL" if not col.get("nullable", True) else ""
                cols_def.append(f'"{col["name"]}" {col["type"]}{pk_str}{not_null}')

            create_sql = f'CREATE TABLE "{t_name}" ({", ".join(cols_def)});'
            cursor.execute(create_sql)

            # Insert sample rows
            rows = table.get("rows", [])
            if rows:
                placeholders = ", ".join(["?"] * len(table["columns"]))
                insert_sql = f'INSERT INTO "{t_name}" VALUES ({placeholders});'
                cursor.executemany(insert_sql, rows)

        conn.commit()

        # Run solution query
        cursor.execute(solution_query)
        result_rows = cursor.fetchall()
        result_columns = [desc[0] for desc in cursor.description] if cursor.description else []

        # Convert tuples to lists for JSON serialization
        converted_rows = [list(r) for r in result_rows]

        computed_output = {
            "columns": result_columns,
            "rows": converted_rows
        }

        return computed_output

    finally:
        conn.close()


def save_question(question_data: dict, filename: str = None):
    """
    Saves question JSON into the assets folder and updates index.json.
    """
    os.makedirs(QUESTIONS_DIR, exist_ok=True)

    # 1. Determine ID and filename
    q_id = question_data.get("id")
    if not q_id:
        # Auto-generate next ID by reading index.json
        if os.path.exists(INDEX_PATH):
            with open(INDEX_PATH, "r", encoding="utf-8") as f:
                index_list = json.load(f)
            next_num = len(index_list) + 1
        else:
            next_num = 1
        q_id = f"sql_{next_num:03d}"
        question_data["id"] = q_id

    if not filename:
        filename = f"{q_id}.json"

    # 2. Compute expected output using real SQLite engine
    print(f"[*] Validating and computing expected output for question '{q_id}'...")
    computed_output = validate_and_compute_expected_output(question_data)
    question_data["expectedOutput"] = computed_output
    print(f"[✓] Query verified! Output shape: {len(computed_output['columns'])} cols, {len(computed_output['rows'])} rows.")

    # 3. Write question JSON file
    target_file = os.path.join(QUESTIONS_DIR, filename)
    with open(target_file, "w", encoding="utf-8") as f:
        json.dump(question_data, f, indent=2, ensure_ascii=False)
    print(f"[✓] Saved question file: {target_file}")

    # 4. Update index.json
    index_list = []
    if os.path.exists(INDEX_PATH):
        with open(INDEX_PATH, "r", encoding="utf-8") as f:
            index_list = json.load(f)

    if filename not in index_list:
        index_list.append(filename)
        with open(INDEX_PATH, "w", encoding="utf-8") as f:
            json.dump(index_list, f, indent=2, ensure_ascii=False)
        print(f"[✓] Added '{filename}' to index.json (Total questions: {len(index_list)})")
    else:
        print(f"[*] '{filename}' already listed in index.json (Total questions: {len(index_list)})")


def interactive_mode():
    print("=== DoomSQL Interactive Question Builder ===")
    title = input("Question Title: ").strip()
    difficulty = input("Difficulty (EASY / MEDIUM / HARD) [EASY]: ").strip().upper() or "EASY"
    description = input("Description / Problem prompt: ").strip()
    tags_str = input("Tags (comma separated, e.g. select,where,join): ").strip()
    tags = [t.strip().lower() for t in tags_str.split(",") if t.strip()]

    print("\n--- Table Setup ---")
    table_name = input("Table name [Employees]: ").strip() or "Employees"
    print("Columns (comma separated with types, e.g. id:INTEGER:pk, name:TEXT, salary:REAL):")
    cols_input = input("Columns: ").strip() or "id:INTEGER:pk, name:TEXT, salary:REAL"

    columns = []
    for col_def in cols_input.split(","):
        parts = [p.strip() for p in col_def.split(":")]
        c_name = parts[0]
        c_type = parts[1].upper() if len(parts) > 1 else "TEXT"
        is_pk = (len(parts) > 2 and parts[2].lower() in ("pk", "primary", "primarykey"))
        columns.append({
            "name": c_name,
            "type": c_type,
            "nullable": not is_pk,
            "primaryKey": is_pk
        })

    print(f"Sample Rows for '{table_name}' (Enter as Python lists or JSON array, or press Enter for default sample):")
    rows_str = input("Rows: ").strip()
    if rows_str:
        rows = json.loads(rows_str)
    else:
        rows = [
            [1, "Alice", 70000.0],
            [2, "Bob", 48000.0],
            [3, "Charlie", 95000.0]
        ]

    solution_query = input("\nSolution Query (e.g. SELECT name FROM Employees WHERE salary > 50000;):\n> ").strip()
    explanation = input("Explanation for the user: ").strip()

    question_data = {
        "contentVersion": 1,
        "title": title,
        "difficulty": difficulty,
        "sqlDialect": "SQLITE",
        "tags": tags,
        "description": description,
        "orderSensitive": False,
        "tables": [
            {
                "name": table_name,
                "columns": columns,
                "rows": rows
            }
        ],
        "solutionQuery": solution_query,
        "explanation": explanation
    }

    save_question(question_data)


def main():
    parser = argparse.ArgumentParser(description="DoomSQL Question Generator & Uploader")
    parser.add_argument("--file", "-f", help="Path to question JSON file to add")
    parser.add_argument("--interactive", "-i", action="store_true", help="Launch interactive step-by-step wizard")

    args = parser.parse_args()

    if args.interactive:
        interactive_mode()
    elif args.file:
        if not os.path.exists(args.file):
            print(f"[!] File not found: {args.file}", file=sys.stderr)
            sys.exit(1)
        with open(args.file, "r", encoding="utf-8") as f:
            data = json.load(f)
        save_question(data, os.path.basename(args.file))
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
