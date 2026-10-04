package com.chaduvukondi.firstu.data.model

data class QueryResult(
    val columns: List<String>,
    val rows: List<List<SqlValue>>,
    val truncated: Boolean,
    val executionTimeMs: Long
)
