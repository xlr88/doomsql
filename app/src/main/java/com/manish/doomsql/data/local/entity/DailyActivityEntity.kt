package com.manish.doomsql.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String, // "YYYY-MM-DD" local date
    @ColumnInfo(name = "queries_run")
    val queriesRun: Int = 0,
    @ColumnInfo(name = "questions_solved")
    val questionsSolved: Int = 0
)
