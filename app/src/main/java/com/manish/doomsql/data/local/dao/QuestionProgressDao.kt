package com.manish.doomsql.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manish.doomsql.data.local.entity.QuestionProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionProgressDao {

    @Query("SELECT * FROM question_progress WHERE question_id = :questionId")
    fun getProgress(questionId: String): Flow<QuestionProgressEntity?>

    @Query("SELECT * FROM question_progress WHERE question_id = :questionId")
    suspend fun getProgressSync(questionId: String): QuestionProgressEntity?

    @Query("SELECT * FROM question_progress")
    fun getAllProgress(): Flow<List<QuestionProgressEntity>>

    @Query("SELECT COUNT(*) FROM question_progress WHERE is_solved = 1")
    suspend fun getSolvedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: QuestionProgressEntity)

    @Query("DELETE FROM question_progress")
    suspend fun clearAll()
}
