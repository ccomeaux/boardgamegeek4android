package com.boardgamegeek.db

import androidx.room.*
import com.boardgamegeek.db.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Transaction
    suspend fun upsert(game: GameForUpsert): Long {
        val internalId = if (game.header.internalId == 0L) {
            insertGame(game.header)
        } else {
            updateGame(game.header)
            deleteGameRanksForGame(game.header.gameId)
            deleteAgePollForGame(game.header.gameId)
            deleteLanguagePollForGame(game.header.gameId)
            deletePlayerPollForGame(game.header.gameId)
            deleteDesignersForGame(game.header.gameId)
            deleteArtistsForGame(game.header.gameId)
            deletePublishersForGame(game.header.gameId)
            deleteCategoriesForGame(game.header.gameId)
            deleteMechanicsForGame(game.header.gameId)
            deleteExpansionsForGame(game.header.gameId)
            game.header.internalId
        }
        insertRanks(game.ranks)
        insertAgePoll(game.agePollResults)
        insertLanguagePoll(game.languagePollResults)
        insertPlayerPoll(game.playerPoll)
        insertDesigners(game.designers)
        insertArtists(game.artists)
        insertPublishers(game.publishers)
        insertCategories(game.categories)
        insertMechanics(game.mechanics)
        insertExpansions(game.expansions)
        return internalId
    }

    @Insert(GameEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameForUpsertHeader): Long

    @Update(GameEntity::class)
    suspend fun updateGame(game: GameForUpsertHeader)

    @Insert(GameEntity::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGame(game: CollectionGameForUpsert): Long

    @Update(GameEntity::class, onConflict = OnConflictStrategy.IGNORE)
    suspend fun updateGame(game: CollectionGameForUpsert)

    @Insert
    suspend fun insertRanks(ranks: List<GameRankEntity>)

    @Insert
    suspend fun insertAgePoll(pollResults: List<GameAgePollResultEntity>)

    @Insert
    suspend fun insertLanguagePoll(pollResults: List<GameLanguagePollResultEntity>)

    @Insert
    suspend fun insertPlayerPoll(pollResults: List<GameSuggestedPlayerCountPollResultsEntity>)

    @Insert
    suspend fun insertDesigners(designers: List<GameDesignerEntity>)

    @Insert
    suspend fun insertArtists(designers: List<GameArtistEntity>)

    @Insert
    suspend fun insertPublishers(designers: List<GamePublisherEntity>)

    @Insert
    suspend fun insertCategories(designers: List<GameCategoryEntity>)

    @Insert
    suspend fun insertMechanics(designers: List<GameMechanicEntity>)

    @Insert
    suspend fun insertExpansions(designers: List<GameExpansionEntity>)

    @Query("DELETE FROM game_ranks WHERE game_id = :gameId") // TODO test that this cascades the delete
    suspend fun deleteGameRanksForGame(gameId: Int)

    @Query("DELETE FROM game_poll_age_results WHERE game_id = :gameId")
    suspend fun deleteAgePollForGame(gameId: Int)

    @Query("DELETE FROM game_poll_language_results WHERE game_id = :gameId")
    suspend fun deleteLanguagePollForGame(gameId: Int)

    @Query("DELETE FROM game_suggested_player_count_poll_results WHERE game_id = :gameId")
    suspend fun deletePlayerPollForGame(gameId: Int)

    @Query("DELETE FROM games_designers WHERE game_id = :gameId")
    suspend fun deleteDesignersForGame(gameId: Int)

    @Query("DELETE FROM games_artists WHERE game_id = :gameId")
    suspend fun deleteArtistsForGame(gameId: Int)

    @Query("DELETE FROM games_publishers WHERE game_id = :gameId")
    suspend fun deletePublishersForGame(gameId: Int)

    @Query("DELETE FROM games_categories WHERE game_id = :gameId")
    suspend fun deleteCategoriesForGame(gameId: Int)

    @Query("DELETE FROM games_mechanics WHERE game_id = :gameId")
    suspend fun deleteMechanicsForGame(gameId: Int)

    @Query("DELETE FROM games_expansions WHERE game_id = :gameId")
    suspend fun deleteExpansionsForGame(gameId: Int)

    @Query("SELECT games.*, MAX(plays.date) AS lastPlayedDate FROM games LEFT OUTER JOIN plays ON games.game_id = plays.object_id WHERE game_id = :gameId")
    suspend fun loadGame(gameId: Int): GameWithLastPlayed?

    @Query("SELECT games.*, MAX(plays.date) AS lastPlayedDate FROM games LEFT OUTER JOIN plays ON games.game_id = plays.object_id WHERE game_id = :gameId")
    fun loadGameFlow(gameId: Int): Flow<GameWithLastPlayed?>

    @Query("SELECT game_id, game_name FROM games WHERE (updated != 0 OR updated IS NOT NULL) ORDER BY updated LIMIT :gamesPerFetch")
    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int): List<GameIdAndName>

    @Query("SELECT game_id, game_name FROM games WHERE (updated = 0 OR updated IS NULL) ORDER BY updated_list LIMIT :gamesPerFetch")
    suspend fun loadUnupdatedGames(gamesPerFetch: Int): List<GameIdAndName>

    @Query("SELECT games.game_id, game_name FROM games LEFT OUTER JOIN collection ON games.game_id = collection.game_id WHERE collection_id IS NULL AND last_viewed < :sinceTimestamp ORDER BY games.updated")
    suspend fun loadNonCollectionGames(sinceTimestamp: Long): List<GameIdAndName>

    @Query("SELECT games.game_id, game_name FROM games LEFT OUTER JOIN collection ON games.game_id = collection.game_id WHERE collection_id IS NULL AND last_viewed < :sinceTimestamp AND num_of_plays = 0 ORDER BY games.updated")
    suspend fun loadNonCollectionAndUnplayedGames(sinceTimestamp: Long): List<GameIdAndName>

    @Query("SELECT * FROM game_poll_age_results WHERE game_id = :gameId ORDER BY value")
    suspend fun loadAgePollForGame(gameId: Int): List<GameAgePollResultEntity>

    @Query("SELECT * FROM game_poll_language_results WHERE game_id = :gameId ORDER BY level")
    suspend fun loadLanguagePollForGame(gameId: Int): List<GameLanguagePollResultEntity>

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
