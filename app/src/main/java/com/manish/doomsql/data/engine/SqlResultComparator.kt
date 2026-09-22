package com.manish.doomsql.data.engine

import com.manish.doomsql.data.model.QueryResult
import com.manish.doomsql.data.model.SqlValue
import kotlin.math.abs
import kotlin.math.max

data class ComparisonResult(
    val isEqual: Boolean,
    val reason: String? = null
)

object SqlResultComparator {

    fun compare(
        actual: QueryResult,
        expected: QueryResult,
        orderSensitive: Boolean
    ): ComparisonResult {
        // 1. Column count must match; column NAMES are ignored.
        if (actual.columns.size != expected.columns.size) {
            return ComparisonResult(
                isEqual = false,
                reason = "Column count mismatch: got ${actual.columns.size} columns, expected ${expected.columns.size} columns."
            )
        }

        // Two empty results with the same column count are equal.
        if (actual.rows.isEmpty() && expected.rows.isEmpty()) {
            return ComparisonResult(isEqual = true)
        }

        // 2. Row count must match. Duplicates matter: [A, A, B] != [A, B].
        if (actual.rows.size != expected.rows.size) {
            return ComparisonResult(
                isEqual = false,
                reason = "Row count mismatch: got ${actual.rows.size} rows, expected ${expected.rows.size} rows."
            )
        }

        // 3. Order-sensitive vs Order-insensitive comparison
        if (orderSensitive) {
            for (i in actual.rows.indices) {
                if (!rowsEqual(actual.rows[i], expected.rows[i])) {
                    return ComparisonResult(
                        isEqual = false,
                        reason = "Row order mismatch at index $i: got ${actual.rows[i]}, expected ${expected.rows[i]}."
                    )
                }
            }
            return ComparisonResult(isEqual = true)
        } else {
            // Multiset comparison where order is ignored, but duplicates matter
            val matched = BooleanArray(expected.rows.size)
            for (actualRow in actual.rows) {
                var foundMatch = false
                for (j in expected.rows.indices) {
                    if (!matched[j] && rowsEqual(actualRow, expected.rows[j])) {
                        matched[j] = true
                        foundMatch = true
                        break
                    }
                }
                if (!foundMatch) {
                    return ComparisonResult(
                        isEqual = false,
                        reason = "Row $actualRow not found in expected results or duplicate count mismatch."
                    )
                }
            }
            return ComparisonResult(isEqual = true)
        }
    }

    fun rowsEqual(r1: List<SqlValue>, r2: List<SqlValue>): Boolean {
        if (r1.size != r2.size) return false
        for (i in r1.indices) {
            if (!valuesEqual(r1[i], r2[i])) return false
        }
        return true
    }

    fun valuesEqual(v1: SqlValue, v2: SqlValue): Boolean {
        return when {
            // NULL equals only NULL. NULL != 0 != "".
            v1 is SqlValue.Null && v2 is SqlValue.Null -> true
            v1 is SqlValue.Null || v2 is SqlValue.Null -> false

            // Numbers compared numerically: 10 == 10.0. Reals compared with relative tolerance 1e-6.
            (v1 is SqlValue.Integer || v1 is SqlValue.Real) &&
            (v2 is SqlValue.Integer || v2 is SqlValue.Real) -> {
                val d1 = when (v1) {
                    is SqlValue.Integer -> v1.value.toDouble()
                    is SqlValue.Real -> v1.value
                    else -> 0.0
                }
                val d2 = when (v2) {
                    is SqlValue.Integer -> v2.value.toDouble()
                    is SqlValue.Real -> v2.value
                    else -> 0.0
                }
                if (d1 == d2) {
                    true
                } else {
                    val diff = abs(d1 - d2)
                    val denom = max(abs(d1), abs(d2))
                    if (denom == 0.0) diff <= 1e-6 else (diff / denom) <= 1e-6
                }
            }

            // Text is case-sensitive exact match.
            v1 is SqlValue.Text && v2 is SqlValue.Text -> v1.value == v2.value

            else -> false
        }
    }
}
