#!/usr/bin/env python3
"""
DoomSQL - Question Suite Validator
Scans all questions in app/src/main/assets/questions/, executes each question's
solutionQuery in an isolated in-memory SQLite instance, and verifies that the output
matches the declared expectedOutput exactly.
"""

import os
import sys
import json
import sqlite3

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
QUESTIONS_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "questions")
INDEX_PATH = os.path.join(QUESTIONS_DIR, "index.json")


def cells_equal(a, b) -> bool:
    if a is None and b is None:
        return True
    if a is None or b is None:
        return False
    # If both are numbers (or numeric strings), compare as floats
    try:
        fa = float(a)
        fb = float(b)
        return abs(fa - fb) < 1e-6
    except (ValueError, TypeError):
        pass
    return str(a).strip() == str(b).strip()


def rows_equal(r1, r2) -> bool:
    if len(r1) != len(r2):
        return False
    return all(cells_equal(c1, c2) for c1, c2 in zip(r1, r2))


def validate_question_file(file_path: str) -> bool:
    filename = os.path.basename(file_path)
    with open(file_path, "r", encoding="utf-8") as f:
        try:
            q = json.load(f)
        except json.JSONDecodeError as e:
            print(f"[FAIL] {filename}: Invalid JSON format ({e})")
            return False

    q_id = q.get("id", filename)
    required_keys = ["id", "title", "difficulty", "description", "tables", "expectedOutput", "solutionQuery"]
    missing = [k for k in required_keys if k not in q]
    if missing:
        print(f"[FAIL] {filename} ({q_id}): Missing required keys: {missing}")
        return False

    # Check difficulty
    if q["difficulty"] not in ("EASY", "MEDIUM", "HARD"):
        print(f"[FAIL] {filename} ({q_id}): Invalid difficulty '{q['difficulty']}'. Must be EASY, MEDIUM, or HARD.")
        return False

    # Check SQLite execution
    conn = sqlite3.connect(":memory:")
    cursor = conn.cursor()

    try:
        for t in q["tables"]:
            t_name = t["name"]
            cols = []
            for col in t["columns"]:
                pk = " PRIMARY KEY" if col.get("primaryKey") else ""
                not_null = " NOT NULL" if not col.get("nullable", True) else ""
                cols.append(f'"{col["name"]}" {col["type"]}{pk}{not_null}')
            cursor.execute(f'CREATE TABLE "{t_name}" ({", ".join(cols)});')
            if t.get("rows"):
                ph = ", ".join(["?"] * len(t["columns"]))
                cursor.executemany(f'INSERT INTO "{t_name}" VALUES ({ph});', t["rows"])
        conn.commit()

        cursor.execute(q["solutionQuery"])
        actual_rows = [list(r) for r in cursor.fetchall()]
        actual_cols = [d[0] for d in cursor.description] if cursor.description else []

        exp = q["expectedOutput"]
        exp_cols = exp.get("columns", [])
        exp_rows = exp.get("rows", [])

        # Check column names (case-insensitive)
        if [c.lower() for c in actual_cols] != [c.lower() for c in exp_cols]:
            print(f"[FAIL] {filename} ({q_id}): Column mismatch!\n  Expected: {exp_cols}\n  Got:      {actual_cols}")
            return False

        # Check row count
        if len(actual_rows) != len(exp_rows):
            print(f"[FAIL] {filename} ({q_id}): Row count mismatch! Expected {len(exp_rows)}, got {len(actual_rows)}")
            return False

        # Check row content
        if q.get("orderSensitive", False):
            for i, (act, ex) in enumerate(zip(actual_rows, exp_rows)):
                if not rows_equal(act, ex):
                    print(f"[FAIL] {filename} ({q_id}): Row #{i} mismatch in order-sensitive mode!\n  Expected: {ex}\n  Got:      {act}")
                    return False
        else:
            # Unordered comparison: check each row exists in expected with exact multiset count
            matched_indices = set()
            for act in actual_rows:
                found = False
                for idx, ex in enumerate(exp_rows):
                    if idx not in matched_indices and rows_equal(act, ex):
                        matched_indices.add(idx)
                        found = True
                        break
                if not found:
                    print(f"[FAIL] {filename} ({q_id}): Actual row {act} not found in expected rows: {exp_rows}")
                    return False

    except Exception as ex:
        print(f"[FAIL] {filename} ({q_id}): SQL Execution error: {ex}")
        return False
    finally:
        conn.close()

    print(f"[PASS] {filename:<14} -> '{q['title']}' ({q['difficulty']})")
    return True


def main():
    if not os.path.exists(INDEX_PATH):
        print(f"[!] Error: {INDEX_PATH} not found.", file=sys.stderr)
        sys.exit(1)

    with open(INDEX_PATH, "r", encoding="utf-8") as f:
        index_list = json.load(f)

    print(f"=== Validating {len(index_list)} DoomSQL Questions ===")
    all_passed = True
    for item in index_list:
        file_path = os.path.join(QUESTIONS_DIR, item)
        if not os.path.exists(file_path):
            print(f"[FAIL] Missing file listed in index.json: {item}")
            all_passed = False
            continue
        if not validate_question_file(file_path):
            all_passed = False

    print("=" * 55)
    if all_passed:
        print(f"All {len(index_list)} questions PASSED validation perfectly! 🎉")
        sys.exit(0)
    else:
        print("Some questions failed validation. Please fix errors before shipping.", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
