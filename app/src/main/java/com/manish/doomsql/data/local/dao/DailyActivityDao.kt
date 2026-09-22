package com.manish.doomsql.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manish.doomsql.data.local.entity.DailyActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivityDao {

    @Query("SELECT * FROM daily_activity WHERE date = :date")
    fun getActivity(date: String): Flow<DailyActivityEntity?>

    @Query("SELECT * FROM daily_activity WHERE date = :date")
    suspend fun getActivitySync(date: String): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    fun getAllActivities(): Flow<List<DailyActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(entity: DailyActivityEntity)

    @Query("DELETE FROM daily_activity")
    suspend fun clearAll()
}
