package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.Builder
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.boardgamegeek.applyBatch
import com.boardgamegeek.entities.GameEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.*
import com.boardgamegeek.queryInts
import com.boardgamegeek.queryStrings
import com.boardgamegeek.rowExists
import com.boardgamegeek.util.DataUtils
import com.boardgamegeek.util.NotificationUtils
import com.boardgamegeek.util.PlayerCountRecommendation
import timber.log.Timber
import java.util.*

class GameDao(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    private val updateTime: Long = System.currentTimeMillis()

    fun save(game: GameEntity) {
        // TODO return the internal ID
        if (TextUtils.isEmpty(game.name)) {
            Timber.w("Missing name from game ID=%s", game.id)
            return
        }

        Timber.i("Saving game %s (%s)", game.name, game.id)

        val batch = arrayListOf<ContentProviderOperation>()

        val cpoBuilder: Builder
        val values = toValues(game, updateTime)
        cpoBuilder = if (resolver.rowExists(Games.buildGameUri(game.id))) {
            values.remove(Games.GAME_ID)
            if (shouldClearHeroImageUrl(game)) {
                values.put(Games.HERO_IMAGE_URL, "")
            }
            ContentProviderOperation.newUpdate(Games.buildGameUri(game.id))
        } else {
            ContentProviderOperation.newInsert(Games.CONTENT_URI)
        }

        batch.add(cpoBuilder.withValues(values).withYieldAllowed(true).build())
        batch.addAll(createRanksBatch(game))
        batch.addAll(createPollsBatch(game, values))
        batch.addAll(createExpansionsBatch(game.id, game.expansions))

        saveReference(game.designers, Designers.CONTENT_URI, Designers.DESIGNER_ID, Designers.DESIGNER_NAME)
        saveReference(game.artists, Artists.CONTENT_URI, Artists.ARTIST_ID, Artists.ARTIST_NAME)
        saveReference(game.publishers, Publishers.CONTENT_URI, Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME)
        saveReference(game.categories, Categories.CONTENT_URI, Categories.CATEGORY_ID, Categories.CATEGORY_NAME)
        saveReference(game.mechanics, Mechanics.CONTENT_URI, Mechanics.MECHANIC_ID, Mechanics.MECHANIC_NAME)

        batch.addAll(createAssociationBatch(game.id, game.designers, BggContract.PATH_DESIGNERS, GamesDesigners.DESIGNER_ID))
        batch.addAll(createAssociationBatch(game.id, game.artists, BggContract.PATH_ARTISTS, GamesArtists.ARTIST_ID))
        batch.addAll(createAssociationBatch(game.id, game.publishers, BggContract.PATH_PUBLISHERS, GamesPublishers.PUBLISHER_ID))
        batch.addAll(createAssociationBatch(game.id, game.categories, BggContract.PATH_CATEGORIES, GamesCategories.CATEGORY_ID))
        batch.addAll(createAssociationBatch(game.id, game.mechanics, BggContract.PATH_MECHANICS, GamesMechanics.MECHANIC_ID))

        try {
            resolver.applyBatch(context, batch, "Game ${game.id}")
            Timber.i("Saved game ID '%s'", game.id)
        } catch (e: Exception) {
            NotificationUtils.showPersistErrorNotification(context, e)
        }
    }

    private fun toValues(game: GameEntity, updateTime: Long): ContentValues {
        val values = ContentValues()
        values.put(Games.UPDATED, updateTime)
        values.put(Games.UPDATED_LIST, updateTime)
        values.put(Games.GAME_ID, game.id)
        values.put(Games.GAME_NAME, game.name)
        values.put(Games.GAME_SORT_NAME, game.sortName)
        values.put(Games.THUMBNAIL_URL, game.thumbnailUrl)
        values.put(Games.IMAGE_URL, game.imageUrl)
        values.put(Games.DESCRIPTION, game.description)
        values.put(Games.SUBTYPE, game.subtype)
        values.put(Games.YEAR_PUBLISHED, game.yearPublished)
        values.put(Games.MIN_PLAYERS, game.minPlayers)
        values.put(Games.MAX_PLAYERS, game.maxPlayers)
        values.put(Games.PLAYING_TIME, game.playingTime)
        values.put(Games.MIN_PLAYING_TIME, game.minPlayingTime)
        values.put(Games.MAX_PLAYING_TIME, game.maxPlayingTime)
        values.put(Games.MINIMUM_AGE, game.minAge)
        if (game.hasStatistics) {
            values.put(Games.STATS_AVERAGE, game.average)
            values.put(Games.STATS_BAYES_AVERAGE, game.bayesAverage)
            values.put(Games.STATS_STANDARD_DEVIATION, game.standardDeviation)
            values.put(Games.STATS_MEDIAN, game.median)
            values.put(Games.STATS_USERS_RATED, game.numberOfRatings)
            values.put(Games.STATS_NUMBER_OWNED, game.numberOfUsersOwned)
            values.put(Games.STATS_NUMBER_TRADING, game.numberOfUsersTrading)
            values.put(Games.STATS_NUMBER_WANTING, game.numberOfUsersWanting)
            values.put(Games.STATS_NUMBER_WISHING, game.numberOfUsersWishListing)
            values.put(Games.STATS_NUMBER_COMMENTS, game.numberOfComments)
            values.put(Games.STATS_NUMBER_WEIGHTS, game.numberOfUsersWeighting)
            values.put(Games.STATS_AVERAGE_WEIGHT, game.averageWeight)
        }
        values.put(Games.GAME_RANK, game.overallRank)
        return values
    }

    private fun shouldClearHeroImageUrl(game: GameEntity): Boolean {
        val cursor = resolver.query(Games.buildGameUri(game.id), arrayOf(Games.IMAGE_URL, Games.THUMBNAIL_URL), null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val imageUrl = c.getString(0) ?: ""
                val thumbnailUrl = c.getString(1) ?: ""
                if (imageUrl != game.imageUrl || thumbnailUrl != game.thumbnailUrl) {
                    return true
                }
            }
        }
        return false
    }

    private fun createPollsBatch(game: GameEntity, gameContentValues: ContentValues): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        val existingPollNames = resolver.queryStrings(Games.buildPollsUri(game.id), GamePolls.POLL_NAME).toMutableList()
        for (poll in game.polls) {
            if ("suggested_numplayers" == poll.name) {
                gameContentValues.put(Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL, poll.totalVotes)
                val existingResults = resolver.queryStrings(Games.buildSuggestedPlayerCountPollResultsUri(game.id), GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT).toMutableList()
                for ((sortIndex, results) in poll.results.withIndex()) {
                    val values = ContentValues(6)
                    val builder = PlayerCountRecommendation.Builder()
                    values.put(GameSuggestedPlayerCountPollPollResults.SORT_INDEX, sortIndex + 1)
                    for (result in results.result) {
                        when (result.value) {
                            "Best" -> {
                                values.put(GameSuggestedPlayerCountPollPollResults.BEST_VOTE_COUNT, result.numberOfVotes)
                                builder.bestVoteCount(result.numberOfVotes)
                            }
                            "Recommended" -> {
                                values.put(GameSuggestedPlayerCountPollPollResults.RECOMMENDED_VOTE_COUNT, result.numberOfVotes)
                                builder.recommendedVoteCount(result.numberOfVotes)
                            }
                            "Not Recommended" -> {
                                values.put(GameSuggestedPlayerCountPollPollResults.NOT_RECOMMENDED_VOTE_COUNT, result.numberOfVotes)
                                builder.notRecommendVoteCount(result.numberOfVotes)
                            }
                            else -> Timber.i("Unexpected suggested player count result of '${result.value}'")
                        }
                    }
                    values.put(GameSuggestedPlayerCountPollPollResults.RECOMMENDATION, builder.build().calculate())
                    if (existingResults.remove(results.key)) {
                        val uri = Games.buildSuggestedPlayerCountPollResultsUri(game.id, results.key)
                        batch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build())
                    } else {
                        values.put(GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT, results.key)
                        val uri = Games.buildSuggestedPlayerCountPollResultsUri(game.id)
                        batch.add(ContentProviderOperation.newInsert(uri).withValues(values).build())
                    }
                }
                for (result in existingResults) {
                    val uri = Games.buildSuggestedPlayerCountPollResultsUri(game.id, result)
                    batch.add(ContentProviderOperation.newDelete(uri).build())
                }
            } else {
                val values = ContentValues()
                values.put(GamePolls.POLL_TITLE, poll.title)
                values.put(GamePolls.POLL_TOTAL_VOTES, poll.totalVotes)

                var existingResultKeys = mutableListOf<String>()
                if (existingPollNames.remove(poll.name)) {
                    batch.add(ContentProviderOperation.newUpdate(Games.buildPollsUri(game.id, poll.name)).withValues(values).build())
                    existingResultKeys = resolver.queryStrings(Games.buildPollResultsUri(game.id, poll.name), GamePollResults.POLL_RESULTS_PLAYERS).toMutableList()
                } else {
                    values.put(GamePolls.POLL_NAME, poll.name)
                    batch.add(ContentProviderOperation.newInsert(Games.buildPollsUri(game.id)).withValues(values).build())
                }

                for ((resultsIndex, results) in poll.results.withIndex()) {
                    values.clear()
                    values.put(GamePollResults.POLL_RESULTS_SORT_INDEX, resultsIndex + 1)

                    var existingValues = mutableListOf<String>()
                    if (existingResultKeys.remove(results.key)) {
                        batch.add(ContentProviderOperation
                                .newUpdate(Games.buildPollResultsUri(game.id, poll.name, results.key))
                                .withValues(values).build())
                        existingValues = resolver.queryStrings(
                                Games.buildPollResultsResultUri(game.id, poll.name, results.key),
                                GamePollResultsResult.POLL_RESULTS_RESULT_KEY).toMutableList()
                    } else {
                        values.put(GamePollResults.POLL_RESULTS_PLAYERS, results.key)
                        batch.add(ContentProviderOperation.newInsert(Games.buildPollResultsUri(game.id, poll.name)).withValues(values).build())
                    }

                    for ((resultSortIndex, result) in results.result.withIndex()) {
                        values.clear()
                        if (result.level > 0)
                            values.put(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL, result.level)
                        values.put(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE, result.value)
                        values.put(GamePollResultsResult.POLL_RESULTS_RESULT_VOTES, result.numberOfVotes)
                        values.put(GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX, resultSortIndex + 1)

                        val key = DataUtils.generatePollResultsKey(result.level, result.value)
                        if (existingValues.remove(key)) {
                            batch.add(ContentProviderOperation.newUpdate(Games.buildPollResultsResultUri(game.id, poll.name, results.key, key))
                                    .withValues(values)
                                    .build())
                        } else {
                            batch.add(ContentProviderOperation
                                    .newInsert(Games.buildPollResultsResultUri(game.id, poll.name, results.key))
                                    .withValues(values)
                                    .build())
                        }
                    }

                    for (value in existingValues) {
                        batch.add(ContentProviderOperation.newDelete(Games.buildPollResultsResultUri(game.id, poll.name, results.key, value)).build())
                    }
                }

                for (player in existingResultKeys) {
                    batch.add(ContentProviderOperation.newDelete(Games.buildPollResultsUri(game.id, poll.name, player)).build())
                }
            }
        }
        for (pollName in existingPollNames) {
            batch.add(ContentProviderOperation.newDelete(Games.buildPollsUri(game.id, pollName)).build())
        }
        return batch
    }

    private fun createRanksBatch(game: GameEntity): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        val existingRankIds = resolver.queryInts(GameRanks.CONTENT_URI, GameRanks.GAME_RANK_ID, "${GameRanks.GAME_RANK_ID}=?", arrayOf(game.id.toString())).toMutableList()
        for ((id, type, name, friendlyName, value, bayesAverage) in game.ranks) {
            val values = ContentValues()
            values.put(GameRanks.GAME_RANK_TYPE, type)
            values.put(GameRanks.GAME_RANK_NAME, name)
            values.put(GameRanks.GAME_RANK_FRIENDLY_NAME, friendlyName)
            values.put(GameRanks.GAME_RANK_VALUE, value)
            values.put(GameRanks.GAME_RANK_BAYES_AVERAGE, bayesAverage)

            if (existingRankIds.remove(id)) {
                batch.add(ContentProviderOperation.newUpdate(Games.buildRanksUri(game.id, id)).withValues(values).build())
            } else {
                values.put(GameRanks.GAME_RANK_ID, id)
                batch.add(ContentProviderOperation.newInsert(Games.buildRanksUri(game.id)).withValues(values).build())
            }
        }
        for (rankId in existingRankIds) {
            batch.add(ContentProviderOperation.newDelete(GameRanks.buildGameRankUri(rankId)).build())
        }
        return batch
    }

    private fun createExpansionsBatch(gameId: Int, newLinks: List<Triple<Int, String, Boolean>>): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        val pathUri = Games.buildPathUri(gameId, BggContract.PATH_EXPANSIONS)
        val existingIds = resolver.queryInts(pathUri, GamesExpansions.EXPANSION_ID).toMutableList()

        for ((id, name, inbound) in newLinks) {
            if (!existingIds.remove(id)) {
                // insert association row
                val values = ContentValues()
                values.put(GamesExpansions.EXPANSION_ID, id)
                values.put(GamesExpansions.EXPANSION_NAME, name)
                values.put(GamesExpansions.INBOUND, inbound)
                batch.add(ContentProviderOperation.newInsert(pathUri).withValues(values).build())
            }
        }
        // remove unused associations
        for (existingId in existingIds) {
            batch.add(ContentProviderOperation.newDelete(Games.buildPathUri(gameId, BggContract.PATH_EXPANSIONS, existingId)).build())
        }
        return batch
    }

    private fun saveReference(newLinks: List<Pair<Int, String>>, baseUri: Uri, idColumn: String, nameColumn: String) {
        val batch = arrayListOf<ContentProviderOperation>()
        for ((id, name) in newLinks) {
            val uri = baseUri.buildUpon().appendPath(id.toString()).build()
            if (resolver.rowExists(uri)) {
                batch.add(ContentProviderOperation.newUpdate(uri).withValue(nameColumn, name).build())
            } else {
                val cv = ContentValues(2)
                cv.put(idColumn, id)
                cv.put(nameColumn, name)
                batch.add(ContentProviderOperation.newInsert(baseUri).withValues(cv).build())
            }
        }
        resolver.applyBatch(context, batch, "Saving ${baseUri.lastPathSegment}")
    }

    private fun createAssociationBatch(gameId: Int, newLinks: List<Pair<Int, String>>, uriPath: String, idColumn: String): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        val associationUri = Games.buildPathUri(gameId, uriPath)
        val existingIds = resolver.queryInts(associationUri, idColumn).toMutableList()
        for ((id, _) in newLinks) {
            if (!existingIds.remove(id)) {
                // insert association row
                batch.add(ContentProviderOperation.newInsert(associationUri).withValue(idColumn, id).build())
            }
        }
        // remove unused associations
        for (existingId in existingIds) {
            batch.add(ContentProviderOperation.newDelete(Games.buildPathUri(gameId, uriPath, existingId)).build())
        }
        return batch
    }
}
