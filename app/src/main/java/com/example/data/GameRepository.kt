package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: GameDao) {
    val allGames: Flow<List<GameRecordEntity>> = dao.observeAllGames()
    val pausedGames: Flow<List<GameRecordEntity>> = dao.observePausedGames()
    val completedGames: Flow<List<GameRecordEntity>> = dao.observeCompletedGames()

    suspend fun getById(id: Long): GameRecordEntity? = dao.getGameById(id)

    suspend fun saveGame(game: GameRecordEntity): Long = dao.upsertGame(game)

    suspend fun deleteById(id: Long) = dao.deleteGameById(id)
}
