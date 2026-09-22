package com.manish.doomsql

import com.manish.doomsql.data.engine.SqlValidationResult
import com.manish.doomsql.data.engine.SqlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlValidatorTest {

    @Test
    fun validSelectWithCommentsAndTrailingSemicolon() {
        val query = """
            -- Fetch all users
            SELECT id, name /* internal id */ FROM User;
        """.trimIndent()
        val result = SqlValidator.validate(query)
        assertTrue(result is SqlValidationResult.Valid)
    }

    @Test
    fun validWithCteAndValues() {
        val withQuery = "WITH Ranked AS (SELECT id FROM Employee) SELECT * FROM Ranked;"
        val valuesQuery = "VALUES (1, 'Alice'), (2, 'Bob');"

        assertTrue(SqlValidator.validate(withQuery) is SqlValidationResult.Valid)
        assertTrue(SqlValidator.validate(valuesQuery) is SqlValidationResult.Valid)
    }

    @Test
    fun rejectMultipleStatements() {
        val query = "SELECT 1; SELECT 2;"
        val result = SqlValidator.validate(query)
        assertTrue(result is SqlValidationResult.Invalid)
        assertEquals("Multiple SQL statements are not permitted.", (result as SqlValidationResult.Invalid).reason)
    }

    @Test
    fun rejectNonSelectStatements() {
        val deleteQuery = "DELETE FROM Employee WHERE id = 1;"
        val dropQuery = "DROP TABLE Employee;"
        val updateQuery = "UPDATE Employee SET salary = 100;"

        assertTrue(SqlValidator.validate(deleteQuery) is SqlValidationResult.Invalid)
        assertTrue(SqlValidator.validate(dropQuery) is SqlValidationResult.Invalid)
        assertTrue(SqlValidator.validate(updateQuery) is SqlValidationResult.Invalid)
    }

    @Test
    fun rejectForbiddenKeywords() {
        val pragmaQuery = "PRAGMA table_info(Employee);"
        val attachQuery = "ATTACH DATABASE 'test.db' AS test;"
        val vacuumQuery = "VACUUM;"

        assertTrue(SqlValidator.validate(pragmaQuery) is SqlValidationResult.Invalid)
        assertTrue(SqlValidator.validate(attachQuery) is SqlValidationResult.Invalid)
        assertTrue(SqlValidator.validate(vacuumQuery) is SqlValidationResult.Invalid)
    }

    @Test
    fun allowForbiddenKeywordsInsideStringLiterals() {
        // 'PRAGMA' or 'ATTACH' inside quotes must not be flagged
        val query = "SELECT 'PRAGMA table_info' AS note FROM Employee;"
        val result = SqlValidator.validate(query)
        assertTrue(result is SqlValidationResult.Valid)
    }
}
