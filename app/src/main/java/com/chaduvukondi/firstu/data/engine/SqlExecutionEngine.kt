package com.chaduvukondi.firstu.data.engine

import com.chaduvukondi.firstu.data.model.QueryExecutionResult
import com.chaduvukondi.firstu.data.model.Question

interface SqlExecutionEngine {
    suspend fun execute(question: Question, query: String): QueryExecutionResult
}
