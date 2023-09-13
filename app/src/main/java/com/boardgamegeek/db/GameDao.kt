package com.boardgamegeek.db

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import android.text.format.DateUtils
import androidx.core.content.contentValuesOf
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.*
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
import com.boardgamegeek.provider.BggDatabase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameDao(private val context: Context) {
    suspend fun load(gameId: Int): GameLocal? = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            context.contentResolver.loadEntity(
                Games.buildGameUri(gameId),
                arrayOf(
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
                    BaseColumns._ID, // 30
                    Games.Columns.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL,
                    Games.Columns.ICON_COLOR,
                    Games.Columns.DARK_COLOR,
                    Games.Columns.WINS_COLOR,
                    Games.Columns.WINNABLE_PLAYS_COLOR, // 35
                    Games.Columns.ALL_PLAYS_COLOR,
                    Games.Columns.GAME_SORT_NAME,
                    Games.Columns.STATS_MEDIAN,
                    Games.Columns.NUM_PLAYS,
                    Games.Columns.UPDATED_LIST, //40
                    Games.Columns.LAST_VIEWED,
                    Games.Columns.PLAYER_COUNTS_BEST,
                    Games.Columns.PLAYER_COUNTS_RECOMMENDED,
                    Games.Columns.PLAYER_COUNTS_NOT_RECOMMENDED,
                    Plays.Columns.MAX_DATE, //45
                )
            ) {
                GameLocal(
                    internalId = it.getLong(30),
                    gameId = it.getInt(0),
                    gameName = it.getString(1).orEmpty(),
                    description = it.getStringOrNull(2).orEmpty(),
                    subtype = it.getStringOrNull(3),
                    thumbnailUrl = it.getStringOrNull(4),
                    imageUrl = it.getStringOrNull(5),
                    yearPublished = it.getIntOrNull(6),
                    minPlayers = it.getIntOrNull(7) ?: 0,
                    maxPlayers = it.getIntOrNull(8) ?: 0,
                    playingTime = it.getIntOrNull(9) ?: 0,
                    minPlayingTime = it.getIntOrNull(10),
                    maxPlayingTime = it.getIntOrNull(11),
                    minimumAge = it.getIntOrNull(12),
                    heroImageUrl = it.getStringOrNull(13),
                    average = it.getDoubleOrNull(14),
                    numberOfRatings = it.getIntOrNull(15),
                    numberOfComments = it.getIntOrNull(16),
                    gameRank = it.getIntOrNull(19),
                    standardDeviation = it.getDoubleOrNull(20),
                    bayesAverage = it.getDoubleOrNull(21),
                    averageWeight = it.getDoubleOrNull(22),
                    numberOfUsersWeighting = it.getIntOrNull(23),
                    numberOfUsersOwned = it.getIntOrNull(24),
                    numberOfUsersTrading = it.getIntOrNull(25),
                    numberOfUsersWanting = it.getIntOrNull(26),
                    numberOfUsersWishListing = it.getIntOrNull(27),
                    updated = it.getLongOrNull(17),
                    updatedPlays = it.getLongOrNull(18),
                    customPlayerSort = it.getBoolean(28),
                    isStarred = it.getBoolean(29),
                    suggestedPlayerCountPollVoteTotal = it.getIntOrNull(31) ?: 0,
                    iconColor = it.getIntOrNull(32),
                    darkColor = it.getIntOrNull(33),
                    winsColor = it.getIntOrNull(34),
                    winnablePlaysColor = it.getIntOrNull(35),
                    allPlaysColor = it.getIntOrNull(36),
                    gameSortName = it.getString(37),
                    median = it.getDoubleOrNull(38),
                    numberOfPlays = it.getIntOrNull(39),
                    updatedList = it.getLong(40),
                    lastViewedTimestamp = it.getLongOrNull(41),
                    playerCountsBest = it.getStringOrNull(42),
                    playerCountsRecommended = it.getStringOrNull(43),
                    playerCountsNotRecommended = it.getStringOrNull(44),
                    lastPlayDate = it.getStringOrNull(45),
                )
            }
        } else null
    }

    suspend fun loadRanks(gameId: Int): List<GameRankLocal> = withContext(Dispatchers.IO) {
        if (gameId == INVALID_ID) return@withContext emptyList()
        val uri = Games.buildRanksUri(gameId)
        context.contentResolver.loadList(
            uri,
            arrayOf(
                GameRanks.Columns.GAME_RANK_ID,
                GameRanks.Columns.GAME_RANK_TYPE,
                GameRanks.Columns.GAME_RANK_NAME,
                GameRanks.Columns.GAME_RANK_FRIENDLY_NAME,
                GameRanks.Columns.GAME_RANK_VALUE,
                GameRanks.Columns.GAME_RANK_BAYES_AVERAGE,
                GameRanks.Columns.GAME_ID,
                BaseColumns._ID,
            )
        ) {
            GameRankLocal(
                internalId = it.getLong(7),
                gameId = it.getInt(6),
                gameRankId = it.getIntOrNull(0) ?: INVALID_ID,
                gameRankType = it.getStringOrNull(1).orEmpty(),
                gameRankName = it.getStringOrNull(2).orEmpty(),
                gameFriendlyRankName = it.getStringOrNull(3).orEmpty(),
                gameRankValue = it.getIntOrNull(4) ?: GameRankLocal.RANK_UNKNOWN,
                gameRankBayesAverage = it.getDoubleOrNull(5) ?: 0.0,
            )
        }
    }

    enum class PollType(val code: String) {
        LANGUAGE_DEPENDENCE("language_dependence"),

        @Suppress("SpellCheckingInspection")
        SUGGESTED_PLAYER_AGE("suggested_playerage"),
    }

    suspend fun loadPoll(gameId: Int, pollType: PollType): List<GamePollResultsResultLocal> = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            context.contentResolver.loadList(
                Games.buildPollResultsResultUri(gameId, pollType.code),
                arrayOf(
                    BaseColumns._ID,
                    GamePollResultsResult.Columns.POLL_RESULTS_ID,
                    GamePollResultsResult.Columns.POLL_RESULTS_RESULT_LEVEL,
                    GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VALUE,
                    GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VOTES,
                    GamePollResultsResult.Columns.POLL_RESULTS_RESULT_SORT_INDEX,
                )
            ) {
                GamePollResultsResultLocal(
                    internalId = it.getLong(0),
                    pollResultsId = it.getInt(1),
                    pollResultsResultLevel = it.getInt(2),
                    pollResultsResultValue = it.getString(3),
                    pollResultsResultVotes = it.getInt(4),
                    pollResultsResulSortIndex = it.getInt(5),
                )
            }
        } else emptyList()
    }

    suspend fun loadPlayerPoll(gameId: Int): List<GameSuggestedPlayerCountPollResultsLocal> = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            context.contentResolver.loadList(
                Games.buildSuggestedPlayerCountPollResultsUri(gameId),
                arrayOf(
                    GameSuggestedPlayerCountPollPollResults.Columns.GAME_ID,
                    GameSuggestedPlayerCountPollPollResults.Columns.PLAYER_COUNT,
                    GameSuggestedPlayerCountPollPollResults.Columns.BEST_VOTE_COUNT,
                    GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDED_VOTE_COUNT,
                    GameSuggestedPlayerCountPollPollResults.Columns.NOT_RECOMMENDED_VOTE_COUNT,
                    BaseColumns._ID,
                    GameSuggestedPlayerCountPollPollResults.Columns.SORT_INDEX,
                )
            ) {
                GameSuggestedPlayerCountPollResultsLocal(
                    internalId = it.getLong(5),
                    gameId = it.getInt(0),
                    playerCount = it.getStringOrNull(1).orEmpty(),
                    sortIndex = it.getInt(6),
                    bestVoteCount = it.getIntOrNull(2) ?: 0,
                    recommendedVoteCount = it.getIntOrNull(3) ?: 0,
                    notRecommendedVoteCount = it.getIntOrNull(4) ?: 0,
                )
            }
        } else emptyList()
    }

    suspend fun loadOldestUpdatedGames(gamesPerFetch: Int = 0): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        val games = mutableListOf<Pair<Int, String>>()
        val limit = if (gamesPerFetch > 0) " LIMIT $gamesPerFetch" else ""
        context.contentResolver.loadList(
            Games.CONTENT_URI,
            arrayOf(Games.Columns.GAME_ID, Games.Columns.GAME_NAME),
            "${Tables.GAMES}.${Games.Columns.UPDATED}".whereNotZeroOrNull(),
            null,
            "${Tables.GAMES}.${Games.Columns.UPDATED_LIST}$limit"
        ) {
            games += it.getInt(0) to it.getString(1)
        }
        games
    }

    suspend fun loadUnupdatedGames(gamesPerFetch: Int = 0): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        val games = mutableListOf<Pair<Int, String>>()
        val limit = if (gamesPerFetch > 0) " LIMIT $gamesPerFetch" else ""
        context.contentResolver.loadList(
            Games.CONTENT_URI,
            arrayOf(Games.Columns.GAME_ID, Games.Columns.GAME_NAME),
            "${Tables.GAMES}.${Games.Columns.UPDATED}".whereZeroOrNull(),
            null,
            "${Tables.GAMES}.${Games.Columns.UPDATED_LIST}$limit",
        ) {
            games += it.getInt(0) to it.getString(1)
        }
        games
    }


    /**
     * Get a list of games, sorted by least recently updated, that
     * 1. have no associated collection record
     * 2. haven't been viewed in a configurable number of hours
     * 3. and have 0 plays (if plays are being synced)
     */
    suspend fun loadDeletableGames(hoursAgo: Long, includeUnplayedGames: Boolean): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        val games = mutableListOf<Pair<Int, String>>()
        var selection = "${Tables.COLLECTION}.${Collection.Columns.GAME_ID} IS NULL AND ${Tables.GAMES}.${Games.Columns.LAST_VIEWED}<?"
        if (includeUnplayedGames) {
            selection += " AND ${Tables.GAMES}.${Games.Columns.NUM_PLAYS}=0"
        }
        context.contentResolver.loadList(
            Games.CONTENT_URI,
            arrayOf(Games.Columns.GAME_ID, Games.Columns.GAME_NAME),
            selection,
            arrayOf(hoursAgo.toString()),
            "${Tables.GAMES}.${Games.Columns.UPDATED}"
        ) {
            games += it.getInt(0) to it.getString(1)
        }
        games
    }

    suspend fun loadDesigners(gameId: Int): List<DesignerBrief> = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            context.contentResolver.loadList(
                Games.buildDesignersUri(gameId),
                arrayOf(
                    BaseColumns._ID,
                    Designers.Columns.DESIGNER_ID,
                    Designers.Columns.DESIGNER_NAME,
                    Designers.Columns.DESIGNER_THUMBNAIL_URL,
                )
            ) {
                DesignerBrief(
                    it.getLong(0),
                    it.getInt(1),
                    it.getString(2),
                    it.getStringOrNull(3),
                )
            }
        } else emptyList()
    }

    suspend fun loadArtists(gameId: Int): List<ArtistBrief> = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            context.contentResolver.loadList(
                Games.buildArtistsUri(gameId),
                arrayOf(
                     BaseColumns._ID,
                    Artists.Columns.ARTIST_ID,
                    Artists.Columns.ARTIST_NAME,
                    Artists.Columns.ARTIST_THUMBNAIL_URL,
                )
            ) {
                ArtistBrief(
                    it.getLong(0),
                    it.getInt(1),
                    it.getString(2),
                    it.getStringOrNull(3),
                )
            }
        } else emptyList()
    }

    suspend fun loadPublishers(gameId: Int): List<PublisherBrief> = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            context.contentResolver.loadList(
                Games.buildPublishersUri(gameId),
                arrayOf(
                    BaseColumns._ID,
                    Publishers.Columns.PUBLISHER_ID,
                    Publishers.Columns.PUBLISHER_NAME,
                    Publishers.Columns.PUBLISHER_THUMBNAIL_URL,
                )
            ) {
                PublisherBrief(
                    it.getLong(0),
                    it.getInt(1),
                    it.getString(2),
                    it.getString(3),
                )
            }
        } else emptyList()
    }

    suspend fun loadCategories(gameId: Int): List<CategoryLocal> = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            context.contentResolver.loadList(
                Games.buildCategoriesUri(gameId),
                arrayOf(
                    BaseColumns._ID,
                    Categories.Columns.CATEGORY_ID,
                    Categories.Columns.CATEGORY_NAME,
                )
            ) {
                CategoryLocal(
                    it.getInt(0),
                    it.getInt(1),
                    it.getString(2),
                )
            }
        } else emptyList()
    }

    suspend fun loadMechanics(gameId: Int): List<MechanicLocal> = withContext(Dispatchers.IO) {
        if (gameId != INVALID_ID) {
            context.contentResolver.loadList(
                Games.buildMechanicsUri(gameId),
                arrayOf(
                    BaseColumns._ID,
                    Mechanics.Columns.MECHANIC_ID,
                    Mechanics.Columns.MECHANIC_NAME,
                )
            ) {
                MechanicLocal(
                    it.getInt(0),
                    it.getInt(1),
                    it.getString(2),
                )
            }
        } else emptyList()
    }

    suspend fun loadExpansions(gameId: Int, inbound: Boolean = false): List<GamesExpansionLocal> =
        withContext(Dispatchers.IO) {
            if (gameId != INVALID_ID) {
                val briefResults = context.contentResolver.loadList(
                    Games.buildExpansionsUri(gameId),
                    arrayOf(
                        BaseColumns._ID,
                        GamesExpansions.Columns.GAME_ID,
                        GamesExpansions.Columns.EXPANSION_ID,
                        GamesExpansions.Columns.EXPANSION_NAME,
                        Games.Columns.THUMBNAIL_URL,
                    ),
                    selection = "${GamesExpansions.Columns.INBOUND}=?",
                    selectionArgs = arrayOf(if (inbound) "1" else "0")
                ) {
                    GamesExpansionLocal(
                        it.getLong(0),
                        it.getInt(1),
                        it.getInt(2),
                        it.getString(3),
                        it.getBooleanOrNull(4),
                    )
                }
                briefResults
            } else emptyList()
        }

    suspend fun loadPlayColors(gameId: Int): List<String> = withContext(Dispatchers.IO) {
        context.contentResolver.queryStrings(Games.buildColorsUri(gameId), GameColors.Columns.COLOR).filterNot { it.isBlank() }
    }

    suspend fun loadPlayColors(): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        context.contentResolver.loadList(
            GameColors.CONTENT_URI,
            arrayOf(GameColors.Columns.GAME_ID, GameColors.Columns.COLOR),
        ) {
            (it.getIntOrNull(0) ?: INVALID_ID) to it.getStringOrNull(1).orEmpty()
        }
    }

    suspend fun loadGamesForPlayStats(
        includeIncompletePlays: Boolean,
        includeExpansions: Boolean,
        includeAccessories: Boolean
    ): List<GameForPlayStatEntity> = withContext(Dispatchers.IO) {
        context.contentResolver.loadList(
            Games.CONTENT_PLAYS_URI,
            arrayOf(
                Games.Columns.GAME_ID,
                Games.Columns.GAME_NAME,
                Games.Columns.GAME_RANK,
                Plays.Columns.SUM_QUANTITY,
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
        ) {
            GameForPlayStatEntity(
                id = it.getIntOrNull(0) ?: INVALID_ID,
                name = it.getStringOrNull(1).orEmpty(),
                playCount = it.getIntOrNull(3) ?: 0,
                bggRank = it.getIntOrNull(2) ?: GameRankEntity.RANK_UNKNOWN,
            )
        }
    }

    suspend fun delete(gameId: Int): Int = withContext(Dispatchers.IO) {
        if (gameId == INVALID_ID) 0
        else context.contentResolver.delete(Games.buildGameUri(gameId), null, null)
    }

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Games.CONTENT_URI, null, null)
    }

    suspend fun insertColor(gameId: Int, color: String) = withContext(Dispatchers.IO) {
        context.contentResolver.insert(Games.buildColorsUri(gameId), contentValuesOf(GameColors.Columns.COLOR to color))
    }

    suspend fun deleteColor(gameId: Int, color: String): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Games.buildColorsUri(gameId, color), null, null)
    }

    suspend fun insertColors(gameId: Int, colors: List<String>): Int = withContext(Dispatchers.IO) {
        if (colors.isEmpty()) 0
        else {
            val values = colors.map { contentValuesOf(GameColors.Columns.COLOR to it) }
            context.contentResolver.bulkInsert(Games.buildColorsUri(gameId), values.toTypedArray())
        }
    }

    suspend fun save(game: GameForUpsert) = withContext(Dispatchers.IO) {
        if (game.gameName.isBlank()) {
            Timber.w("Missing name from game ID=${game.gameId}")
        } else {
            Timber.i("Saving game $game")

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

            saveReference(game.designers, Designers.CONTENT_URI, Designers.Columns.DESIGNER_ID, Designers.Columns.DESIGNER_NAME)
            saveReference(game.artists, Artists.CONTENT_URI, Artists.Columns.ARTIST_ID, Artists.Columns.ARTIST_NAME)
            saveReference(game.publishers, Publishers.CONTENT_URI, Publishers.Columns.PUBLISHER_ID, Publishers.Columns.PUBLISHER_NAME)
            saveReference(game.categories, Categories.CONTENT_URI, Categories.Columns.CATEGORY_ID, Categories.Columns.CATEGORY_NAME)
            saveReference(game.mechanics, Mechanics.CONTENT_URI, Mechanics.Columns.MECHANIC_ID, Mechanics.Columns.MECHANIC_NAME)

            batch += createAssociationBatch(game.gameId, game.designers, PATH_DESIGNERS, GamesDesigners.DESIGNER_ID)
            batch += createAssociationBatch(game.gameId, game.artists, PATH_ARTISTS, GamesArtists.ARTIST_ID)
            batch += createAssociationBatch(game.gameId, game.publishers, PATH_PUBLISHERS, GamesPublishers.PUBLISHER_ID)
            batch += createAssociationBatch(game.gameId, game.categories, PATH_CATEGORIES, GamesCategories.CATEGORY_ID)
            batch += createAssociationBatch(game.gameId, game.mechanics, PATH_MECHANICS, GamesMechanics.MECHANIC_ID)

            context.contentResolver.applyBatch(batch, "Game $game")
            val dateTime = DateUtils.formatDateTime(context, game.updated ?: System.currentTimeMillis(), DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME)
            if (internalId == INVALID_ID.toLong()) {
                Timber.i("Inserted game $game at $dateTime")
            } else {
                Timber.i("Updated game $game (${internalId}) at $dateTime")
            }
        }
    }

    suspend fun resetPlaySync(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.update(Games.CONTENT_URI, contentValuesOf(Games.Columns.UPDATED_PLAYS to 0), null, null)
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
            Games.Columns.GAME_RANK to game.overallRank,
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
                "${GameRanks.Columns.GAME_RANK_ID}=?",
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

    /**
     * Upsert each ID/name pair.
     */
    private fun saveReference(newLinks: List<Pair<Int, String>>?, baseUri: Uri, idColumn: String, nameColumn: String) {
        if (newLinks == null) return
        val batch = arrayListOf<ContentProviderOperation>()
        for ((id, name) in newLinks) {
            val uri = baseUri.buildUpon().appendPath(id.toString()).build()
            batch += if (context.contentResolver.rowExists(uri)) {
                ContentProviderOperation
                    .newUpdate(uri)
                    .withValue(nameColumn, name)
                    .build()
            } else {
                ContentProviderOperation
                    .newInsert(baseUri)
                    .withValues(contentValuesOf(idColumn to id, nameColumn to name))
                    .build()
            }
        }
        context.contentResolver.applyBatch(batch, "Saving ${baseUri.lastPathSegment}")
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

    suspend fun updateLastViewed(gameId: Int, lastViewed: Long) {
        update(gameId, contentValuesOf(Games.Columns.LAST_VIEWED to lastViewed))
    }

    suspend fun updateHeroUrl(gameId: Int, url: String) {
        update(gameId, contentValuesOf(Games.Columns.HERO_IMAGE_URL to url))
    }

    suspend fun updateStarred(gameId: Int, isStarred: Boolean) {
        update(gameId, contentValuesOf(Games.Columns.STARRED to isStarred))
    }

    suspend fun updateGameColors(
        gameId: Int,
        iconColor: Int,
        darkColor: Int,
        winsColor: Int,
        winnablePlaysColor: Int,
        allPlaysColor: Int
    ): Int {
        val values = contentValuesOf(
            Games.Columns.ICON_COLOR to iconColor,
            Games.Columns.DARK_COLOR to darkColor,
            Games.Columns.WINS_COLOR to winsColor,
            Games.Columns.WINNABLE_PLAYS_COLOR to winnablePlaysColor,
            Games.Columns.ALL_PLAYS_COLOR to allPlaysColor,
        )
        return update(gameId, values)
    }

    suspend fun updateGamePlayCount(gameId: Int, playCount: Int) = withContext(Dispatchers.Default) {
        update(gameId, contentValuesOf(Games.Columns.NUM_PLAYS to playCount))
    }

    suspend fun updatePlaysSyncedTimestamp(gameId: Int, timestamp: Long) {
        update(gameId, contentValuesOf(Games.Columns.UPDATED_PLAYS to timestamp))
    }

    private suspend fun update(gameId: Int, values: ContentValues) = withContext(Dispatchers.IO) {
        context.contentResolver.update(Games.buildGameUri(gameId), values, null, null)
    }

    suspend fun updateColors(gameId: Int, colors: List<String>) = withContext(Dispatchers.IO) {
        if (context.contentResolver.rowExists(Games.buildGameUri(gameId))) {
            val gameColorsUri = Games.buildColorsUri(gameId)
            context.contentResolver.delete(gameColorsUri, null, null)
            val values = colors.filter { it.isNotBlank() }.map { contentValuesOf(GameColors.Columns.COLOR to it) }
            if (values.isNotEmpty()) {
                context.contentResolver.bulkInsert(gameColorsUri, values.toTypedArray())
            }
        }
    }
}
