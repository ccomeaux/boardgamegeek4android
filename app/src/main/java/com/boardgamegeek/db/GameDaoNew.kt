package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.boardgamegeek.db.model.*

@Dao
interface GameDaoNew {
    @Query("SELECT game_id, game_name FROM games WHERE (updated != 0 OR updated IS NOT NULL) ORDER BY updated LIMIT :gamesPerFetch")
    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int): List<GameIdAndName>

    @Query("SELECT game_id, game_name FROM games WHERE (updated = 0 OR updated IS NULL) ORDER BY updated_list LIMIT :gamesPerFetch")
    suspend fun loadUnupdatedGames(gamesPerFetch: Int): List<GameIdAndName>

    @Query("SELECT games.game_id, game_name FROM games LEFT OUTER JOIN collection ON games.game_id = collection.game_id WHERE collection_id IS NULL AND last_viewed < :sinceTimestamp ORDER BY games.updated")
    suspend fun loadNonCollectionGames(sinceTimestamp: Long): List<GameIdAndName>

    @Query("SELECT games.game_id, game_name FROM games LEFT OUTER JOIN collection ON games.game_id = collection.game_id WHERE collection_id IS NULL AND last_viewed < :sinceTimestamp AND num_of_plays = 0 ORDER BY games.updated")
    suspend fun loadNonCollectionAndUnplayedGames(sinceTimestamp: Long): List<GameIdAndName>

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