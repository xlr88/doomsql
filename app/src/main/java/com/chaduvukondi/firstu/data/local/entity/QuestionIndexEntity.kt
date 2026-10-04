package com.chaduvukondi.firstu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_index")
data class QuestionIndexEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val tags: String,
    val contentVersion: Int,
    val addedAt: String?,
    val sourceLayer: String, // "INTERNAL_STORAGE" or "ASSETS"
    val file: String
)
