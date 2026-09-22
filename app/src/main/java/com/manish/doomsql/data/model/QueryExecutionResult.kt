package com.manish.doomsql.data.model

sealed interface QueryExecutionResult {
    data class Success(val result: QueryResult) : QueryExecutionResult
    data class Error(val message: String) : QueryExecutionResult
}
