package com.manish.doomsql.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "query_draft")
data class QueryDraftEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "query_text")
    val queryText: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
