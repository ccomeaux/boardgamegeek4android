package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.Builder
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Color
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.io.BggService
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggDatabase.*
import com.boardgamegeek.util.DataUtils
import com.boardgamegeek.util.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameDao(private val context: BggApplication) {
    private val resolver: ContentResolver = context.contentResolver

    fun load(gameId: Int): LiveData<GameEntity> {
        if (gameId == INVALID_ID) return AbsentLiveData.create()
        val uri = Games.buildGameUri(gameId)
        return RegisteredLiveData(context, uri) {
            val projection = arrayOf(
                Games.GAME_ID,
                Games.STATS_AVERAGE,
                Games.YEAR_PUBLISHED,
                Games.MIN_PLAYERS,
                Games.MAX_PLAYERS,
                Games.PLAYING_TIME,
                Games.MINIMUM_AGE,
                Games.DESCRIPTION,
                Games.STATS_USERS_RATED,
                Games.UPDATED,
                Games.UPDATED_PLAYS,
                Games.GAME_RANK,
                Games.GAME_NAME,
                Games.THUMBNAIL_URL,
                Games.STATS_BAYES_AVERAGE,
                Games.STATS_MEDIAN,
                Games.STATS_STANDARD_DEVIATION,
                Games.STATS_NUMBER_WEIGHTS,
                Games.STATS_AVERAGE_WEIGHT,
                Games.STATS_NUMBER_OWNED,
                Games.STATS_NUMBER_TRADING,
                Games.STATS_NUMBER_WANTING,
                Games.STATS_NUMBER_WISHING,
                Games.IMAGE_URL,
                Games.SUBTYPE,
                Games.CUSTOM_PLAYER_SORT,
                Games.STATS_NUMBER_COMMENTS,
                Games.MIN_PLAYING_TIME,
                Games.MAX_PLAYING_TIME,
                Games.STARRED,
                Games.POLLS_COUNT,
                Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
                Games.HERO_IMAGE_URL,
                Games.ICON_COLOR,
                Games.DARK_COLOR,
                Games.WINS_COLOR,
                Games.WINNABLE_PLAYS_COLOR,
                Games.ALL_PLAYS_COLOR
            )
            context.contentResolver.load(uri, projection)?.use {
                if (it.moveToFirst()) {
                    return@RegisteredLiveData GameEntity(
                        id = it.getInt(Games.GAME_ID),
                        name = it.getStringOrEmpty(Games.GAME_NAME),
                        description = it.getStringOrEmpty(Games.DESCRIPTION),
                        subtype = it.getStringOrEmpty(Games.SUBTYPE),
                        thumbnailUrl = it.getStringOrEmpty(Games.THUMBNAIL_URL),
                        imageUrl = it.getStringOrEmpty(Games.IMAGE_URL),
                        yearPublished = it.getIntOrNull(Games.YEAR_PUBLISHED) ?: YEAR_UNKNOWN,
                        minPlayers = it.getIntOrZero(Games.MIN_PLAYERS),
                        maxPlayers = it.getIntOrZero(Games.MAX_PLAYERS),
                        playingTime = it.getIntOrZero(Games.PLAYING_TIME),
                        minPlayingTime = it.getIntOrZero(Games.MIN_PLAYING_TIME),
                        maxPlayingTime = it.getIntOrZero(Games.MAX_PLAYING_TIME),
                        minimumAge = it.getIntOrZero(Games.MINIMUM_AGE)
                    ).apply {
                        heroImageUrl = it.getStringOrEmpty(Games.HERO_IMAGE_URL)
                        rating = it.getDoubleOrZero(Games.STATS_AVERAGE)
                        numberOfRatings = it.getIntOrZero(Games.STATS_USERS_RATED)
                        numberOfComments = it.getIntOrZero(Games.STATS_NUMBER_COMMENTS)
                        updated = it.getLongOrZero(Games.UPDATED)
                        updatedPlays = it.getLongOrZero(Games.UPDATED_PLAYS)
                        overallRank = it.getIntOrNull(Games.GAME_RANK) ?: RANK_UNKNOWN
                        standardDeviation = it.getDoubleOrZero(Games.STATS_STANDARD_DEVIATION)
                        bayesAverage = it.getDoubleOrZero(Games.STATS_BAYES_AVERAGE)
                        averageWeight = it.getDoubleOrZero(Games.STATS_AVERAGE_WEIGHT)
                        numberOfUsersWeighting = it.getIntOrZero(Games.STATS_NUMBER_WEIGHTS)
                        numberOfUsersOwned = it.getIntOrZero(Games.STATS_NUMBER_OWNED)
                        numberOfUsersTrading = it.getIntOrZero(Games.STATS_NUMBER_TRADING)
                        numberOfUsersWanting = it.getIntOrZero(Games.STATS_NUMBER_WANTING)
                        numberOfUsersWishListing = it.getIntOrZero(Games.STATS_NUMBER_WISHING)
                        customPlayerSort = it.getBoolean(Games.CUSTOM_PLAYER_SORT)
                        isFavorite = it.getBoolean(Games.STARRED)
                        pollVoteTotal = it.getIntOrZero(Games.POLLS_COUNT)
                        suggestedPlayerCountPollVoteTotal =
                            it.getIntOrZero(Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL)
                        iconColor = it.getIntOrNull(Games.ICON_COLOR) ?: Color.TRANSPARENT
                        darkColor = it.getIntOrNull(Games.DARK_COLOR) ?: Color.TRANSPARENT
                        winsColor = it.getIntOrNull(Games.WINS_COLOR) ?: Color.TRANSPARENT
                        winnablePlaysColor = it.getIntOrNull(Games.WINNABLE_PLAYS_COLOR) ?: Color.TRANSPARENT
                        allPlaysColor = it.getIntOrNull(Games.ALL_PLAYS_COLOR) ?: Color.TRANSPARENT
                    }
                }
                return@RegisteredLiveData null
            }
        }
    }

    suspend fun loadRanks(gameId: Int): List<GameRankEntity> = withContext(Dispatchers.IO) {
        val ranks = arrayListOf<GameRankEntity>()
        if (gameId != INVALID_ID) {
            val uri = Games.buildRanksUri(gameId)
            context.contentResolver.load(uri)?.use {
                if (it.moveToFirst()) {
                    do {
                        ranks += GameRankEntity(
                            id = it.getIntOrNull(GameRanks.GAME_RANK_ID) ?: INVALID_ID,
                            type = it.getStringOrEmpty(GameRanks.GAME_RANK_TYPE),
                            name = it.getStringOrEmpty(GameRanks.GAME_RANK_NAME),
                            friendlyName = it.getStringOrEmpty(GameRanks.GAME_RANK_FRIENDLY_NAME),
                            value = it.getIntOrNull(GameRanks.GAME_RANK_VALUE) ?: RANK_UNKNOWN,
                            bayesAverage = it.getDoubleOrZero(GameRanks.GAME_RANK_BAYES_AVERAGE),
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
                GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL,
                GamePollResultsResult.POLL_RESULTS_RESULT_VALUE,
                GamePollResultsResult.POLL_RESULTS_RESULT_VOTES,
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
                GameSuggestedPlayerCountPollPollResults.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
                GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT,
                GameSuggestedPlayerCountPollPollResults.BEST_VOTE_COUNT,
                GameSuggestedPlayerCountPollPollResults.RECOMMENDED_VOTE_COUNT,
                GameSuggestedPlayerCountPollPollResults.NOT_RECOMMENDED_VOTE_COUNT,
                GameSuggestedPlayerCountPollPollResults.RECOMMENDATION, // why is this not loading into the entity
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
                    Designers.DESIGNER_ID,
                    Designers.DESIGNER_NAME,
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
                    Artists.ARTIST_ID,
                    Artists.ARTIST_NAME,
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
                    Publishers.PUBLISHER_ID,
                    Publishers.PUBLISHER_NAME,
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
                    Categories.CATEGORY_ID,
                    Categories.CATEGORY_NAME,
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
                    Mechanics.MECHANIC_ID,
                    Mechanics.MECHANIC_NAME,
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
                        GamesExpansions.EXPANSION_ID,
                        GamesExpansions.EXPANSION_NAME,
                    ),
                    selection = "${GamesExpansions.INBOUND}=?",
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
                            Collection.STATUS_OWN,
                            Collection.STATUS_PREVIOUSLY_OWNED,
                            Collection.STATUS_PREORDERED,
                            Collection.STATUS_FOR_TRADE,
                            Collection.STATUS_WANT,
                            Collection.STATUS_WANT_TO_PLAY,
                            Collection.STATUS_WANT_TO_BUY,
                            Collection.STATUS_WISHLIST,
                            Collection.STATUS_WISHLIST_PRIORITY,
                            Collection.NUM_PLAYS,
                            Collection.RATING,
                            Collection.COMMENT
                        ),
                        selection = "collection.${Collection.GAME_ID}=?",
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
                                    wishListPriority = it.getIntOrNull(8) ?: WISHLIST_PRIORITY_UNKNOWN,
                                    numberOfPlays = it.getIntOrNull(9) ?: 0,
                                    rating = it.getDoubleOrNull(10) ?: 0.0,
                                    comment = it.getStringOrNull(11).orEmpty(),
                                )
                            } while (it.moveToNext())
                        } else {
                            results+= result
                        }
                    }
                }
            }
            results
        }

    suspend fun loadPlayColors(gameId: Int): List<String> = withContext(Dispatchers.IO) {
        val results = arrayListOf<String>()
        if (gameId != INVALID_ID) {
            context.contentResolver.load(
                Games.buildColorsUri(gameId),
                arrayOf(GameColors.COLOR),
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        results.add(it.getString(0))
                    } while (it.moveToNext())
                }
            }
        }
        results
    }

    suspend fun loadColors(gameId: Int): List<String> = withContext(Dispatchers.IO) {
        val colors = mutableListOf<String>()
        context.contentResolver.load(
            Games.buildColorsUri(gameId),
            arrayOf(GameColors.COLOR)
        )?.use {
            if (it.moveToFirst()) {
                do {
                    colors += it.getStringOrNull(0).orEmpty()
                } while (it.moveToNext())
            }
        }
        colors
    }

    fun loadPlayInfo(
        includeIncompletePlays: Boolean,
        includeExpansions: Boolean,
        includeAccessories: Boolean
    ): List<GameForPlayStatEntity> {
        val results = arrayListOf<GameForPlayStatEntity>()
        context.contentResolver.load(
            Games.CONTENT_PLAYS_URI,
            arrayOf(
                Plays.SUM_QUANTITY,
                Plays.ITEM_NAME,
                Games.GAME_RANK,
                Games.GAME_ID
            ),
            selection = arrayListOf<String>().apply {
                add(Plays.DELETE_TIMESTAMP.whereZeroOrNull())
                if (!includeIncompletePlays) {
                    add(Plays.INCOMPLETE.whereZeroOrNull())
                }
                if (!includeExpansions && !includeAccessories) {
                    add(Games.SUBTYPE.whereEqualsOrNull())
                } else if (!includeExpansions || !includeAccessories) {
                    add(Games.SUBTYPE.whereNotEqualsOrNull())
                }
            }.joinTo(" AND ").toString(),
            selectionArgs = arrayListOf<String>().apply {
                if (!includeExpansions && !includeAccessories) {
                    add(BggService.THING_SUBTYPE_BOARDGAME)
                } else if (!includeExpansions) {
                    add(BggService.THING_SUBTYPE_BOARDGAME_EXPANSION)
                } else if (!includeAccessories) {
                    add(BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY)
                }
            }.toTypedArray(),
            sortOrder = "${Plays.SUM_QUANTITY} DESC, ${Games.GAME_SORT_NAME} ASC"
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += GameForPlayStatEntity(
                        it.getIntOrNull(3) ?: INVALID_ID,
                        it.getStringOrNull(1).orEmpty(),
                        it.getIntOrNull(0) ?: 0,
                        it.getIntOrNull(2) ?: 0,
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadPlayInfoAsLiveData(
        includeIncompletePlays: Boolean,
        includeExpansions: Boolean,
        includeAccessories: Boolean
    ): LiveData<List<GameForPlayStatEntity>> {
        return RegisteredLiveData(context, Games.CONTENT_PLAYS_URI) {
            return@RegisteredLiveData loadPlayInfo(includeIncompletePlays, includeExpansions, includeAccessories)
        }
    }

    fun delete(gameId: Int): Int {
        if (gameId == INVALID_ID) return 0
        return resolver.delete(Games.buildGameUri(gameId), null, null)
    }

    fun insertColors(gameId: Int, color: String) {
        val values = contentValuesOf(GameColors.COLOR to color)
        resolver.insert(Games.buildColorsUri(gameId), values)
    }

    fun deleteColor(gameId: Int, color: String): Int {
        return resolver.delete(Games.buildColorsUri(gameId, color), null, null)
    }

    fun computeColors(gameId: Int): Int {
        val values = mutableListOf<ContentValues>()
        val cursor = resolver.query(
            Plays.buildPlayersByColor(),
            arrayOf(PlayPlayers.COLOR),
            "${Plays.OBJECT_ID}=?",
            arrayOf(gameId.toString()),
            null
        )
        cursor?.use { c ->
            if (c.moveToFirst()) {
                do {
                    val color = c.getString(0).orEmpty()
                    if (color.isNotBlank()) {
                        values.add(contentValuesOf(GameColors.COLOR to color))
                    }
                } while (c.moveToNext())
            }
        }
        return if (values.size > 0) {
            resolver.bulkInsert(Games.buildColorsUri(gameId), values.toTypedArray())
        } else 0

    }

    fun update(gameId: Int, values: ContentValues): Int {
        return resolver.update(Games.buildGameUri(gameId), values, null, null)
    }

    fun save(game: GameEntity, updateTime: Long) {
        // TODO return the internal ID
        if (game.name.isBlank()) {
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
        batch.addAll(createPollsBatch(game))
        batch.addAll(createPlayerPollBatch(game.id, game.playerPoll))
        batch.addAll(createExpansionsBatch(game.id, game.expansions))

        saveReference(game.designers, Designers.CONTENT_URI, Designers.DESIGNER_ID, Designers.DESIGNER_NAME)
        saveReference(game.artists, Artists.CONTENT_URI, Artists.ARTIST_ID, Artists.ARTIST_NAME)
        saveReference(game.publishers, Publishers.CONTENT_URI, Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME)
        saveReference(game.categories, Categories.CONTENT_URI, Categories.CATEGORY_ID, Categories.CATEGORY_NAME)
        saveReference(game.mechanics, Mechanics.CONTENT_URI, Mechanics.MECHANIC_ID, Mechanics.MECHANIC_NAME)

        batch.addAll(createAssociationBatch(game.id, game.designers, PATH_DESIGNERS, GamesDesigners.DESIGNER_ID))
        batch.addAll(createAssociationBatch(game.id, game.artists, PATH_ARTISTS, GamesArtists.ARTIST_ID))
        batch.addAll(createAssociationBatch(game.id, game.publishers, PATH_PUBLISHERS, GamesPublishers.PUBLISHER_ID))
        batch.addAll(createAssociationBatch(game.id, game.categories, PATH_CATEGORIES, GamesCategories.CATEGORY_ID))
        batch.addAll(createAssociationBatch(game.id, game.mechanics, PATH_MECHANICS, GamesMechanics.MECHANIC_ID))

        try {
            resolver.applyBatch(batch, "Game ${game.id}")
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
        values.put(Games.MINIMUM_AGE, game.minimumAge)
        if (game.hasStatistics) {
            values.put(Games.STATS_AVERAGE, game.rating)
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
        game.playerPoll?.let {
            values.put(Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL, it.totalVotes)
            val separator = "|"
            values.put(
                Games.PLAYER_COUNTS_BEST,
                it.bestCounts.joinToString(separator, prefix = separator, postfix = separator)
            )
            values.put(
                Games.PLAYER_COUNTS_RECOMMENDED,
                it.recommendedAndBestCounts.joinToString(separator, prefix = separator, postfix = separator)
            )
            values.put(
                Games.PLAYER_COUNTS_NOT_RECOMMENDED,
                it.notRecommendedCounts.joinToString(separator, prefix = separator, postfix = separator)
            )
        }
        return values
    }

    private fun shouldClearHeroImageUrl(game: GameEntity): Boolean {
        val cursor =
            resolver.query(Games.buildGameUri(game.id), arrayOf(Games.IMAGE_URL, Games.THUMBNAIL_URL), null, null, null)
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val imageUrl = c.getStringOrNull(0) ?: ""
                val thumbnailUrl = c.getStringOrNull(1) ?: ""
                if (imageUrl != game.imageUrl || thumbnailUrl != game.thumbnailUrl) {
                    return true
                }
            }
        }
        return false
    }

    private fun createPollsBatch(game: GameEntity): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        val existingPollNames =
            resolver.queryStrings(Games.buildPollsUri(game.id), GamePolls.POLL_NAME).filterNotNull().toMutableList()
        for (poll in game.polls) {
            val values = ContentValues()
            values.put(GamePolls.POLL_TITLE, poll.title)
            values.put(GamePolls.POLL_TOTAL_VOTES, poll.totalVotes)

            var existingResultKeys = mutableListOf<String>()
            if (existingPollNames.remove(poll.name)) {
                batch.add(
                    ContentProviderOperation.newUpdate(Games.buildPollsUri(game.id, poll.name)).withValues(values)
                        .build()
                )
                existingResultKeys = resolver.queryStrings(
                    Games.buildPollResultsUri(game.id, poll.name),
                    GamePollResults.POLL_RESULTS_PLAYERS
                ).filterNotNull().toMutableList()
            } else {
                values.put(GamePolls.POLL_NAME, poll.name)
                batch.add(ContentProviderOperation.newInsert(Games.buildPollsUri(game.id)).withValues(values).build())
            }

            for ((resultsIndex, results) in poll.results.withIndex()) {
                values.clear()
                values.put(GamePollResults.POLL_RESULTS_SORT_INDEX, resultsIndex + 1)

                var existingValues = mutableListOf<String>()
                if (existingResultKeys.remove(results.key)) {
                    batch.add(
                        ContentProviderOperation
                            .newUpdate(Games.buildPollResultsUri(game.id, poll.name, results.key))
                            .withValues(values).build()
                    )
                    existingValues = resolver.queryStrings(
                        Games.buildPollResultsResultUri(game.id, poll.name, results.key),
                        GamePollResultsResult.POLL_RESULTS_RESULT_KEY
                    )
                        .filterNotNull()
                        .toMutableList()
                } else {
                    values.put(GamePollResults.POLL_RESULTS_PLAYERS, results.key)
                    batch.add(
                        ContentProviderOperation.newInsert(Games.buildPollResultsUri(game.id, poll.name))
                            .withValues(values).build()
                    )
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
                        batch.add(
                            ContentProviderOperation.newUpdate(
                                Games.buildPollResultsResultUri(
                                    game.id,
                                    poll.name,
                                    results.key,
                                    key
                                )
                            )
                                .withValues(values)
                                .build()
                        )
                    } else {
                        batch.add(
                            ContentProviderOperation
                                .newInsert(Games.buildPollResultsResultUri(game.id, poll.name, results.key))
                                .withValues(values)
                                .build()
                        )
                    }
                }

                for (value in existingValues) {
                    batch.add(
                        ContentProviderOperation.newDelete(
                            Games.buildPollResultsResultUri(
                                game.id,
                                poll.name,
                                results.key,
                                value
                            )
                        ).build()
                    )
                }
            }

            for (player in existingResultKeys) {
                batch.add(
                    ContentProviderOperation.newDelete(Games.buildPollResultsUri(game.id, poll.name, player)).build()
                )
            }
        }
        for (pollName in existingPollNames) {
            batch.add(ContentProviderOperation.newDelete(Games.buildPollsUri(game.id, pollName)).build())
        }
        return batch
    }

    private fun createPlayerPollBatch(gameId: Int, poll: GamePlayerPollEntity?): ArrayList<ContentProviderOperation> {
        if (poll == null) return ArrayList()
        val batch = arrayListOf<ContentProviderOperation>()
        val existingResults = resolver.queryStrings(
            Games.buildSuggestedPlayerCountPollResultsUri(gameId),
            GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT
        ).filterNotNull().toMutableList()
        for ((sortIndex, results) in poll.results.withIndex()) {
            val values = contentValuesOf(
                GameSuggestedPlayerCountPollPollResults.SORT_INDEX to sortIndex + 1,
                GameSuggestedPlayerCountPollPollResults.BEST_VOTE_COUNT to results.bestVoteCount,
                GameSuggestedPlayerCountPollPollResults.RECOMMENDED_VOTE_COUNT to results.recommendedVoteCount,
                GameSuggestedPlayerCountPollPollResults.NOT_RECOMMENDED_VOTE_COUNT to results.notRecommendedVoteCount,
                GameSuggestedPlayerCountPollPollResults.RECOMMENDATION to results.calculatedRecommendation,
            )
            if (existingResults.remove(results.playerCount)) {
                val uri = Games.buildSuggestedPlayerCountPollResultsUri(gameId, results.playerCount)
                batch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build())
            } else {
                values.put(GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT, results.playerCount)
                val uri = Games.buildSuggestedPlayerCountPollResultsUri(gameId)
                batch.add(ContentProviderOperation.newInsert(uri).withValues(values).build())
            }
        }
        for (result in existingResults) {
            val uri = Games.buildSuggestedPlayerCountPollResultsUri(gameId, result)
            batch.add(ContentProviderOperation.newDelete(uri).build())
        }
        return batch
    }

    private fun createRanksBatch(game: GameEntity): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        val existingRankIds = resolver.queryInts(
            GameRanks.CONTENT_URI,
            GameRanks.GAME_RANK_ID,
            "${GameRanks.GAME_RANK_ID}=?",
            arrayOf(game.id.toString())
        ).toMutableList()
        for ((id, type, name, friendlyName, value, bayesAverage) in game.ranks) {
            val values = ContentValues()
            values.put(GameRanks.GAME_RANK_TYPE, type)
            values.put(GameRanks.GAME_RANK_NAME, name)
            values.put(GameRanks.GAME_RANK_FRIENDLY_NAME, friendlyName)
            values.put(GameRanks.GAME_RANK_VALUE, value)
            values.put(GameRanks.GAME_RANK_BAYES_AVERAGE, bayesAverage)

            if (existingRankIds.remove(id)) {
                batch.add(
                    ContentProviderOperation.newUpdate(Games.buildRanksUri(game.id, id)).withValues(values).build()
                )
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

    private fun createExpansionsBatch(
        gameId: Int,
        newLinks: List<Triple<Int, String, Boolean>>
    ): ArrayList<ContentProviderOperation> {
        val batch = arrayListOf<ContentProviderOperation>()
        val pathUri = Games.buildPathUri(gameId, PATH_EXPANSIONS)
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
            batch.add(
                ContentProviderOperation.newDelete(Games.buildPathUri(gameId, PATH_EXPANSIONS, existingId)).build()
            )
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
