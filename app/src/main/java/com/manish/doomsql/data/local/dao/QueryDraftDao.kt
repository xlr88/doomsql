package com.manish.doomsql.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manish.doomsql.data.local.entity.QueryDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryDraftDao {

    @Query("SELECT * FROM query_draft WHERE question_id = :questionId")
    fun getDraft(questionId: String): Flow<QueryDraftEntity?>

    @Query("SELECT * FROM query_draft WHERE question_id = :questionId")
    suspend fun getDraftSync(questionId: String): QueryDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(entity: QueryDraftEntity)

    @Query("DELETE FROM query_draft WHERE question_id = :questionId")
    suspend fun deleteDraft(questionId: String)

    @Query("DELETE FROM query_draft")
    suspend fun clearAll()
}
