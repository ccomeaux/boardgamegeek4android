package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.Builder
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Color
import android.net.Uri
import android.provider.BaseColumns
import android.text.format.DateUtils
import androidx.core.content.contentValuesOf
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.provider.BggContract.Companion.PATH_ARTISTS
import com.boardgamegeek.provider.BggContract.Companion.PATH_CATEGORIES
import com.boardgamegeek.provider.BggContract.Companion.PATH_DESIGNERS
import com.boardgamegeek.provider.BggContract.Companion.PATH_EXPANSIONS
import com.boardgamegeek.provider.BggContract.Companion.PATH_MECHANICS
import com.boardgamegeek.provider.BggContract.Companion.PATH_PUBLISHERS
import com.boardgamegeek.provider.BggContract.Companion.POLL_TYPE_LANGUAGE_DEPENDENCE
import com.boardgamegeek.provider.BggContract.Companion.POLL_TYPE_SUGGESTED_PLAYER_AGE
import com.boardgamegeek.provider.BggDatabase.*
import com.boardgamegeek.util.DataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameDao(private val context: BggApplication) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun load(gameId: Int): GameEntity? = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            val projection = arrayOf(
                Games.Columns.GAME_ID,
                Games.Columns.GAME_NAME,
                Games.Columns.DESCRIPTION,
                Games.Columns.SUBTYPE,
                Games.Columns.THUMBNAIL_URL,
                Games.Columns.IMAGE_URL, // 5
                Games.Columns.YEAR_PUBLISHED,
                Games.Columns.MIN_PLAYERS,
                Games.Columns.MAX_PLAYERS,
                Games.Columns.PLAYING_TIME,
                Games.Columns.MIN_PLAYING_TIME, // 10
                Games.Columns.MAX_PLAYING_TIME,
                Games.Columns.MINIMUM_AGE,
                Games.Columns.HERO_IMAGE_URL,
                Games.Columns.STATS_AVERAGE,
                Games.Columns.STATS_USERS_RATED, // 15
                Games.Columns.STATS_NUMBER_COMMENTS,
                Games.Columns.UPDATED,
                Games.Columns.UPDATED_PLAYS,
                Games.Columns.GAME_RANK,
                Games.Columns.STATS_STANDARD_DEVIATION, // 20
                Games.Columns.STATS_BAYES_AVERAGE,
                Games.Columns.STATS_AVERAGE_WEIGHT,
                Games.Columns.STATS_NUMBER_WEIGHTS,
                Games.Columns.STATS_NUMBER_OWNED,
                Games.Columns.STATS_NUMBER_TRADING, // 25
                Games.Columns.STATS_NUMBER_WANTING,
                Games.Columns.STATS_NUMBER_WISHING,
                Games.Columns.CUSTOM_PLAYER_SORT,
                Games.Columns.STARRED,
                Games.Columns.POLLS_COUNT, // 30
                Games.Columns.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
                Games.Columns.ICON_COLOR,
                Games.Columns.DARK_COLOR,
                Games.Columns.WINS_COLOR,
                Games.Columns.WINNABLE_PLAYS_COLOR, // 35
                Games.Columns.ALL_PLAYS_COLOR,
                Games.Columns.GAME_SORT_NAME,
            )
            context.contentResolver.load(Games.buildGameUri(gameId), projection)?.use {
                if (it.moveToFirst()) {
                    GameEntity(
                        id = it.getInt(0),
                        name = it.getStringOrNull(1).orEmpty(),
                        description = it.getStringOrNull(2).orEmpty(),
                        subtype = it.getStringOrNull(3).toSubtype(),
                        thumbnailUrl = it.getStringOrNull(4).orEmpty(),
                        imageUrl = it.getStringOrNull(5).orEmpty(),
                        yearPublished = it.getIntOrNull(6) ?: GameEntity.YEAR_UNKNOWN,
                        minPlayers = it.getIntOrNull(7) ?: 0,
                        maxPlayers = it.getIntOrNull(8) ?: 0,
                        playingTime = it.getIntOrNull(9) ?: 0,
                        minPlayingTime = it.getIntOrNull(10) ?: 0,
                        maxPlayingTime = it.getIntOrNull(11) ?: 0,
                        minimumAge = it.getIntOrNull(12) ?: 0,
                        heroImageUrl = it.getStringOrNull(13).orEmpty(),
                        rating = it.getDoubleOrNull(14) ?: 0.0,
                        numberOfRatings = it.getIntOrNull(15) ?: 0,
                        numberOfComments = it.getIntOrNull(16) ?: 0,
                        overallRank = it.getIntOrNull(19) ?: GameRankEntity.RANK_UNKNOWN,
                        standardDeviation = it.getDoubleOrNull(20) ?: 0.0,
                        bayesAverage = it.getDoubleOrNull(21) ?: 0.0,
                        averageWeight = it.getDoubleOrNull(22) ?: 0.0,
                        numberOfUsersWeighting = it.getIntOrNull(23) ?: 0,
                        numberOfUsersOwned = it.getIntOrNull(24) ?: 0,
                        numberOfUsersTrading = it.getIntOrNull(25) ?: 0,
                        numberOfUsersWanting = it.getIntOrNull(26) ?: 0,
                        numberOfUsersWishListing = it.getIntOrNull(27) ?: 0,
                        updated = it.getLongOrNull(17) ?: 0L,
                        updatedPlays = it.getLongOrNull(18) ?: 0L,
                        customPlayerSort = it.getBoolean(28),
                        isFavorite = it.getBoolean(29),
                        pollVoteTotal = it.getIntOrNull(30) ?: 0,
                        suggestedPlayerCountPollVoteTotal = it.getIntOrNull(31) ?: 0,
                        iconColor = it.getIntOrNull(32) ?: Color.TRANSPARENT,
                        darkColor = it.getIntOrNull(33) ?: Color.TRANSPARENT,
                        winsColor = it.getIntOrNull(34) ?: Color.TRANSPARENT,
                        winnablePlaysColor = it.getIntOrNull(35) ?: Color.TRANSPARENT,
                        allPlaysColor = it.getIntOrNull(36) ?: Color.TRANSPARENT,
                        sortName = it.getStringOrNull(37).orEmpty(),
                    )
                } else null
            }
        } else null
    }

    suspend fun loadRanks(gameId: Int): List<GameRankEntity> = withContext(Dispatchers.IO) {
        val ranks = arrayListOf<GameRankEntity>()
        if (gameId != INVALID_ID) {
            val uri = Games.buildRanksUri(gameId)
            context.contentResolver.load(
                uri,
                arrayOf(
                    GameRanks.Columns.GAME_RANK_ID,
                    GameRanks.Columns.GAME_RANK_TYPE,
                    GameRanks.Columns.GAME_RANK_NAME,
                    GameRanks.Columns.GAME_RANK_FRIENDLY_NAME,
                    GameRanks.Columns.GAME_RANK_VALUE,
                    GameRanks.Columns.GAME_RANK_BAYES_AVERAGE,
                )
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        ranks += GameRankEntity(
                            id = it.getIntOrNull(0) ?: INVALID_ID,
                            type = it.getStringOrNull(1).orEmpty(),
                            name = it.getStringOrNull(2).orEmpty(),
                            friendlyName = it.getStringOrNull(3).orEmpty(),
                            value = it.getIntOrNull(4) ?: GameRankEntity.RANK_UNKNOWN,
                            bayesAverage = it.getDoubleOrNull(5) ?: 0.0,
                        )
                    } while (it.moveToNext())
                }
            }
        }
        ranks
    }

    suspend fun loadPoll(gameId: Int, pollType: String): GamePollEntity? = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID && pollType in arrayOf(
                POLL_TYPE_SUGGESTED_PLAYER_AGE,
                POLL_TYPE_LANGUAGE_DEPENDENCE,
            )
        ) {
            val uri = Games.buildPollResultsResultUri(gameId, pollType)
            val results = arrayListOf<GamePollResultEntity>()
            val projection = arrayOf(
                GamePollResultsResult.Columns.POLL_RESULTS_RESULT_LEVEL,
                GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VALUE,
                GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VOTES,
            )
            context.contentResolver.load(uri, projection)?.use {
                if (it.moveToFirst()) {
                    do {
                        results += GamePollResultEntity(
                            level = it.getIntOrNull(0) ?: 0,
                            value = it.getStringOrNull(1).orEmpty(),
                            numberOfVotes = it.getIntOrNull(2) ?: 0,
                        )
                    } while (it.moveToNext())
                }
            }
            GamePollEntity(results)
        } else null
    }

    suspend fun loadPlayerPoll(gameId: Int): GamePlayerPollEntity? = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            val uri = Games.buildSuggestedPlayerCountPollResultsUri(gameId)
            val results = arrayListOf<GamePlayerPollResultsEntity>()
            val projection = arrayOf(
                Games.Columns.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
                GameSuggestedPlayerCountPollPollResults.Columns.PLAYER_COUNT,
                GameSuggestedPlayerCountPollPollResults.Columns.BEST_VOTE_COUNT,
                GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDED_VOTE_COUNT,
                GameSuggestedPlayerCountPollPollResults.Columns.NOT_RECOMMENDED_VOTE_COUNT,
                GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDATION,
            )
            context.contentResolver.load(uri, projection)?.use {
                if (it.moveToFirst()) {
                    do {
                        results += GamePlayerPollResultsEntity(
                            totalVotes = it.getIntOrNull(0) ?: 0,
                            playerCount = it.getStringOrNull(1).orEmpty(),
                            bestVoteCount = it.getIntOrNull(2) ?: 0,
                            recommendedVoteCount = it.getIntOrNull(3) ?: 0,
                            notRecommendedVoteCount = it.getIntOrNull(4) ?: 0,
                            recommendation = it.getIntOrNull(5) ?: GamePlayerPollResultsEntity.UNKNOWN,
                        )
                    } while (it.moveToNext())
                }
            }
            GamePlayerPollEntity(results)
        } else null
    }

    suspend fun loadDesigners(gameId: Int): List<GameDetailEntity> = withContext(Dispatchers.IO) {
        val results = arrayListOf<GameDetailEntity>()
        if (gameId != INVALID_ID) {
            context.contentResolver.load(
                Games.buildDesignersUri(gameId),
                arrayOf(
                    Designers.Columns.DESIGNER_ID,
                    Designers.Columns.DESIGNER_NAME,
                )
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results += GameDetailEntity(
                            it.getInt(0),
                            it.getString(1),
                        )
                    } while (it.moveToNext())
                }
            }
        }
        results
    }

    suspend fun loadArtists(gameId: Int): List<GameDetailEntity> = withContext(Dispatchers.IO) {
        val results = arrayListOf<GameDetailEntity>()
        if (gameId != INVALID_ID) {
            context.contentResolver.load(
                Games.buildArtistsUri(gameId),
                arrayOf(
                    Artists.Columns.ARTIST_ID,
                    Artists.Columns.ARTIST_NAME,
                )
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results += GameDetailEntity(
                            it.getInt(0),
                            it.getString(1),
                        )
                    } while (it.moveToNext())
                }
            }
        }
        results
    }

    suspend fun loadPublishers(gameId: Int): List<GameDetailEntity> = withContext(Dispatchers.IO) {
        val results = arrayListOf<GameDetailEntity>()
        if (gameId != INVALID_ID) {
            context.contentResolver.load(
                Games.buildPublishersUri(gameId),
                arrayOf(
                    Publishers.Columns.PUBLISHER_ID,
                    Publishers.Columns.PUBLISHER_NAME,
                )
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results += GameDetailEntity(
                            it.getInt(0),
                            it.getString(1),
                        )
                    } while (it.moveToNext())
                }
            }
        }
        results
    }

    suspend fun loadCategories(gameId: Int): List<GameDetailEntity> = withContext(Dispatchers.IO) {
        val results = arrayListOf<GameDetailEntity>()
        if (gameId != INVALID_ID) {
            context.contentResolver.load(
                Games.buildCategoriesUri(gameId),
                arrayOf(
                    Categories.Columns.CATEGORY_ID,
                    Categories.Columns.CATEGORY_NAME,
                ),
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results += GameDetailEntity(
                            it.getInt(0),
                            it.getString(1),
                        )
                    } while (it.moveToNext())
                }
            }
        }
        results
    }

    suspend fun loadMechanics(gameId: Int): List<GameDetailEntity> = withContext(Dispatchers.IO) {
        val results = arrayListOf<GameDetailEntity>()
        if (gameId != INVALID_ID) {
            context.contentResolver.load(
                Games.buildMechanicsUri(gameId),
                arrayOf(
                    Mechanics.Columns.MECHANIC_ID,
                    Mechanics.Columns.MECHANIC_NAME,
                ),
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results += GameDetailEntity(
                            it.getInt(0),
                            it.getString(1)
                        )
                    } while (it.moveToNext())
                }
            }
        }
        results
    }

    suspend fun loadExpansions(gameId: Int, inbound: Boolean = false): List<GameExpansionsEntity> =
        withContext(Dispatchers.IO) {
            val results = arrayListOf<GameExpansionsEntity>()
            if (gameId != INVALID_ID) {
                val briefResults = arrayListOf<GameExpansionsEntity>()
                context.contentResolver.load(
                    Games.buildExpansionsUri(gameId),
                    arrayOf(
                        GamesExpansions.Columns.EXPANSION_ID,
                        GamesExpansions.Columns.EXPANSION_NAME,
                    ),
                    selection = "${GamesExpansions.Columns.INBOUND}=?",
                    selectionArgs = arrayOf(if (inbound) "1" else "0")
                )?.use {
                    if (it.moveToFirst()) {
                        do {
                            briefResults += GameExpansionsEntity(
                                it.getInt(0),
                                it.getString(1),
                            )
                        } while (it.moveToNext())
                    }
                }
                for (result in briefResults) {
                    context.contentResolver.load(
                        Collection.CONTENT_URI,
                        projection = arrayOf(
                            Collection.Columns.STATUS_OWN,
                            Collection.Columns.STATUS_PREVIOUSLY_OWNED,
                            Collection.Columns.STATUS_PREORDERED,
                            Collection.Columns.STATUS_FOR_TRADE,
                            Collection.Columns.STATUS_WANT,
                            Collection.Columns.STATUS_WANT_TO_PLAY,
                            Collection.Columns.STATUS_WANT_TO_BUY,
                            Collection.Columns.STATUS_WISHLIST,
                            Collection.Columns.STATUS_WISHLIST_PRIORITY,
                            Games.Columns.NUM_PLAYS,
                            Collection.Columns.RATING,
                            Collection.Columns.COMMENT
                        ),
                        selection = "collection.${Collection.Columns.GAME_ID}=?",
                        selectionArgs = arrayOf(result.id.toString())
                    )?.use {
                        if (it.moveToFirst()) {
                            do {
                                results += result.copy(
                                    own = it.getBoolean(0),
                                    previouslyOwned = it.getBoolean(1),
                                    preOrdered = it.getBoolean(2),
                                    forTrade = it.getBoolean(3),
                                    wantInTrade = it.getBoolean(4),
                                    wantToPlay = it.getBoolean(5),
                                    wantToBuy = it.getBoolean(6),
                                    wishList = it.getBoolean(7),
                                    wishListPriority = it.getIntOrNull(8) ?: GameExpansionsEntity.WISHLIST_PRIORITY_UNKNOWN,
                                    numberOfPlays = it.getIntOrNull(9) ?: 0,
                                    rating = it.getDoubleOrNull(10) ?: 0.0,
                                    comment = it.getStringOrNull(11).orEmpty(),
                                )
                            } while (it.moveToNext())
                        } else {
                            results += result
                        }
                    }
                }
            }
            results
        }

    suspend fun loadPlayColors(gameId: Int): List<String> = withContext(Dispatchers.IO) {
        context.contentResolver.queryStrings(Games.buildColorsUri(gameId), GameColors.Columns.COLOR).filterNot { it.isBlank() }
    }

    suspend fun loadPlayColors(): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        val colors = mutableListOf<Pair<Int, String>>()
        context.contentResolver.load(
            GameColors.CONTENT_URI,
            arrayOf(GameColors.Columns.GAME_ID, GameColors.Columns.COLOR),
        )?.use {
            if (it.moveToFirst()) {
                do {
                    colors += (it.getIntOrNull(0) ?: INVALID_ID) to it.getStringOrNull(1).orEmpty()
                } while (it.moveToNext())
            }
        }
        colors
    }

    suspend fun loadPlayInfo(
        includeIncompletePlays: Boolean,
        includeExpansions: Boolean,
        includeAccessories: Boolean
    ): List<GameForPlayStatEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<GameForPlayStatEntity>()
        context.contentResolver.load(
            Games.CONTENT_PLAYS_URI,
            arrayOf(
                Plays.Columns.SUM_QUANTITY,
                Plays.Columns.ITEM_NAME,
                Games.Columns.GAME_RANK,
                Games.Columns.GAME_ID,
            ),
            selection = mutableListOf<String>().apply {
                add(Plays.Columns.DELETE_TIMESTAMP.whereZeroOrNull())
                if (!includeIncompletePlays) {
                    add(Plays.Columns.INCOMPLETE.whereZeroOrNull())
                }
                if (!includeExpansions && !includeAccessories) {
                    add(Games.Columns.SUBTYPE.whereEqualsOrNull())
                } else if (!includeExpansions || !includeAccessories) {
                    add(Games.Columns.SUBTYPE.whereNotEqualsOrNull())
                }
            }.joinTo(" AND ").toString(),
            selectionArgs = mutableListOf<String>().apply {
                if (!includeExpansions && !includeAccessories) {
                    add(GameEntity.Subtype.BOARDGAME.code)
                } else if (!includeExpansions) {
                    add(GameEntity.Subtype.BOARDGAME_EXPANSION.code)
                } else if (!includeAccessories) {
                    add(GameEntity.Subtype.BOARDGAME_ACCESSORY.code)
                }
            }.toTypedArray(),
            sortOrder = "${Plays.Columns.SUM_QUANTITY} DESC, ${Games.Columns.GAME_SORT_NAME} ASC"
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += GameForPlayStatEntity(
                        it.getIntOrNull(3) ?: INVALID_ID,
                        it.getStringOrNull(1).orEmpty(),
                        it.getIntOrNull(0) ?: 0,
                        it.getIntOrNull(2) ?: GameRankEntity.RANK_UNKNOWN,
                    )
                } while (it.moveToNext())
            }
        }
        results
    }

    suspend fun delete(gameId: Int): Int = withContext(Dispatchers.IO) {
        if (gameId == INVALID_ID) 0
        else resolver.delete(Games.buildGameUri(gameId), null, null)
    }

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        resolver.delete(Games.CONTENT_URI, null, null)
    }

    suspend fun insertColor(gameId: Int, color: String) = withContext(Dispatchers.IO) {
        resolver.insert(Games.buildColorsUri(gameId), contentValuesOf(GameColors.Columns.COLOR to color))
    }

    suspend fun deleteColor(gameId: Int, color: String): Int = withContext(Dispatchers.IO) {
        resolver.delete(Games.buildColorsUri(gameId, color), null, null)
    }

    suspend fun insertColors(gameId: Int, colors: List<String>): Int = withContext(Dispatchers.IO) {
        if (colors.isEmpty()) 0
        else {
            val values = colors.map { contentValuesOf(GameColors.Columns.COLOR to it) }
            resolver.bulkInsert(Games.buildColorsUri(gameId), values.toTypedArray())
        }
    }

    suspend fun update(gameId: Int, values: ContentValues) = withContext(Dispatchers.IO) {
        resolver.update(Games.buildGameUri(gameId), values, null, null)
    }

    suspend fun save(game: GameEntity, updateTime: Long) = withContext(Dispatchers.IO) {
        if (game.name.isBlank()) {
            Timber.w("Missing name from game ID=%s", game.id)
        } else {
            Timber.i("Saving game %s (%s)", game.name, game.id)

            val batch = arrayListOf<ContentProviderOperation>()

            val cpoBuilder: Builder
            val values = toValues(game, updateTime)
            val internalId = resolver.queryLong(Games.buildGameUri(game.id), BaseColumns._ID, INVALID_ID.toLong())
            cpoBuilder = if (internalId != INVALID_ID.toLong()) {
                values.remove(Games.Columns.GAME_ID)
                if (shouldClearHeroImageUrl(game)) {
                    values.put(Games.Columns.HERO_IMAGE_URL, "")
                }
                ContentProviderOperation.newUpdate(Games.buildGameUri(game.id))
            } else {
                ContentProviderOperation.newInsert(Games.CONTENT_URI)
            }

            batch += cpoBuilder.withValues(values).withYieldAllowed(true).build()
            batch += createRanksBatch(game)
            batch += createPollsBatch(game)
            batch += createPlayerPollBatch(game.id, game.playerPoll)
            batch += createExpansionsBatch(game.id, game.expansions)

            saveReference(game.designers, Designers.CONTENT_URI, Designers.Columns.DESIGNER_ID, Designers.Columns.DESIGNER_NAME)
            saveReference(game.artists, Artists.CONTENT_URI, Artists.Columns.ARTIST_ID, Artists.Columns.ARTIST_NAME)
            saveReference(game.publishers, Publishers.CONTENT_URI, Publishers.Columns.PUBLISHER_ID, Publishers.Columns.PUBLISHER_NAME)
            saveReference(game.categories, Categories.CONTENT_URI, Categories.Columns.CATEGORY_ID, Categories.Columns.CATEGORY_NAME)
            saveReference(game.mechanics, Mechanics.CONTENT_URI, Mechanics.Columns.MECHANIC_ID, Mechanics.Columns.MECHANIC_NAME)

            batch += createAssociationBatch(game.id, game.designers, PATH_DESIGNERS, GamesDesigners.DESIGNER_ID)
            batch += createAssociationBatch(game.id, game.artists, PATH_ARTISTS, GamesArtists.ARTIST_ID)
            batch += createAssociationBatch(game.id, game.publishers, PATH_PUBLISHERS, GamesPublishers.PUBLISHER_ID)
            batch += createAssociationBatch(game.id, game.categories, PATH_CATEGORIES, GamesCategories.CATEGORY_ID)
            batch += createAssociationBatch(game.id, game.mechanics, PATH_MECHANICS, GamesMechanics.MECHANIC_ID)

            resolver.applyBatch(batch, "Game ${game.id}")
            if (internalId == INVALID_ID.toLong()) {
                Timber.i(
                    "Inserted game ID '%s' at %s",
                    game.id,
                    DateUtils.formatDateTime(context, updateTime, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
                )
            } else {
                Timber.i(
                    "Updated game ID '%s' (%s) at %s",
                    game.id,
                    internalId,
                    DateUtils.formatDateTime(context, updateTime, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
                )
            }
        }
    }

    suspend fun upsert(gameId: Int, values: ContentValues): Int = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Games.buildGameUri(gameId)
        if (resolver.rowExists(uri)) {
            val count = resolver.update(uri, values, null, null)
            Timber.d("Updated %,d game rows at %s", count, uri)
            count
        } else {
            values.put(Games.Columns.GAME_ID, gameId)
            val insertedUri = resolver.insert(Artists.CONTENT_URI, values)
            Timber.d("Inserted game at %s", insertedUri)
            1
        }
    }

    suspend fun resetPlaySync(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.update(Games.CONTENT_URI, contentValuesOf(Games.Columns.UPDATED_PLAYS to 0), null, null)
    }

    private fun toValues(game: GameEntity, updateTime: Long): ContentValues {
        val values = contentValuesOf(
            Games.Columns.UPDATED to updateTime,
            Games.Columns.UPDATED_LIST to updateTime,
            Games.Columns.GAME_ID to game.id,
            Games.Columns.GAME_NAME to game.name,
            Games.Columns.GAME_SORT_NAME to game.sortName,
            Games.Columns.THUMBNAIL_URL to game.thumbnailUrl,
            Games.Columns.IMAGE_URL to game.imageUrl,
            Games.Columns.DESCRIPTION to game.description,
            Games.Columns.SUBTYPE to game.subtype?.code.orEmpty(),
            Games.Columns.YEAR_PUBLISHED to game.yearPublished,
            Games.Columns.MIN_PLAYERS to game.minPlayers,
            Games.Columns.MAX_PLAYERS to game.maxPlayers,
            Games.Columns.PLAYING_TIME to game.playingTime,
            Games.Columns.MIN_PLAYING_TIME to game.minPlayingTime,
            Games.Columns.MAX_PLAYING_TIME to game.maxPlayingTime,
            Games.Columns.MINIMUM_AGE to game.minimumAge,
            Games.Columns.GAME_RANK to game.overallRank,
        )
        val statsValues = if (game.hasStatistics) {
            contentValuesOf(
                Games.Columns.STATS_AVERAGE to game.rating,
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
        } else contentValuesOf()
        val pollValues = game.playerPoll?.let {
            contentValuesOf(
                Games.Columns.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL to it.totalVotes,
                Games.Columns.PLAYER_COUNTS_BEST to it.bestCounts.forDatabase(GamePlayerPollEntity.separator),
                Games.Columns.PLAYER_COUNTS_RECOMMENDED to it.recommendedAndBestCounts.forDatabase(GamePlayerPollEntity.separator),
                Games.Columns.PLAYER_COUNTS_NOT_RECOMMENDED to it.notRecommendedCounts.forDatabase(GamePlayerPollEntity.separator),
            )
        } ?: contentValuesOf()
        values.putAll(statsValues)
        values.putAll(pollValues)
        return values
    }

    private suspend fun shouldClearHeroImageUrl(game: GameEntity): Boolean = withContext(Dispatchers.IO) {
        val cursor = resolver.query(Games.buildGameUri(game.id), arrayOf(Games.Columns.IMAGE_URL, Games.Columns.THUMBNAIL_URL), null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val imageUrl = c.getStringOrNull(0).orEmpty()
                val thumbnailUrl = c.getStringOrNull(1).orEmpty()
                imageUrl != game.imageUrl || thumbnailUrl != game.thumbnailUrl
            } else false
        } ?: false
    }

    private suspend fun createPollsBatch(game: GameEntity): ArrayList<ContentProviderOperation> = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()
        val existingPollNames = resolver.queryStrings(Games.buildPollsUri(game.id), GamePolls.Columns.POLL_NAME).filterNot { it.isBlank() }.toMutableList()
        for (poll in game.polls) {
            val values = contentValuesOf(
                GamePolls.Columns.POLL_TITLE to poll.title,
                GamePolls.Columns.POLL_TOTAL_VOTES to poll.totalVotes,
            )

            val existingResultKeys = mutableListOf<String>()
            batch += if (existingPollNames.remove(poll.name)) {
                existingResultKeys += resolver.queryStrings(
                    Games.buildPollResultsUri(game.id, poll.name),
                    GamePollResults.Columns.POLL_RESULTS_PLAYERS
                ).filterNot { it.isBlank() }
                ContentProviderOperation.newUpdate(Games.buildPollsUri(game.id, poll.name))
            } else {
                values.put(GamePolls.Columns.POLL_NAME, poll.name)
                ContentProviderOperation.newInsert(Games.buildPollsUri(game.id))
            }.withValues(values).build()

            for ((resultsIndex, results) in poll.results.withIndex()) {
                val resultsValues = contentValuesOf(GamePollResults.Columns.POLL_RESULTS_SORT_INDEX to resultsIndex + 1)

                val existingResultsResultKeys = mutableListOf<String>()
                batch += if (existingResultKeys.remove(results.key)) {
                    existingResultsResultKeys += resolver.queryStrings(
                        Games.buildPollResultsResultUri(game.id, poll.name, results.key),
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_KEY
                    ).filterNot { it.isBlank() }
                    ContentProviderOperation.newUpdate(Games.buildPollResultsUri(game.id, poll.name, results.key))
                } else {
                    resultsValues.put(GamePollResults.Columns.POLL_RESULTS_PLAYERS, results.key)
                    ContentProviderOperation.newInsert(Games.buildPollResultsUri(game.id, poll.name))
                }.withValues(resultsValues).build()

                for ((resultSortIndex, result) in results.result.withIndex()) {
                    val resultsResultValues = contentValuesOf(
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VALUE to result.value,
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VOTES to result.numberOfVotes,
                        GamePollResultsResult.Columns.POLL_RESULTS_RESULT_SORT_INDEX to resultSortIndex + 1,
                    )
                    if (result.level > 0) resultsResultValues.put(GamePollResultsResult.Columns.POLL_RESULTS_RESULT_LEVEL, result.level)

                    val key = DataUtils.generatePollResultsKey(result.level, result.value)
                    batch += if (existingResultsResultKeys.remove(key)) {
                        ContentProviderOperation.newUpdate(Games.buildPollResultsResultUri(game.id, poll.name, results.key, key))
                    } else {
                        ContentProviderOperation.newInsert(Games.buildPollResultsResultUri(game.id, poll.name, results.key))
                    }.withValues(resultsResultValues).build()
                }

                existingResultsResultKeys.mapTo(batch) {
                    ContentProviderOperation.newDelete(Games.buildPollResultsResultUri(game.id, poll.name, results.key, it)).build()
                }
            }

            existingResultKeys.mapTo(batch) { ContentProviderOperation.newDelete(Games.buildPollResultsUri(game.id, poll.name, it)).build() }
        }
        existingPollNames.mapTo(batch) { ContentProviderOperation.newDelete(Games.buildPollsUri(game.id, it)).build() }
    }

    private suspend fun createPlayerPollBatch(gameId: Int, poll: GamePlayerPollEntity?): ArrayList<ContentProviderOperation> =
        withContext(Dispatchers.IO) {
            val batch = arrayListOf<ContentProviderOperation>()
            if (poll == null)
                batch
            else {
                val existingResults = resolver.queryStrings(
                    Games.buildSuggestedPlayerCountPollResultsUri(gameId),
                    GameSuggestedPlayerCountPollPollResults.Columns.PLAYER_COUNT
                ).filterNot { it.isBlank() }.toMutableList()
                for ((sortIndex, results) in poll.results.withIndex()) {
                    val values = contentValuesOf(
                        GameSuggestedPlayerCountPollPollResults.Columns.SORT_INDEX to sortIndex + 1,
                        GameSuggestedPlayerCountPollPollResults.Columns.BEST_VOTE_COUNT to results.bestVoteCount,
                        GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDED_VOTE_COUNT to results.recommendedVoteCount,
                        GameSuggestedPlayerCountPollPollResults.Columns.NOT_RECOMMENDED_VOTE_COUNT to results.notRecommendedVoteCount,
                        GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDATION to results.calculatedRecommendation,
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

    private suspend fun createRanksBatch(game: GameEntity): ArrayList<ContentProviderOperation> = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()
        val existingRankIds = resolver.queryInts(
            GameRanks.CONTENT_URI,
            GameRanks.Columns.GAME_RANK_ID,
            "${GameRanks.Columns.GAME_RANK_ID}=?",
            arrayOf(game.id.toString())
        ).toMutableList()
        for ((id, type, name, friendlyName, value, bayesAverage) in game.ranks) {
            val values = contentValuesOf(
                GameRanks.Columns.GAME_RANK_TYPE to type,
                GameRanks.Columns.GAME_RANK_NAME to name,
                GameRanks.Columns.GAME_RANK_FRIENDLY_NAME to friendlyName,
                GameRanks.Columns.GAME_RANK_VALUE to value,
                GameRanks.Columns.GAME_RANK_BAYES_AVERAGE to bayesAverage,
            )
            batch += if (existingRankIds.remove(id)) {
                ContentProviderOperation.newUpdate(Games.buildRanksUri(game.id, id)).withValues(values).build()
            } else {
                values.put(GameRanks.Columns.GAME_RANK_ID, id)
                ContentProviderOperation.newInsert(Games.buildRanksUri(game.id)).withValues(values).build()
            }
        }
        existingRankIds.mapTo(batch) { ContentProviderOperation.newDelete(GameRanks.buildGameRankUri(it)).build() }
    }

    private suspend fun createExpansionsBatch(
        gameId: Int,
        newLinks: List<Triple<Int, String, Boolean>>
    ): ArrayList<ContentProviderOperation> = withContext(Dispatchers.IO) {
        val batch = arrayListOf<ContentProviderOperation>()
        val pathUri = Games.buildPathUri(gameId, PATH_EXPANSIONS)
        val existingIds = resolver.queryInts(pathUri, GamesExpansions.Columns.EXPANSION_ID).toMutableList()

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
    }

    /**
     * Upsert each ID/name pair.
     */
    private fun saveReference(newLinks: List<Pair<Int, String>>, baseUri: Uri, idColumn: String, nameColumn: String) {
        val batch = arrayListOf<ContentProviderOperation>()
        for ((id, name) in newLinks) {
            val uri = baseUri.buildUpon().appendPath(id.toString()).build()
            batch += if (resolver.rowExists(uri)) {
                ContentProviderOperation.newUpdate(uri).withValue(nameColumn, name).build()
            } else {
                ContentProviderOperation.newInsert(baseUri).withValues(
                    contentValuesOf(
                        idColumn to id,
                        nameColumn to name,
                    )
                ).build()
            }
        }
        resolver.applyBatch(batch, "Saving ${baseUri.lastPathSegment}")
    }

    private fun createAssociationBatch(
        gameId: Int,
        newLinks: List<Pair<Int, String>>,
        uriPath: String,
        idColumn: String
    ): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        val associationUri = Games.buildPathUri(gameId, uriPath)
        val existingIds = resolver.queryInts(associationUri, idColumn).toMutableList()
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
