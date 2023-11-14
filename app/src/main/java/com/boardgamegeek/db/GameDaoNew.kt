package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.boardgamegeek.db.model.*

@Dao
interface GameDaoNew {
    @Transaction
    @Query("SELECT * FROM games WHERE game_id = :gameId")
    suspend fun loadDesignersForGame(gameId: Int): GameWithDesigners?

    @Transaction
    @Query("SELECT * FROM games WHERE game_id = :gameId")
    suspend fun loadArtistsForGame(gameId: Int): GameWithArtists?

    @Transaction
    @Query("SELECT * FROM games WHERE game_id = :gameId")
    suspend fun loadPublishersForGame(gameId: Int): GameWithPublishers?

    @Transaction
    @Query("SELECT * FROM games WHERE game_id = :gameId")
    suspend fun loadCategoriesForGame(gameId: Int): GameWithCategories?

    @Transaction
    @Query("SELECT * FROM games WHERE game_id = :gameId")
    suspend fun loadMechanicsForGame(gameId: Int): GameWithMechanics?
}