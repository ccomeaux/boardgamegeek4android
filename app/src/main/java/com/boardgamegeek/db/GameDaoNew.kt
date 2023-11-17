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

    @Query("UPDATE games SET custom_player_sort = :isCustom WHERE game_id = :gameId")
    suspend fun updateCustomPlayerSort(gameId: Int, isCustom: Boolean): Int

    @Query("UPDATE games SET hero_image_url = :heroUrl WHERE game_id = :gameId")
    suspend fun updateHeroUrl(gameId: Int, heroUrl: String): Int

    @Query("UPDATE games SET icon_color = :iconColor, dark_color = :darkColor, wins_color = :winsColor, winnable_plays_color = :winnablePlaysColor, all_plays_color = :allPlaysColor WHERE game_id = :gameId")
    suspend fun updateImageColors(gameId: Int, iconColor: Int, darkColor: Int, winsColor: Int, winnablePlaysColor: Int, allPlaysColor: Int)

    @Query("UPDATE games SET last_viewed = :lastViewed WHERE game_id = :gameId")
    suspend fun updateLastViewed(gameId: Int, lastViewed: Long): Int

    @Query("UPDATE games SET num_of_plays = :playCount WHERE game_id = :gameId")
    suspend fun updatePlayCount(gameId: Int, playCount: Int): Int

    @Query("UPDATE games SET updated_plays = :timestamp WHERE game_id = :gameId")
    suspend fun updatePlayTimestamp(gameId: Int, timestamp: Long): Int

    @Query("UPDATE games SET starred = :isStarred WHERE game_id = :gameId")
    suspend fun updateStarred(gameId: Int, isStarred: Boolean): Int

    @Query("UPDATE games SET updated_plays = 0")
    suspend fun resetPlaySync()

    @Query("DELETE FROM games")
    suspend fun deleteAll(): Int

    @Query("DELETE FROM games WHERE game_id = :gameId")
    suspend fun delete(gameId: Int): Int

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
