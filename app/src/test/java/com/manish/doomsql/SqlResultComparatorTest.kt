package com.manish.doomsql

import com.manish.doomsql.data.engine.SqlResultComparator
import com.manish.doomsql.data.model.QueryResult
import com.manish.doomsql.data.model.SqlValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlResultComparatorTest {

    // 1. Column count must match; column NAMES are ignored.
    @Test
    fun columnCountMustMatchAndNamesIgnored() {
        val resultA = QueryResult(
            columns = listOf("name", "age"),
            rows = listOf(listOf(SqlValue.Text("Alice"), SqlValue.Integer(30))),
            truncated = false,
            executionTimeMs = 1
        )
        val resultB = QueryResult(
            columns = listOf("col1", "col2"), // Different column names
            rows = listOf(listOf(SqlValue.Text("Alice"), SqlValue.Integer(30))),
            truncated = false,
            executionTimeMs = 1
        )
        val resultC = QueryResult(
            columns = listOf("name"), // Only 1 column
            rows = listOf(listOf(SqlValue.Text("Alice"))),
            truncated = false,
            executionTimeMs = 1
        )

        assertTrue(SqlResultComparator.compare(resultA, resultB, orderSensitive = false).isEqual)
        assertFalse(SqlResultComparator.compare(resultA, resultC, orderSensitive = false).isEqual)
    }

    // 2. Two empty results with the same column count are equal.
    @Test
    fun emptyResultsWithSameColumnCountAreEqual() {
        val resultA = QueryResult(
            columns = listOf("a", "b"),
            rows = emptyList(),
            truncated = false,
            executionTimeMs = 1
        )
        val resultB = QueryResult(
            columns = listOf("col_x", "col_y"),
            rows = emptyList(),
            truncated = false,
            executionTimeMs = 1
        )
        assertTrue(SqlResultComparator.compare(resultA, resultB, orderSensitive = false).isEqual)
        assertTrue(SqlResultComparator.compare(resultA, resultB, orderSensitive = true).isEqual)
    }

    // 3. Order sensitivity flag
    @Test
    fun orderSensitivityRespected() {
        val resultA = QueryResult(
            columns = listOf("val"),
            rows = listOf(listOf(SqlValue.Integer(1)), listOf(SqlValue.Integer(2))),
            truncated = false,
            executionTimeMs = 1
        )
        val resultB = QueryResult(
            columns = listOf("val"),
            rows = listOf(listOf(SqlValue.Integer(2)), listOf(SqlValue.Integer(1))),
            truncated = false,
            executionTimeMs = 1
        )

        // Order ignored when orderSensitive = false
        assertTrue(SqlResultComparator.compare(resultA, resultB, orderSensitive = false).isEqual)
        // Order strictly checked when orderSensitive = true
        assertFalse(SqlResultComparator.compare(resultA, resultB, orderSensitive = true).isEqual)
    }

    // 4. Duplicates matter: [A, A, B] != [A, B]
    @Test
    fun duplicatesMatter() {
        val withDuplicates = QueryResult(
            columns = listOf("val"),
            rows = listOf(
                listOf(SqlValue.Text("A")),
                listOf(SqlValue.Text("A")),
                listOf(SqlValue.Text("B"))
            ),
            truncated = false,
            executionTimeMs = 1
        )
        val withoutDuplicates = QueryResult(
            columns = listOf("val"),
            rows = listOf(
                listOf(SqlValue.Text("A")),
                listOf(SqlValue.Text("B"))
            ),
            truncated = false,
            executionTimeMs = 1
        )

        assertFalse(SqlResultComparator.compare(withDuplicates, withoutDuplicates, orderSensitive = false).isEqual)
    }

    // 5. NULL equals only NULL. NULL != 0 != ""
    @Test
    fun nullEqualityRules() {
        val nullResult = QueryResult(
            columns = listOf("col"),
            rows = listOf(listOf(SqlValue.Null)),
            truncated = false,
            executionTimeMs = 1
        )
        val zeroResult = QueryResult(
            columns = listOf("col"),
            rows = listOf(listOf(SqlValue.Integer(0))),
            truncated = false,
            executionTimeMs = 1
        )
        val emptyStringResult = QueryResult(
            columns = listOf("col"),
            rows = listOf(listOf(SqlValue.Text(""))),
            truncated = false,
            executionTimeMs = 1
        )
        val anotherNullResult = QueryResult(
            columns = listOf("different_name"),
            rows = listOf(listOf(SqlValue.Null)),
            truncated = false,
            executionTimeMs = 1
        )

        // NULL equals NULL
        assertTrue(SqlResultComparator.compare(nullResult, anotherNullResult, orderSensitive = false).isEqual)
        // NULL != 0
        assertFalse(SqlResultComparator.compare(nullResult, zeroResult, orderSensitive = false).isEqual)
        // NULL != ""
        assertFalse(SqlResultComparator.compare(nullResult, emptyStringResult, orderSensitive = false).isEqual)
        // 0 != ""
        assertFalse(SqlResultComparator.compare(zeroResult, emptyStringResult, orderSensitive = false).isEqual)
    }

    // 6. Numbers compared numerically: 10 == 10.0. Reals compared with relative tolerance 1e-6
    @Test
    fun numericComparisonAndTolerance() {
        val intResult = QueryResult(
            columns = listOf("num"),
            rows = listOf(listOf(SqlValue.Integer(10))),
            truncated = false,
            executionTimeMs = 1
        )
        val floatResult = QueryResult(
            columns = listOf("num"),
            rows = listOf(listOf(SqlValue.Real(10.0))),
            truncated = false,
            executionTimeMs = 1
        )
        val closeFloatResult = QueryResult(
            columns = listOf("num"),
            rows = listOf(listOf(SqlValue.Real(10.000001))),
            truncated = false,
            executionTimeMs = 1
        )
        val distantFloatResult = QueryResult(
            columns = listOf("num"),
            rows = listOf(listOf(SqlValue.Real(10.01))),
            truncated = false,
            executionTimeMs = 1
        )

        // 10 == 10.0
        assertTrue(SqlResultComparator.compare(intResult, floatResult, orderSensitive = false).isEqual)
        // Within 1e-6 tolerance
        assertTrue(SqlResultComparator.compare(intResult, closeFloatResult, orderSensitive = false).isEqual)
        // Outside 1e-6 tolerance
        assertFalse(SqlResultComparator.compare(intResult, distantFloatResult, orderSensitive = false).isEqual)
    }

    // 7. Text is case-sensitive exact match
    @Test
    fun textCaseSensitivity() {
        val lower = QueryResult(
            columns = listOf("name"),
            rows = listOf(listOf(SqlValue.Text("alice"))),
            truncated = false,
            executionTimeMs = 1
        )
        val capitalized = QueryResult(
            columns = listOf("name"),
            rows = listOf(listOf(SqlValue.Text("Alice"))),
            truncated = false,
            executionTimeMs = 1
        )

        assertFalse(SqlResultComparator.compare(lower, capitalized, orderSensitive = false).isEqual)
    }
}
