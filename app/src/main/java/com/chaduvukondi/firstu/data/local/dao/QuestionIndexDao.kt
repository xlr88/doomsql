package com.chaduvukondi.firstu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chaduvukondi.firstu.data.local.entity.QuestionIndexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionIndexDao {

    @Query("SELECT * FROM question_index")
    fun getAllIndexFlow(): Flow<List<QuestionIndexEntity>>

    @Query("SELECT * FROM question_index")
    suspend fun getAllIndexSync(): List<QuestionIndexEntity>

    @Query("SELECT * FROM question_index WHERE id = :id LIMIT 1")
    suspend fun getIndexSync(id: String): QuestionIndexEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIndex(entity: QuestionIndexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllIndex(entities: List<QuestionIndexEntity>)

    @Query("SELECT COUNT(*) FROM question_index")
    suspend fun getCount(): Int

    @Query("DELETE FROM question_index")
    suspend fun clearIndex()
}
