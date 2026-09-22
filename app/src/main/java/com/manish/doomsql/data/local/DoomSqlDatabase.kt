package com.manish.doomsql.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.manish.doomsql.data.local.dao.DailyActivityDao
import com.manish.doomsql.data.local.dao.QueryDraftDao
import com.manish.doomsql.data.local.dao.QuestionProgressDao
import com.manish.doomsql.data.local.entity.DailyActivityEntity
import com.manish.doomsql.data.local.entity.QueryDraftEntity
import com.manish.doomsql.data.local.entity.QuestionProgressEntity

@Database(
    entities = [
        QuestionProgressEntity::class,
        QueryDraftEntity::class,
        DailyActivityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DoomSqlDatabase : RoomDatabase() {
    abstract fun questionProgressDao(): QuestionProgressDao
    abstract fun queryDraftDao(): QueryDraftDao
    abstract fun dailyActivityDao(): DailyActivityDao
}
