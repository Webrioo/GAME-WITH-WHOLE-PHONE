package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserStats(stats: UserStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRunRecord(record: RunRecordEntity)

    @Query("SELECT * FROM run_records ORDER BY score DESC LIMIT 20")
    fun getTopRuns(): Flow<List<RunRecordEntity>>

    @Query("SELECT * FROM run_records ORDER BY timestamp DESC LIMIT 20")
    fun getRecentRuns(): Flow<List<RunRecordEntity>>
}
