#!/usr/bin/env python3
"""
DoomSQL - Question Builder & Validator CLI
Automatically generates, tests, and inserts new SQL questions into either:
  1. Local bundled assets: app/src/main/assets/questions/ (default)
  2. Remote content repo:  questions/ (via --remote), updating manifest.json

Usage:
  python3 add_question.py --interactive
  python3 add_question.py --interactive --remote
  python3 add_question.py --file my_question.json
  python3 add_question.py --file my_question.json --remote
"""

import os
import sys
import glob
import json
import sqlite3
import argparse
import subprocess

# Resolve paths
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
ASSETS_QUESTIONS_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "questions")
INDEX_PATH = os.path.join(ASSETS_QUESTIONS_DIR, "index.json")
REMOTE_QUESTIONS_DIR = os.path.join(PROJECT_ROOT, "questions")
BUILD_MANIFEST_SCRIPT = os.path.join(PROJECT_ROOT, "tools", "build_manifest.py")


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


def get_next_question_id(target_dir: str) -> str:
    """Find the next sql_XXX id based on files existing in target directory and assets."""
    existing_nums = []
    # Check target dir
    if os.path.exists(target_dir):
        for f in glob.glob(os.path.join(target_dir, "sql_*.json")):
            base = os.path.basename(f)
            num_part = base[4:-5]
            if num_part.isdigit():
                existing_nums.append(int(num_part))
    # Also check assets dir
    if os.path.exists(ASSETS_QUESTIONS_DIR):
        for f in glob.glob(os.path.join(ASSETS_QUESTIONS_DIR, "sql_*.json")):
            base = os.path.basename(f)
            num_part = base[4:-5]
            if num_part.isdigit():
                existing_nums.append(int(num_part))

    next_num = (max(existing_nums) + 1) if existing_nums else 1
    return f"sql_{next_num:03d}"


def save_question(question_data: dict, filename: str = None, is_remote: bool = False):
    """
    Saves question JSON into the target folder and updates index or manifest.
    """
    target_dir = REMOTE_QUESTIONS_DIR if is_remote else ASSETS_QUESTIONS_DIR
    os.makedirs(target_dir, exist_ok=True)

    # 1. Determine ID and filename
    q_id = question_data.get("id")
    if not q_id:
        q_id = get_next_question_id(target_dir)
        question_data["id"] = q_id

    if not filename:
        filename = f"{q_id}.json"

    # Ensure required fields
    if "contentVersion" not in question_data:
        question_data["contentVersion"] = 1
    if "sqlDialect" not in question_data:
        question_data["sqlDialect"] = "SQLITE"

    # 2. Compute expected output using real SQLite engine
    print(f"[*] Validating and computing expected output for question '{q_id}'...")
    computed_output = validate_and_compute_expected_output(question_data)
    question_data["expectedOutput"] = computed_output
    print(f"[✓] Query verified! Output shape: {len(computed_output['columns'])} cols, {len(computed_output['rows'])} rows.")

    # 3. Write question JSON file
    target_file = os.path.join(target_dir, filename)
    with open(target_file, "w", encoding="utf-8") as f:
        json.dump(question_data, f, indent=2, ensure_ascii=False)
    print(f"[✓] Saved question file: {target_file}")

    if is_remote:
        # Rebuild manifest.json
        if os.path.exists(BUILD_MANIFEST_SCRIPT):
            print("[*] Rebuilding questions/manifest.json...")
            res = subprocess.run([sys.executable, BUILD_MANIFEST_SCRIPT], capture_output=True, text=True)
            if res.returncode == 0:
                print(res.stdout.strip())
                print("[✓] Remote manifest updated and verified!")
                print("\n🚀 Next steps to publish to users (no app update needed):")
                print("   git add questions/")
                print(f"   git commit -m \"Add question {q_id}: {question_data.get('title', '')}\"")
                print("   git push origin main")
            else:
                print(f"[!] Warning: manifest build failed:\n{res.stderr}", file=sys.stderr)
    else:
        # 4. Update index.json in assets
        index_list = []
        if os.path.exists(INDEX_PATH):
            with open(INDEX_PATH, "r", encoding="utf-8") as f:
                index_list = json.load(f)

        if filename not in index_list:
            index_list.append(filename)
            with open(INDEX_PATH, "w", encoding="utf-8") as f:
                json.dump(index_list, f, indent=2, ensure_ascii=False)
            print(f"[✓] Added '{filename}' to index.json (Total bundled questions: {len(index_list)})")
        else:
            print(f"[*] '{filename}' already listed in index.json (Total bundled questions: {len(index_list)})")


def interactive_mode(is_remote: bool = False):
    dest_name = "Remote CDN Content Repo (questions/)" if is_remote else "Local Bundled Assets (app/src/.../assets/questions/)"
    print(f"=== DoomSQL Interactive Question Builder [{dest_name}] ===")
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
        try:
            rows = json.loads(rows_str)
        except Exception:
            rows = eval(rows_str)
    else:
        rows = [
            [1, "Alice", 75000.0],
            [2, "Bob", 62000.0],
            [3, "Charlie", 89000.0]
        ]

    print("\n--- Solution Query ---")
    solution_query = input("Solution SQL Query: ").strip()
    explanation = input("Explanation / Hint: ").strip()

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

    save_question(question_data, is_remote=is_remote)


def main():
    parser = argparse.ArgumentParser(description="DoomSQL Question Generator & Uploader")
    parser.add_argument("--file", "-f", help="Path to question JSON file to add")
    parser.add_argument("--interactive", "-i", action="store_true", help="Launch interactive step-by-step wizard")
    parser.add_argument("--remote", "-r", action="store_true", help="Add to remote content repo (questions/) instead of bundled assets")

    args = parser.parse_args()

    if args.interactive:
        interactive_mode(is_remote=args.remote)
    elif args.file:
        if not os.path.exists(args.file):
            print(f"[!] File not found: {args.file}", file=sys.stderr)
            sys.exit(1)
        with open(args.file, "r", encoding="utf-8") as f:
            data = json.load(f)
        save_question(data, os.path.basename(args.file), is_remote=args.remote)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
