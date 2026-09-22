package com.manish.doomsql.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_progress")
data class QuestionProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "is_solved")
    val isSolved: Boolean = false,
    @ColumnInfo(name = "first_solved_at")
    val firstSolvedAt: Long? = null,
    @ColumnInfo(name = "last_attempted_at")
    val lastAttemptedAt: Long? = null,
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,
    @ColumnInfo(name = "solution_viewed")
    val solutionViewed: Boolean = false
)
