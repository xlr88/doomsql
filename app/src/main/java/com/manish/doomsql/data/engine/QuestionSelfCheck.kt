package com.manish.doomsql.data.engine

import android.util.Log
import com.manish.doomsql.data.model.QueryExecutionResult
import com.manish.doomsql.data.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QuestionSelfCheck {
    private const val TAG = "DoomSQL_SelfCheck"

    suspend fun runCheck(questions: List<Question>, engine: SqlExecutionEngine) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting self-check on ${questions.size} questions...")
        var failedCount = 0

        for (question in questions) {
            val result = engine.execute(question, question.solutionQuery)
            when (result) {
                is QueryExecutionResult.Error -> {
                    failedCount++
                    Log.e(
                        TAG,
                        "FAILED [${question.id}] \"${question.title}\" - Solution execution error: ${result.message}"
                    )
                }
                is QueryExecutionResult.Success -> {
                    val expectedResult = question.expectedOutput.toQueryResult()
                    val comparison = SqlResultComparator.compare(
                        actual = result.result,
                        expected = expectedResult,
                        orderSensitive = question.orderSensitive
                    )
                    if (!comparison.isEqual) {
                        failedCount++
                        Log.e(
                            TAG,
                            "MISMATCH [${question.id}] \"${question.title}\": ${comparison.reason}. " +
                                "Expected: cols=${expectedResult.columns} rows=${expectedResult.rows}, " +
                                "Actual: cols=${result.result.columns} rows=${result.result.rows}"
                        )
                    } else {
                        Log.d(TAG, "PASSED [${question.id}] \"${question.title}\"")
                    }
                }
            }
        }

        if (failedCount == 0) {
            Log.i(TAG, "All ${questions.size} questions PASSED self-check successfully!")
        } else {
            Log.e(TAG, "$failedCount / ${questions.size} questions FAILED self-check!")
        }
    }
}
