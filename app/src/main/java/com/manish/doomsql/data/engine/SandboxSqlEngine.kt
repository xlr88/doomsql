package com.manish.doomsql.data.engine

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.manish.doomsql.data.model.ColumnSchema
import com.manish.doomsql.data.model.QueryExecutionResult
import com.manish.doomsql.data.model.QueryResult
import com.manish.doomsql.data.model.Question
import com.manish.doomsql.data.model.SqlValue
import com.manish.doomsql.data.model.toSqlValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class SandboxSqlEngine : SqlExecutionEngine {

    override suspend fun execute(question: Question, query: String): QueryExecutionResult {
        // 1. Validate query before execution
        val validation = SqlValidator.validate(query)
        if (validation is SqlValidationResult.Invalid) {
            return QueryExecutionResult.Error(validation.reason)
        }
        val sanitizedQuery = (validation as SqlValidationResult.Valid).sanitizedQuery

        // 2. Run SQL on Dispatchers.IO with 3 second timeout
        return try {
            withTimeout(3000L) {
                withContext(Dispatchers.IO) {
                    runQueryInSandbox(question, sanitizedQuery)
                }
            }
        } catch (e: TimeoutCancellationException) {
            QueryExecutionResult.Error("Query took too long (timed out after 3 seconds).")
        } catch (e: Throwable) {
            QueryExecutionResult.Error(e.message ?: "An unexpected database error occurred.")
        }
    }

    private fun runQueryInSandbox(question: Question, sanitizedQuery: String): QueryExecutionResult {
        return try {
            // Try bundled SQLite driver first
            runWithBundledDriver(question, sanitizedQuery)
        } catch (e: LinkageError) {
            // Fall back to Android built-in SQLite database if native bundled library isn't loaded
            runWithAndroidDatabase(question, sanitizedQuery)
        } catch (e: Exception) {
            // Check if it's an initialization error or SQL syntax error
            if (e.message?.contains("BundledSQLiteDriver") == true || e.cause is LinkageError) {
                runWithAndroidDatabase(question, sanitizedQuery)
            } else {
                QueryExecutionResult.Error(e.message ?: "SQL execution error.")
            }
        }
    }

    private fun runWithBundledDriver(question: Question, sanitizedQuery: String): QueryExecutionResult {
        val driver = BundledSQLiteDriver()
        var connection: SQLiteConnection? = null
        val startTime = System.currentTimeMillis()
        try {
            connection = driver.open(":memory:")

            // 1. Create tables and insert rows using prepared statements
            for (table in question.tables) {
                val colDefs = table.columns.joinToString(", ") { col ->
                    buildColumnDef(col)
                }
                val createSql = "CREATE TABLE ${table.name} ($colDefs);"
                connection.prepare(createSql).use { it.step() }

                if (table.rows.isNotEmpty()) {
                    val placeholders = table.columns.joinToString(", ") { "?" }
                    val insertSql = "INSERT INTO ${table.name} VALUES ($placeholders);"
                    for (row in table.rows) {
                        connection.prepare(insertSql).use { stmt ->
                            for ((idx, cell) in row.withIndex()) {
                                val bindIndex = idx + 1
                                val sqlVal = cell.toSqlValue()
                                when (sqlVal) {
                                    is SqlValue.Null -> stmt.bindNull(bindIndex)
                                    is SqlValue.Integer -> stmt.bindLong(bindIndex, sqlVal.value)
                                    is SqlValue.Real -> stmt.bindDouble(bindIndex, sqlVal.value)
                                    is SqlValue.Text -> stmt.bindText(bindIndex, sqlVal.value)
                                }
                            }
                            stmt.step()
                        }
                    }
                }
            }

            // 2. Set PRAGMA query_only = ON
            connection.prepare("PRAGMA query_only = ON;").use { it.step() }

            // 3. Execute query and fetch up to 1000 rows
            connection.prepare(sanitizedQuery).use { stmt ->
                val colCount = stmt.getColumnCount()
                val columns = ArrayList<String>(colCount)
                for (i in 0 until colCount) {
                    columns.add(stmt.getColumnName(i))
                }

                val rows = ArrayList<List<SqlValue>>()
                var truncated = false

                while (stmt.step()) {
                    if (rows.size >= 1000) {
                        truncated = true
                        break
                    }
                    val row = ArrayList<SqlValue>(colCount)
                    for (i in 0 until colCount) {
                        if (stmt.isNull(i)) {
                            row.add(SqlValue.Null)
                        } else {
                            val textVal = stmt.getText(i)
                            val longVal = textVal.toLongOrNull()
                            val doubleVal = textVal.toDoubleOrNull()
                            val parsedVal = when {
                                longVal != null && !textVal.contains(".") -> SqlValue.Integer(longVal)
                                doubleVal != null && (textVal.contains(".") || textVal.contains("e", ignoreCase = true)) ->
                                    SqlValue.Real(doubleVal)
                                else -> SqlValue.Text(textVal)
                            }
                            row.add(parsedVal)
                        }
                    }
                    rows.add(row)
                }

                val executionTimeMs = System.currentTimeMillis() - startTime
                return QueryExecutionResult.Success(
                    QueryResult(
                        columns = columns,
                        rows = rows,
                        truncated = truncated,
                        executionTimeMs = executionTimeMs
                    )
                )
            }
        } finally {
            connection?.close()
        }
    }

    private fun runWithAndroidDatabase(question: Question, sanitizedQuery: String): QueryExecutionResult {
        var db: SQLiteDatabase? = null
        val startTime = System.currentTimeMillis()
        try {
            // Open in-memory database
            db = SQLiteDatabase.create(null)

            for (table in question.tables) {
                val colDefs = table.columns.joinToString(", ") { col ->
                    buildColumnDef(col)
                }
                val createSql = "CREATE TABLE ${table.name} ($colDefs);"
                db.execSQL(createSql)

                if (table.rows.isNotEmpty()) {
                    val placeholders = table.columns.joinToString(", ") { "?" }
                    val insertSql = "INSERT INTO ${table.name} VALUES ($placeholders);"
                    val stmt = db.compileStatement(insertSql)
                    try {
                        for (row in table.rows) {
                            stmt.clearBindings()
                            for ((idx, cell) in row.withIndex()) {
                                val bindIndex = idx + 1
                                val sqlVal = cell.toSqlValue()
                                when (sqlVal) {
                                    is SqlValue.Null -> stmt.bindNull(bindIndex)
                                    is SqlValue.Integer -> stmt.bindLong(bindIndex, sqlVal.value)
                                    is SqlValue.Real -> stmt.bindDouble(bindIndex, sqlVal.value)
                                    is SqlValue.Text -> stmt.bindString(bindIndex, sqlVal.value)
                                }
                            }
                            stmt.executeInsert()
                        }
                    } finally {
                        stmt.close()
                    }
                }
            }

            // Set PRAGMA query_only = ON
            db.execSQL("PRAGMA query_only = ON;")

            // Execute query
            db.rawQuery(sanitizedQuery, null).use { cursor ->
                val colCount = cursor.columnCount
                val columns = cursor.columnNames.toList()
                val rows = ArrayList<List<SqlValue>>()
                var truncated = false

                while (cursor.moveToNext()) {
                    if (rows.size >= 1000) {
                        truncated = true
                        break
                    }
                    val row = ArrayList<SqlValue>(colCount)
                    for (i in 0 until colCount) {
                        when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> row.add(SqlValue.Null)
                            Cursor.FIELD_TYPE_INTEGER -> row.add(SqlValue.Integer(cursor.getLong(i)))
                            Cursor.FIELD_TYPE_FLOAT -> row.add(SqlValue.Real(cursor.getDouble(i)))
                            Cursor.FIELD_TYPE_STRING -> row.add(SqlValue.Text(cursor.getString(i)))
                            Cursor.FIELD_TYPE_BLOB -> row.add(SqlValue.Text(cursor.getBlob(i).decodeToString()))
                            else -> row.add(SqlValue.Null)
                        }
                    }
                    rows.add(row)
                }

                val executionTimeMs = System.currentTimeMillis() - startTime
                return QueryExecutionResult.Success(
                    QueryResult(
                        columns = columns,
                        rows = rows,
                        truncated = truncated,
                        executionTimeMs = executionTimeMs
                    )
                )
            }
        } finally {
            db?.close()
        }
    }

    private fun buildColumnDef(col: ColumnSchema): String = buildString {
        append(col.name)
        append(" ")
        append(col.type)
        if (col.primaryKey) append(" PRIMARY KEY")
        if (!col.nullable) append(" NOT NULL")
    }
}
