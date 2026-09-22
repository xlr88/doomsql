package com.manish.doomsql.data.engine

import com.manish.doomsql.data.model.QueryExecutionResult
import com.manish.doomsql.data.model.Question

interface SqlExecutionEngine {
    suspend fun execute(question: Question, query: String): QueryExecutionResult
}
