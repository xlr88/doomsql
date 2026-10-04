package com.chaduvukondi.firstu.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chaduvukondi.firstu.data.local.dao.DailyActivityDao
import com.chaduvukondi.firstu.data.local.dao.QueryDraftDao
import com.chaduvukondi.firstu.data.local.dao.QuestionIndexDao
import com.chaduvukondi.firstu.data.local.dao.QuestionProgressDao
import com.chaduvukondi.firstu.data.local.entity.DailyActivityEntity
import com.chaduvukondi.firstu.data.local.entity.QueryDraftEntity
import com.chaduvukondi.firstu.data.local.entity.QuestionIndexEntity
import com.chaduvukondi.firstu.data.local.entity.QuestionProgressEntity

@Database(
    entities = [
        QuestionProgressEntity::class,
        QueryDraftEntity::class,
        DailyActivityEntity::class,
        QuestionIndexEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DoomSqlDatabase : RoomDatabase() {
    abstract fun questionProgressDao(): QuestionProgressDao
    abstract fun queryDraftDao(): QueryDraftDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun questionIndexDao(): QuestionIndexDao
}
