package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY updatedAtMillis DESC")
    fun observeAllGames(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM games WHERE isPaused = 1 ORDER BY updatedAtMillis DESC")
    fun observePausedGames(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM games WHERE isPaused = 0 ORDER BY updatedAtMillis DESC")
    fun observeCompletedGames(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Long): GameRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGame(game: GameRecordEntity): Long

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)
}
