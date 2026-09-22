package com.manish.doomsql.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

@Serializable
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

@Serializable
data class ColumnSchema(
    val name: String,
    val type: String,
    val nullable: Boolean = true,
    val primaryKey: Boolean = false
)

@Serializable
data class TableSchema(
    val name: String,
    val columns: List<ColumnSchema>,
    val rows: List<List<JsonElement>>
)

@Serializable
data class ExpectedOutput(
    val columns: List<String>,
    val rows: List<List<JsonElement>>
) {
    fun toQueryResult(): QueryResult {
        val parsedRows = rows.map { row ->
            row.map { it.toSqlValue() }
        }
        return QueryResult(
            columns = columns,
            rows = parsedRows,
            truncated = false,
            executionTimeMs = 0L
        )
    }
}

@Serializable
data class Question(
    val id: String,
    val contentVersion: Int = 1,
    val title: String,
    val difficulty: Difficulty,
    val sqlDialect: String = "SQLITE",
    val tags: List<String> = emptyList(),
    val description: String,
    val orderSensitive: Boolean = false,
    val tables: List<TableSchema>,
    val expectedOutput: ExpectedOutput,
    val solutionQuery: String,
    val explanation: String = ""
)

fun JsonElement.toSqlValue(): SqlValue {
    if (this is JsonNull) return SqlValue.Null
    if (this is JsonPrimitive) {
        if (this.isString) return SqlValue.Text(this.content)
        val longVal = this.longOrNull
        if (longVal != null) return SqlValue.Integer(longVal)
        val doubleVal = this.doubleOrNull
        if (doubleVal != null) return SqlValue.Real(doubleVal)
        val boolVal = this.booleanOrNull
        if (boolVal != null) return SqlValue.Integer(if (boolVal) 1L else 0L)
        return SqlValue.Text(this.content)
    }
    return SqlValue.Text(this.toString())
}
