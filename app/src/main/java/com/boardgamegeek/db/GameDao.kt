package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import android.text.format.DateUtils
import androidx.core.content.contentValuesOf
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.provider.BggContract.Companion.PATH_ARTISTS
import com.boardgamegeek.provider.BggContract.Companion.PATH_CATEGORIES
import com.boardgamegeek.provider.BggContract.Companion.PATH_DESIGNERS
import com.boardgamegeek.provider.BggContract.Companion.PATH_EXPANSIONS
import com.boardgamegeek.provider.BggContract.Companion.PATH_MECHANICS
import com.boardgamegeek.provider.BggContract.Companion.PATH_PUBLISHERS
import com.boardgamegeek.provider.BggDatabase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameDao(private val context: Context) {
    suspend fun save(game: GameForUpsert) = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()

        val values = toValues(game)
        val internalId = context.contentResolver.queryLong(Games.buildGameUri(game.gameId), BaseColumns._ID, INVALID_ID.toLong())
        val cpoBuilder = if (internalId != INVALID_ID.toLong()) {
            values.remove(Games.Columns.GAME_ID)
            if (shouldClearHeroImageUrl(game)) {
                values.put(Games.Columns.HERO_IMAGE_URL, "")
            }
            ContentProviderOperation.newUpdate(Games.buildGameUri(game.gameId))
        } else {
            ContentProviderOperation.newInsert(Games.CONTENT_URI)
        }

        batch += cpoBuilder.withValues(values).withYieldAllowed(true).build()
        batch += createRanksBatch(game)
        if (game.polls != null) batch += createPollsBatch(game)
        batch += createPlayerPollBatch(game.gameId, game.playerPoll)
        batch += createExpansionsBatch(game.gameId, game.expansions)

        batch += createAssociationBatch(game.gameId, game.designers, PATH_DESIGNERS, GamesDesigners.DESIGNER_ID)
        batch += createAssociationBatch(game.gameId, game.artists, PATH_ARTISTS, GamesArtists.ARTIST_ID)
        batch += createAssociationBatch(game.gameId, game.publishers, PATH_PUBLISHERS, GamesPublishers.PUBLISHER_ID)
        batch += createAssociationBatch(game.gameId, game.categories, PATH_CATEGORIES, GamesCategories.CATEGORY_ID)
        batch += createAssociationBatch(game.gameId, game.mechanics, PATH_MECHANICS, GamesMechanics.MECHANIC_ID)

        context.contentResolver.applyBatch(batch, "Game $game")
        val dateTime =
            DateUtils.formatDateTime(context, game.updated ?: System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
        if (internalId == INVALID_ID.toLong()) {
            Timber.i("Inserted game $game at $dateTime")
        } else {
            Timber.i("Updated game $game (${internalId}) at $dateTime")
        }
    }

    private fun toValues(game: GameForUpsert): ContentValues {
        val values = contentValuesOf(
            Games.Columns.UPDATED to game.updated,
            Games.Columns.UPDATED_LIST to game.updatedList,
            Games.Columns.GAME_ID to game.gameId,
            Games.Columns.GAME_NAME to game.gameName,
            Games.Columns.GAME_SORT_NAME to game.gameSortName,
            Games.Columns.THUMBNAIL_URL to game.thumbnailUrl,
            Games.Columns.IMAGE_URL to game.imageUrl,
            Games.Columns.DESCRIPTION to game.description,
            Games.Columns.SUBTYPE to game.subtype,
            Games.Columns.YEAR_PUBLISHED to game.yearPublished,
            Games.Columns.MIN_PLAYERS to game.minPlayers,
            Games.Columns.MAX_PLAYERS to game.maxPlayers,
            Games.Columns.PLAYING_TIME to game.playingTime,
            Games.Columns.MIN_PLAYING_TIME to game.minPlayingTime,
            Games.Columns.MAX_PLAYING_TIME to game.maxPlayingTime,
            Games.Columns.MINIMUM_AGE to game.minimumAge,
            Games.Columns.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL to game.suggestedPlayerCountPollVoteTotal,
            Games.Columns.PLAYER_COUNTS_BEST to game.playerCountsBest,
            Games.Columns.PLAYER_COUNTS_RECOMMENDED to game.playerCountsRecommended,
            Games.Columns.PLAYER_COUNTS_NOT_RECOMMENDED to game.playerCountsNotRecommended,
        )
        val statsValues = contentValuesOf(
            Games.Columns.GAME_RANK to game.gameRank,
            Games.Columns.STATS_AVERAGE to game.average,
            Games.Columns.STATS_BAYES_AVERAGE to game.bayesAverage,
            Games.Columns.STATS_STANDARD_DEVIATION to game.standardDeviation,
            Games.Columns.STATS_MEDIAN to game.median,
            Games.Columns.STATS_USERS_RATED to game.numberOfRatings,
            Games.Columns.STATS_NUMBER_OWNED to game.numberOfUsersOwned,
            Games.Columns.STATS_NUMBER_TRADING to game.numberOfUsersTrading,
            Games.Columns.STATS_NUMBER_WANTING to game.numberOfUsersWanting,
            Games.Columns.STATS_NUMBER_WISHING to game.numberOfUsersWishListing,
            Games.Columns.STATS_NUMBER_COMMENTS to game.numberOfComments,
            Games.Columns.STATS_NUMBER_WEIGHTS to game.numberOfUsersWeighting,
            Games.Columns.STATS_AVERAGE_WEIGHT to game.averageWeight,
        )
        values.putAll(statsValues)
        return values
    }

    private suspend fun shouldClearHeroImageUrl(game: GameForUpsert): Boolean = withContext(Dispatchers.IO) {
        context.contentResolver.load(Games.buildGameUri(game.gameId), arrayOf(Games.Columns.IMAGE_URL, Games.Columns.THUMBNAIL_URL))?.use {
            if (it.moveToFirst()) {
                val imageUrl = it.getStringOrNull(0).orEmpty()
                val thumbnailUrl = it.getStringOrNull(1).orEmpty()
                imageUrl != game.imageUrl || thumbnailUrl != game.thumbnailUrl
            } else false
        } ?: false
    }

    private suspend fun createPollsBatch(game: GameForUpsert): ArrayList<ContentProviderOperation> = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()

        val existingPollNames = context.contentResolver
            .queryStrings(Games.buildPollsUri(game.gameId), GamePolls.Columns.POLL_NAME)
            .filter { it.isNotBlank() }
            .toMutableList()
        game.polls?.forEach { poll ->
            val values = contentValuesOf(
                GamePolls.Columns.POLL_TITLE to poll.pollTitle,
                GamePolls.Columns.POLL_TOTAL_VOTES to poll.pollTotalVotes,
            )

            val existingResultKeys = mutableListOf<String>()
            batch += if (existingPollNames.remove(poll.pollName)) {
                existingResultKeys += context.contentResolver.queryStrings(
                    Games.buildPollResultsUri(game.gameId, poll.pollName),
                    GamePollResults.Columns.POLL_RESULTS_PLAYERS
                ).filterNot { it.isBlank() }
                ContentProviderOperation.newUpdate(Games.buildPollsUri(game.gameId, poll.pollName))
            } else {
                values.put(GamePolls.Columns.POLL_NAME, poll.pollName)
                ContentProviderOperation.newInsert(Games.buildPollsUri(game.gameId))
            }.withValues(values).build()

            for (results in poll.results) {
                val resultsValues = contentValuesOf(
                    GamePollResults.Columns.POLL_RESULTS_PLAYERS to results.pollResultsPlayers,
                    GamePollResults.Columns.POLL_RESULTS_SORT_INDEX to results.pollResultsSortIndex,
                )

                val existingResultsResultKeys = mutableListOf<String>()
                batch += if (existingResultKeys.remove(results.pollResultsKey)) {
                    existingResultsResultKeys += context.contentResolver.queryStrings(
                        Games.buildPollResultsResultUri(game.gameId, poll.pollName, results.pollResultsKey),
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_KEY
                    ).filterNot { it.isBlank() }
                    ContentProviderOperation.newUpdate(Games.buildPollResultsUri(game.gameId, poll.pollName, results.pollResultsKey))
                } else {
                    resultsValues.put(GamePollResults.Columns.POLL_RESULTS_KEY, results.pollResultsKey)
                    ContentProviderOperation.newInsert(Games.buildPollResultsUri(game.gameId, poll.pollName))
                }.withValues(resultsValues).build()

                for (result in results.pollResultsResult) {
                    val resultsResultValues = contentValuesOf(
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_LEVEL to result.pollResultsResultLevel,
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VALUE to result.pollResultsResultValue,
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VOTES to result.pollResultsResultVotes,
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_SORT_INDEX to result.pollResultsResulSortIndex,
                    )

                    batch += if (existingResultsResultKeys.remove(result.pollResultsResultKey)) {
                        ContentProviderOperation.newUpdate(
                            Games.buildPollResultsResultUri(
                                game.gameId,
                                poll.pollName,
                                results.pollResultsKey,
                                result.pollResultsResultKey
                            )
                        )
                    } else {
                        resultsValues.put(GamePollResultsResult.Columns.POLL_RESULTS_RESULT_KEY, result.pollResultsResultKey)
                        ContentProviderOperation.newInsert(Games.buildPollResultsResultUri(game.gameId, poll.pollName, results.pollResultsKey))
                    }.withValues(resultsResultValues).build()
                }

                existingResultsResultKeys.mapTo(batch) {
                    ContentProviderOperation.newDelete(Games.buildPollResultsResultUri(game.gameId, poll.pollName, results.pollResultsKey, it))
                        .build()
                }
            }

            existingResultKeys.mapTo(batch) { ContentProviderOperation.newDelete(Games.buildPollResultsUri(game.gameId, poll.pollName, it)).build() }
        }
        existingPollNames.mapTo(batch) { ContentProviderOperation.newDelete(Games.buildPollsUri(game.gameId, it)).build() }
    }

    private suspend fun createPlayerPollBatch(
        gameId: Int,
        poll: List<GameSuggestedPlayerCountPollResultsLocal>?
    ): ArrayList<ContentProviderOperation> =
        withContext(Dispatchers.IO) {
            val batch = arrayListOf<ContentProviderOperation>()
            if (poll == null)
                batch
            else {
                val existingResults = context.contentResolver.queryStrings(
                    Games.buildSuggestedPlayerCountPollResultsUri(gameId),
                    GameSuggestedPlayerCountPollPollResults.Columns.PLAYER_COUNT
                ).toMutableList()
                for (results in poll) {
                    val values = contentValuesOf(
                        GameSuggestedPlayerCountPollPollResults.Columns.SORT_INDEX to results.sortIndex,
                        GameSuggestedPlayerCountPollPollResults.Columns.BEST_VOTE_COUNT to results.bestVoteCount,
                        GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDED_VOTE_COUNT to results.recommendedVoteCount,
                        GameSuggestedPlayerCountPollPollResults.Columns.NOT_RECOMMENDED_VOTE_COUNT to results.notRecommendedVoteCount,
                        GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDATION to results.recommendation,
                    )
                    batch += if (existingResults.remove(results.playerCount)) {
                        val uri = Games.buildSuggestedPlayerCountPollResultsUri(gameId, results.playerCount)
                        ContentProviderOperation.newUpdate(uri).withValues(values).build()
                    } else {
                        values.put(GameSuggestedPlayerCountPollPollResults.Columns.PLAYER_COUNT, results.playerCount)
                        val uri = Games.buildSuggestedPlayerCountPollResultsUri(gameId)
                        ContentProviderOperation.newInsert(uri).withValues(values).build()
                    }
                }
                existingResults.mapTo(batch) { ContentProviderOperation.newDelete(Games.buildSuggestedPlayerCountPollResultsUri(gameId, it)).build() }
            }
        }

    private suspend fun createRanksBatch(game: GameForUpsert): ArrayList<ContentProviderOperation> = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()
        game.ranks?.let {
            val existingRankIds = context.contentResolver.queryInts(
                GameRanks.CONTENT_URI,
                GameRanks.Columns.GAME_RANK_ID,
                "${GameRanks.Columns.GAME_ID}=?",
                arrayOf(game.gameId.toString())
            ).toMutableList()
            for (rank in game.ranks) {
                val values = contentValuesOf(
                    GameRanks.Columns.GAME_RANK_TYPE to rank.gameRankType,
                    GameRanks.Columns.GAME_RANK_NAME to rank.gameRankName,
                    GameRanks.Columns.GAME_RANK_FRIENDLY_NAME to rank.gameFriendlyRankName,
                    GameRanks.Columns.GAME_RANK_VALUE to rank.gameRankValue,
                    GameRanks.Columns.GAME_RANK_BAYES_AVERAGE to rank.gameRankBayesAverage,
                )
                batch += if (existingRankIds.remove(rank.gameRankId)) {
                    ContentProviderOperation.newUpdate(Games.buildRanksUri(rank.gameId, rank.gameRankId)).withValues(values).build()
                } else {
                    values.put(GameRanks.Columns.GAME_RANK_ID, rank.gameRankId)
                    ContentProviderOperation.newInsert(Games.buildRanksUri(rank.gameId)).withValues(values).build()
                }
            }
            existingRankIds.mapTo(batch) { ContentProviderOperation.newDelete(GameRanks.buildGameRankUri(it)).build() }
        } ?: batch
    }

    private suspend fun createExpansionsBatch(
        gameId: Int,
        newLinks: List<Triple<Int, String, Boolean>>?
    ): ArrayList<ContentProviderOperation> = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()
        newLinks?.let {
            val pathUri = Games.buildPathUri(gameId, PATH_EXPANSIONS)
            val existingIds = context.contentResolver.queryInts(pathUri, GamesExpansions.Columns.EXPANSION_ID).toMutableList()

            for ((id, name, inbound) in newLinks) {
                if (!existingIds.remove(id)) {
                    // insert association row
                    batch.add(
                        ContentProviderOperation.newInsert(pathUri).withValues(
                            contentValuesOf(
                                GamesExpansions.Columns.EXPANSION_ID to id,
                                GamesExpansions.Columns.EXPANSION_NAME to name,
                                GamesExpansions.Columns.INBOUND to inbound,
                            )
                        ).build()
                    )
                }
            }
            // remove unused associations
            existingIds.mapTo(batch) { ContentProviderOperation.newDelete(Games.buildPathUri(gameId, PATH_EXPANSIONS, it)).build() }
        } ?: batch
    }

    private fun createAssociationBatch(
        gameId: Int,
        newLinks: List<Pair<Int, String>>?,
        uriPath: String,
        idColumn: String
    ): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        if (newLinks == null) return batch
        val associationUri = Games.buildPathUri(gameId, uriPath)
        val existingIds = context.contentResolver.queryInts(associationUri, idColumn).toMutableList()
        for ((id, _) in newLinks) {
            if (!existingIds.remove(id)) {
                // insert association row
                batch += ContentProviderOperation.newInsert(associationUri).withValue(idColumn, id).build()
            }
        }
        // remove unused associations
        return existingIds.mapTo(batch) { ContentProviderOperation.newDelete(Games.buildPathUri(gameId, uriPath, it)).build() }
    }
}
