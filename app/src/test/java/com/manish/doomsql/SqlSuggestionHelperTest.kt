package com.manish.doomsql

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.manish.doomsql.data.model.ColumnSchema
import com.manish.doomsql.data.model.Difficulty
import com.manish.doomsql.data.model.ExpectedOutput
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.data.model.TableSchema
import com.manish.doomsql.ui.components.SqlSuggestionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlSuggestionHelperTest {

    private val sampleQuestion = Question(
        id = "test_q",
        title = "Test Question",
        description = "Test description",
        difficulty = Difficulty.EASY,
        tables = listOf(
            TableSchema(
                name = "employee",
                columns = listOf(
                    ColumnSchema("id", "INTEGER"),
                    ColumnSchema("name", "TEXT"),
                    ColumnSchema("salary", "INTEGER"),
                    ColumnSchema("department_id", "INTEGER")
                ),
                rows = emptyList()
            )
        ),
        solutionQuery = "SELECT name FROM employee",
        expectedOutput = ExpectedOutput(
            columns = listOf("name"),
            rows = emptyList()
        )
    )

    @Test
    fun extractPrefixTest() {
        val (prefix1, range1) = SqlSuggestionHelper.extractPrefix("SELECT N", 8)
        assertEquals("N", prefix1)
        assertEquals(7 until 8, range1)

        val (prefix2, range2) = SqlSuggestionHelper.extractPrefix("SELECT employee.na", 18)
        assertEquals("employee.na", prefix2)
        assertEquals(7 until 18, range2)

        val (prefixEmpty, rangeEmpty) = SqlSuggestionHelper.extractPrefix("SELECT ", 7)
        assertEquals("", prefixEmpty)
        assertEquals(IntRange.EMPTY, rangeEmpty)
    }

    @Test
    fun suggestionsForPrefixN() {
        // User's specific scenario: typing 'N' with employee table (id, name, salary, department_id)
        val suggestions = SqlSuggestionHelper.buildSuggestions(sampleQuestion, "N")
        val displayTexts = suggestions.map { it.displayText }

        // Must include 'name'
        assertTrue("Must suggest column name", displayTexts.contains("name"))
        // Must include qualified 'employee.name'
        assertTrue("Must suggest employee.name", displayTexts.contains("employee.name"))
        // Must include SQL keywords NULL, NOT, NOTNULL, NULLIF
        assertTrue("Must suggest NULL", displayTexts.contains("NULL"))
        assertTrue("Must suggest NOT", displayTexts.contains("NOT"))
        assertTrue("Must suggest NOTNULL", displayTexts.contains("NOTNULL"))
        assertTrue("Must suggest NULLIF", displayTexts.contains("NULLIF"))

        // Priority check: 'name' should be listed before 'NULL' or keywords
        val nameIndex = displayTexts.indexOf("name")
        val nullIndex = displayTexts.indexOf("NULL")
        assertTrue("Column name should precede keyword NULL", nameIndex < nullIndex)
    }

    @Test
    fun applySuggestionReplacesPrefix() {
        val initial = TextFieldValue(
            text = "SELECT N",
            selection = TextRange(8)
        )
        val suggestion = SqlSuggestionHelper.buildSuggestions(sampleQuestion, "N").first { it.displayText == "name" }
        val applied = SqlSuggestionHelper.applySuggestion(initial, suggestion)

        assertEquals("SELECT name ", applied.text)
        assertEquals(12, applied.selection.end)
    }

    @Test
    fun insertTabInsertsTwoSpaces() {
        val initial = TextFieldValue(
            text = "SELECT\n",
            selection = TextRange(7)
        )
        val tabApplied = SqlSuggestionHelper.insertTab(initial)
        assertEquals("SELECT\n  ", tabApplied.text)
        assertEquals(9, tabApplied.selection.end)
    }
}
