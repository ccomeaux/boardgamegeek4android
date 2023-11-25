package com.boardgamegeek.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.boardgamegeek.db.model.*

@Dao
interface GameDaoNew {
    @Query("SELECT games.*, MAX(plays.date) AS lastPlayedDate FROM games JOIN plays ON games.game_id = plays.object_id WHERE game_id = :gameId")
    suspend fun loadGame(gameId: Int): GameWithLastPlayed?

    @Query("SELECT game_id, game_name FROM games WHERE (updated != 0 OR updated IS NOT NULL) ORDER BY updated LIMIT :gamesPerFetch")
    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int): List<GameIdAndName>

    @Query("SELECT game_id, game_name FROM games WHERE (updated = 0 OR updated IS NULL) ORDER BY updated_list LIMIT :gamesPerFetch")
    suspend fun loadUnupdatedGames(gamesPerFetch: Int): List<GameIdAndName>

    @Query("SELECT games.game_id, game_name FROM games LEFT OUTER JOIN collection ON games.game_id = collection.game_id WHERE collection_id IS NULL AND last_viewed < :sinceTimestamp ORDER BY games.updated")
    suspend fun loadNonCollectionGames(sinceTimestamp: Long): List<GameIdAndName>

    @Query("SELECT games.game_id, game_name FROM games LEFT OUTER JOIN collection ON games.game_id = collection.game_id WHERE collection_id IS NULL AND last_viewed < :sinceTimestamp AND num_of_plays = 0 ORDER BY games.updated")
    suspend fun loadNonCollectionAndUnplayedGames(sinceTimestamp: Long): List<GameIdAndName>

    @Query("SELECT game_poll_results_result.*, game_polls.poll_total_votes AS totalVotes FROM game_poll_results_result JOIN game_poll_results ON game_poll_results._id = game_poll_results_result.pollresults_id JOIN game_polls ON game_polls._id = game_poll_results.poll_id WHERE poll_name='suggested_playerage' AND game_id = :gameId ORDER BY pollresultsresult_sortindex")
    suspend fun loadAgePollForGame(gameId: Int): List<GamePollResultsWithPoll>

    @Query("SELECT game_poll_results_result.*, game_polls.poll_total_votes AS totalVotes FROM game_poll_results_result JOIN game_poll_results ON game_poll_results._id = game_poll_results_result.pollresults_id JOIN game_polls ON game_polls._id = game_poll_results.poll_id WHERE poll_name='language_dependence' AND game_id = :gameId ORDER BY pollresultsresult_sortindex")
    suspend fun loadLanguagePollForGame(gameId: Int): List<GamePollResultsWithPoll>

    @Query("SELECT * FROM game_suggested_player_count_poll_results WHERE game_id = :gameId")
    suspend fun loadPlayerPollForGame(gameId: Int): List<GameSuggestedPlayerCountPollResultsEntity>

    @Query("SELECT * FROM game_ranks WHERE game_id = :gameId")
    suspend fun loadRanksForGame(gameId: Int): List<GameRankEntity>

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

    @Query("SELECT games_expansions.*, games.thumbnail_url AS thumbnailUrl FROM games_expansions LEFT OUTER JOIN games ON games.game_id = games_expansions.expansion_id WHERE inbound=0 AND games_expansions.game_id = :gameId")
    suspend fun loadExpansionsForGame(gameId: Int): List<GameExpansionWithGame>

    @Query("SELECT games_expansions.*, games.thumbnail_url AS thumbnailUrl FROM games_expansions LEFT OUTER JOIN games ON games.game_id = games_expansions.expansion_id WHERE inbound=1 AND games_expansions.game_id = :gameId")
    suspend fun loadBaseGamesForGame(gameId: Int): List<GameExpansionWithGame>

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
