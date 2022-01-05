package com.boardgamegeek.provider

import android.content.Context
import android.content.SharedPreferences
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import com.boardgamegeek.extensions.executeAsyncTask
import com.boardgamegeek.pref.SyncPrefs.Companion.getPrefs
import com.boardgamegeek.pref.clearCollection
import com.boardgamegeek.pref.clearPlaysTimestamps
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.service.SyncService
import com.boardgamegeek.service.SyncService.Companion.sync
import com.boardgamegeek.tasks.ResetGamesTask
import com.boardgamegeek.tasks.ResetPlaysTask
import com.boardgamegeek.util.FileUtils.deleteContents
import com.boardgamegeek.util.TableBuilder
import com.boardgamegeek.util.TableBuilder.COLUMN_TYPE
import com.boardgamegeek.util.TableBuilder.CONFLICT_RESOLUTION
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

class BggDatabase(private val context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    var syncPrefs: SharedPreferences = getPrefs(context!!)

    object GamesDesigners {
        const val GAME_ID = Games.GAME_ID
        const val DESIGNER_ID = Designers.DESIGNER_ID
    }

    object GamesArtists {
        const val GAME_ID = Games.GAME_ID
        const val ARTIST_ID = Artists.ARTIST_ID
    }

    object GamesPublishers {
        const val GAME_ID = Games.GAME_ID
        const val PUBLISHER_ID = Publishers.PUBLISHER_ID
    }

    object GamesMechanics {
        const val GAME_ID = Games.GAME_ID
        const val MECHANIC_ID = Mechanics.MECHANIC_ID
    }

    object GamesCategories {
        const val GAME_ID = Games.GAME_ID
        const val CATEGORY_ID = Categories.CATEGORY_ID
    }

    object Tables {
        const val DESIGNERS = "designers"
        const val ARTISTS = "artists"
        const val PUBLISHERS = "publishers"
        const val MECHANICS = "mechanics"
        const val CATEGORIES = "categories"
        const val GAMES = "games"
        const val GAME_RANKS = "game_ranks"
        const val GAMES_DESIGNERS = "games_designers"
        const val GAMES_ARTISTS = "games_artists"
        const val GAMES_PUBLISHERS = "games_publishers"
        const val GAMES_MECHANICS = "games_mechanics"
        const val GAMES_CATEGORIES = "games_categories"
        const val GAMES_EXPANSIONS = "games_expansions"
        const val COLLECTION = "collection"
        const val BUDDIES = "buddies"
        const val GAME_POLLS = "game_polls"
        const val GAME_POLL_RESULTS = "game_poll_results"
        const val GAME_POLL_RESULTS_RESULT = "game_poll_results_result"
        const val GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS = "game_suggested_player_count_poll_results"
        const val GAME_COLORS = "game_colors"
        const val PLAYS = "plays"
        const val PLAY_PLAYERS = "play_players"
        const val COLLECTION_VIEWS = "collection_filters"
        const val COLLECTION_VIEW_FILTERS = "collection_filters_details"
        const val PLAYER_COLORS = "player_colors"

        val GAMES_JOIN_COLLECTION = createJoin(GAMES, COLLECTION, Games.GAME_ID)
        val GAMES_DESIGNERS_JOIN_DESIGNERS = createJoin(GAMES_DESIGNERS, DESIGNERS, Designers.DESIGNER_ID)
        val GAMES_ARTISTS_JOIN_ARTISTS = createJoin(GAMES_ARTISTS, ARTISTS, Artists.ARTIST_ID)
        val GAMES_PUBLISHERS_JOIN_PUBLISHERS = createJoin(GAMES_PUBLISHERS, PUBLISHERS, Publishers.PUBLISHER_ID)
        val GAMES_MECHANICS_JOIN_MECHANICS = createJoin(GAMES_MECHANICS, MECHANICS, Mechanics.MECHANIC_ID)
        val GAMES_CATEGORIES_JOIN_CATEGORIES = createJoin(GAMES_CATEGORIES, CATEGORIES, Categories.CATEGORY_ID)
        val GAMES_EXPANSIONS_JOIN_EXPANSIONS = createJoin(GAMES_EXPANSIONS, GAMES, GamesExpansions.EXPANSION_ID, Games.GAME_ID)
        val GAMES_RANKS_JOIN_GAMES = createJoin(GAME_RANKS, GAMES, GameRanks.GAME_ID, Games.GAME_ID)
        val POLLS_JOIN_POLL_RESULTS = createJoin(GAME_POLLS, GAME_POLL_RESULTS, GamePolls._ID, GamePollResults.POLL_ID)
        val POLLS_JOIN_GAMES =
            createJoin(GAMES, GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS, Games.GAME_ID, GameSuggestedPlayerCountPollPollResults.GAME_ID)
        val POLL_RESULTS_JOIN_POLL_RESULTS_RESULT =
            createJoin(GAME_POLL_RESULTS, GAME_POLL_RESULTS_RESULT, GamePollResults._ID, GamePollResultsResult.POLL_RESULTS_ID)
        val COLLECTION_JOIN_GAMES = createJoin(COLLECTION, GAMES, Collection.GAME_ID)
        val GAMES_JOIN_PLAYS = GAMES + createJoinSuffix(GAMES, PLAYS, Games.GAME_ID, Plays.OBJECT_ID)
        val PLAYS_JOIN_GAMES = PLAYS + createJoinSuffix(PLAYS, GAMES, Plays.OBJECT_ID, Games.GAME_ID)
        val PLAY_PLAYERS_JOIN_PLAYS = createJoin(PLAY_PLAYERS, PLAYS, PlayPlayers._PLAY_ID, Plays._ID)
        val PLAY_PLAYERS_JOIN_PLAYS_JOIN_USERS = PLAY_PLAYERS +
                createJoinSuffix(PLAY_PLAYERS, PLAYS, PlayPlayers._PLAY_ID, Plays._ID) +
                createJoinSuffix(PLAY_PLAYERS, BUDDIES, PlayPlayers.USER_NAME, Buddies.BUDDY_NAME)
        val PLAY_PLAYERS_JOIN_PLAYS_JOIN_GAMES = PLAY_PLAYERS +
                createJoinSuffix(PLAY_PLAYERS, PLAYS, PlayPlayers._PLAY_ID, Plays._ID) +
                createJoinSuffix(PLAYS, GAMES, Plays.OBJECT_ID, Games.GAME_ID)
        val COLLECTION_VIEW_FILTERS_JOIN_COLLECTION_VIEWS =
            createJoin(COLLECTION_VIEWS, COLLECTION_VIEW_FILTERS, CollectionViews._ID, CollectionViewFilters.VIEW_ID)
        val POLLS_RESULTS_RESULT_JOIN_POLLS_RESULTS_JOIN_POLLS =
            createJoin(GAME_POLL_RESULTS_RESULT, GAME_POLL_RESULTS, GamePollResultsResult.POLL_RESULTS_ID, GamePollResults._ID) +
                    createJoinSuffix(GAME_POLL_RESULTS, GAME_POLLS, GamePollResults.POLL_ID, GamePolls._ID)
        val ARTIST_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_ARTISTS, GAMES, Games.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.GAME_ID, Collection.GAME_ID)
        val DESIGNER_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_DESIGNERS, GAMES, Games.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.GAME_ID, Collection.GAME_ID)
        val PUBLISHER_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_PUBLISHERS, GAMES, Games.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.GAME_ID, Collection.GAME_ID)
        val MECHANIC_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_MECHANICS, GAMES, Games.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.GAME_ID, Collection.GAME_ID)
        val CATEGORY_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_CATEGORIES, GAMES, Games.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.GAME_ID, Collection.GAME_ID)
        val ARTISTS_JOIN_COLLECTION = createJoin(ARTISTS, GAMES_ARTISTS, Artists.ARTIST_ID) +
                createInnerJoinSuffix(GAMES_ARTISTS, COLLECTION, Collection.GAME_ID, Collection.GAME_ID)
        val DESIGNERS_JOIN_COLLECTION = createJoin(DESIGNERS, GAMES_DESIGNERS, Designers.DESIGNER_ID) +
                createInnerJoinSuffix(GAMES_DESIGNERS, COLLECTION, Collection.GAME_ID, Collection.GAME_ID)
        val PUBLISHERS_JOIN_COLLECTION = createJoin(PUBLISHERS, GAMES_PUBLISHERS, Publishers.PUBLISHER_ID) +
                createInnerJoinSuffix(GAMES_PUBLISHERS, COLLECTION, Collection.GAME_ID, Collection.GAME_ID)
        val MECHANICS_JOIN_COLLECTION = createJoin(MECHANICS, GAMES_MECHANICS, Mechanics.MECHANIC_ID) +
                createInnerJoinSuffix(GAMES_MECHANICS, COLLECTION, Collection.GAME_ID, Collection.GAME_ID)
        val CATEGORIES_JOIN_COLLECTION = createJoin(CATEGORIES, GAMES_CATEGORIES, Categories.CATEGORY_ID) +
                createInnerJoinSuffix(GAMES_CATEGORIES, COLLECTION, Collection.GAME_ID, Collection.GAME_ID)

        private fun createJoin(table1: String, table2: String, column: String) = table1 + createJoinSuffix(table1, table2, column, column)

        private fun createJoin(table1: String, table2: String, column1: String, column2: String) =
            table1 + createJoinSuffix(table1, table2, column1, column2)

        private fun createJoinSuffix(table1: String, table2: String, column1: String, column2: String) =
            " LEFT OUTER JOIN $table2 ON $table1.$column1=$table2.$column2"

        private fun createInnerJoinSuffix(table1: String, table2: String, column1: String, column2: String) =
            " INNER JOIN $table2 ON $table1.$column1=$table2.$column2"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {
            buildDesignersTable().create(it)
            buildArtistsTable().create(it)
            buildPublishersTable().create(it)
            buildMechanicsTable().create(it)
            buildCategoriesTable().create(it)

            buildGamesTable().create(it)
            buildGameRanksTable().create(it)
            buildGamesDesignersTable().create(it)
            buildGamesArtistsTable().create(it)
            buildGamesPublishersTable().create(it)
            buildGamesMechanicsTable().create(it)
            buildGamesCategoriesTable().create(it)
            buildGameExpansionsTable().create(it)
            buildGamePollsTable().create(it)
            buildGamePollResultsTable().create(it)
            buildGamePollResultsResultTable().create(it)
            buildGameSuggestedPlayerCountPollResultsTable().create(it)
            buildGameColorsTable().create(it)

            buildPlaysTable().create(it)
            buildPlayPlayersTable().create(it)

            buildCollectionTable().create(it)

            buildBuddiesTable().create(it)
            buildPlayerColorsTable().create(it)

            buildCollectionViewsTable().create(it)
            buildCollectionViewFiltersTable().create(it)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Timber.d("Upgrading database from $oldVersion to $newVersion")
        try {
            for (version in oldVersion..newVersion) {
                db?.let {
                    when (version + 1) {
                        VER_INITIAL -> {}
                        VER_WISHLIST_PRIORITY -> addColumn(it, Tables.COLLECTION, Collection.STATUS_WISHLIST_PRIORITY, COLUMN_TYPE.INTEGER)
                        VER_GAME_COLORS -> buildGameColorsTable().create(it)
                        VER_EXPANSIONS -> buildGameExpansionsTable().create(it)
                        VER_VARIOUS -> {
                            addColumn(it, Tables.COLLECTION, Collection.LAST_MODIFIED, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.GAMES, Games.LAST_VIEWED, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.GAMES, Games.STARRED, COLUMN_TYPE.INTEGER)
                        }
                        VER_PLAYS -> {
                            buildPlaysTable().create(it)
                            buildPlayPlayersTable().create(it)
                        }
                        VER_PLAY_NICKNAME -> addColumn(it, Tables.BUDDIES, Buddies.PLAY_NICKNAME, COLUMN_TYPE.TEXT)
                        VER_PLAY_SYNC_STATUS -> {
                            addColumn(it, Tables.PLAYS, "sync_status", COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.PLAYS, "updated", COLUMN_TYPE.INTEGER)
                        }
                        VER_COLLECTION_VIEWS -> {
                            buildCollectionViewsTable().create(it)
                            buildCollectionViewFiltersTable().create(it)
                        }
                        VER_COLLECTION_VIEWS_SORT -> addColumn(it, Tables.COLLECTION_VIEWS, CollectionViews.SORT_TYPE, COLUMN_TYPE.INTEGER)
                        VER_CASCADING_DELETE -> {
                            buildGameRanksTable().replace(it)
                            buildGamesDesignersTable().replace(it)
                            buildGamesArtistsTable().replace(it)
                            buildGamesPublishersTable().replace(it)
                            buildGamesMechanicsTable().replace(it)
                            buildGamesCategoriesTable().replace(it)
                            buildGameExpansionsTable().replace(it)
                            buildGamePollsTable().replace(it)
                            buildGamePollResultsTable().replace(it)
                            buildGamePollResultsResultTable().replace(it)
                            buildGameColorsTable().replace(it)
                            buildPlayPlayersTable().replace(it)
                            buildCollectionViewFiltersTable().replace(it)
                        }
                        VER_IMAGE_CACHE -> {
                            try {
                                @Suppress("DEPRECATION")
                                val oldCacheDirectory = File(Environment.getExternalStorageDirectory(), CONTENT_AUTHORITY)
                                deleteContents(oldCacheDirectory)
                                if (!oldCacheDirectory.delete()) Timber.i("Unable to delete old cache directory")
                            } catch (e: IOException) {
                                Timber.e(e, "Error clearing the cache")
                            }
                        }
                        VER_GAMES_UPDATED_PLAYS -> addColumn(it, Tables.GAMES, Games.UPDATED_PLAYS, COLUMN_TYPE.INTEGER)
                        VER_COLLECTION -> {
                            addColumn(it, Tables.COLLECTION, Collection.CONDITION, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.COLLECTION, Collection.HASPARTS_LIST, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.COLLECTION, Collection.WANTPARTS_LIST, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.COLLECTION, Collection.WISHLIST_COMMENT, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.COLLECTION, Collection.COLLECTION_YEAR_PUBLISHED, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.COLLECTION, Collection.RATING, COLUMN_TYPE.REAL)
                            addColumn(it, Tables.COLLECTION, Collection.COLLECTION_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.COLLECTION, Collection.COLLECTION_IMAGE_URL, COLUMN_TYPE.TEXT)
                            buildCollectionTable().replace(it)
                        }
                        VER_GAME_COLLECTION_CONFLICT -> {
                            addColumn(it, Tables.GAMES, Games.SUBTYPE, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.GAMES, Games.CUSTOM_PLAYER_SORT, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.GAMES, Games.GAME_RANK, COLUMN_TYPE.INTEGER)
                            buildGamesTable().replace(it)
                            it.dropTable(Tables.COLLECTION)
                            buildCollectionTable().create(it)
                            syncPrefs.clearCollection()
                            sync(context, SyncService.FLAG_SYNC_COLLECTION)
                        }
                        VER_PLAYS_START_TIME -> addColumn(it, Tables.PLAYS, Plays.START_TIME, COLUMN_TYPE.INTEGER)
                        VER_PLAYS_PLAYER_COUNT -> {
                            addColumn(it, Tables.PLAYS, Plays.PLAYER_COUNT, COLUMN_TYPE.INTEGER)
                            it.execSQL(
                                "UPDATE ${Tables.PLAYS} SET ${Plays.PLAYER_COUNT}=(SELECT COUNT(${PlayPlayers.USER_ID}) FROM ${Tables.PLAY_PLAYERS} WHERE ${Tables.PLAYS}.${Plays.PLAY_ID}=${Tables.PLAY_PLAYERS}.${PlayPlayers.PLAY_ID})"
                            )
                        }
                        VER_GAMES_SUBTYPE -> addColumn(it, Tables.GAMES, Games.SUBTYPE, COLUMN_TYPE.TEXT)
                        VER_COLLECTION_ID_NULLABLE -> buildCollectionTable().replace(it)
                        VER_GAME_CUSTOM_PLAYER_SORT -> addColumn(it, Tables.GAMES, Games.CUSTOM_PLAYER_SORT, COLUMN_TYPE.INTEGER)
                        VER_BUDDY_FLAG -> addColumn(it, Tables.BUDDIES, Buddies.BUDDY_FLAG, COLUMN_TYPE.INTEGER)
                        VER_GAME_RANK -> addColumn(it, Tables.GAMES, Games.GAME_RANK, COLUMN_TYPE.INTEGER)
                        VER_BUDDY_SYNC_HASH_CODE -> addColumn(it, Tables.BUDDIES, Buddies.SYNC_HASH_CODE, COLUMN_TYPE.INTEGER)
                        VER_PLAY_SYNC_HASH_CODE -> addColumn(it, Tables.PLAYS, Plays.SYNC_HASH_CODE, COLUMN_TYPE.INTEGER)
                        VER_PLAYER_COLORS -> buildPlayerColorsTable().create(it)
                        VER_RATING_DIRTY_TIMESTAMP -> addColumn(it, Tables.COLLECTION, Collection.RATING_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        VER_COMMENT_DIRTY_TIMESTAMP -> addColumn(it, Tables.COLLECTION, Collection.COMMENT_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        VER_PRIVATE_INFO_DIRTY_TIMESTAMP -> addColumn(
                            db,
                            Tables.COLLECTION,
                            Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
                            COLUMN_TYPE.INTEGER
                        )
                        VER_STATUS_DIRTY_TIMESTAMP -> addColumn(it, Tables.COLLECTION, Collection.STATUS_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        VER_COLLECTION_DIRTY_TIMESTAMP -> addColumn(it, Tables.COLLECTION, Collection.COLLECTION_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        VER_COLLECTION_DELETE_TIMESTAMP -> addColumn(
                            db,
                            Tables.COLLECTION,
                            Collection.COLLECTION_DELETE_TIMESTAMP,
                            COLUMN_TYPE.INTEGER
                        )
                        VER_COLLECTION_TIMESTAMPS -> {
                            addColumn(it, Tables.COLLECTION, Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.COLLECTION, Collection.TRADE_CONDITION_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.COLLECTION, Collection.WANT_PARTS_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.COLLECTION, Collection.HAS_PARTS_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        }
                        VER_PLAY_ITEMS_COLLAPSE -> {
                            addColumn(it, Tables.PLAYS, Plays.ITEM_NAME, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.PLAYS, Plays.OBJECT_ID, COLUMN_TYPE.INTEGER)
                            it.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.OBJECT_ID} = (SELECT play_items.object_id FROM play_items WHERE play_items.${Plays.PLAY_ID} = ${Tables.PLAYS}.${Plays.PLAY_ID}), ${Plays.ITEM_NAME} = (SELECT play_items.name FROM play_items WHERE play_items.${Plays.PLAY_ID} = ${Tables.PLAYS}.${Plays.PLAY_ID})")
                            it.dropTable("play_items")
                        }
                        VER_PLAY_PLAYERS_KEY -> {
                            val columnMap: MutableMap<String, String> = HashMap()
                            columnMap[PlayPlayers._PLAY_ID] = String.format("%s.%s", Tables.PLAYS, Plays._ID)
                            buildPlayPlayersTable().replace(it, columnMap, Tables.PLAYS, Plays.PLAY_ID)
                        }
                        VER_PLAY_DELETE_TIMESTAMP -> {
                            addColumn(it, Tables.PLAYS, Plays.DELETE_TIMESTAMP, COLUMN_TYPE.INTEGER)
                            it.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.DELETE_TIMESTAMP}=${System.currentTimeMillis()}, sync_status=0 WHERE sync_status=3") // 3 = deleted sync status
                        }
                        VER_PLAY_UPDATE_TIMESTAMP -> {
                            addColumn(it, Tables.PLAYS, Plays.UPDATE_TIMESTAMP, COLUMN_TYPE.INTEGER)
                            it.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.UPDATE_TIMESTAMP}=${System.currentTimeMillis()}, sync_status=0 WHERE sync_status=1") // 1 = update sync status
                        }
                        VER_PLAY_DIRTY_TIMESTAMP -> {
                            addColumn(it, Tables.PLAYS, Plays.DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
                            it.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.DIRTY_TIMESTAMP}=${System.currentTimeMillis()}, sync_status=0 WHERE sync_status=2") // 2 = in progress
                        }
                        VER_PLAY_PLAY_ID_NOT_REQUIRED -> {
                            buildPlaysTable().replace(it)
                            it.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.PLAY_ID}=null WHERE ${Plays.PLAY_ID}>=100000000 AND (${Plays.DIRTY_TIMESTAMP}>0 OR ${Plays.UPDATE_TIMESTAMP}>0 OR ${Plays.DELETE_TIMESTAMP}>0)")
                            it.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.DIRTY_TIMESTAMP}=${System.currentTimeMillis()}, ${Plays.PLAY_ID}=null WHERE ${Plays.PLAY_ID}>=100000000")
                        }
                        VER_PLAYS_RESET -> ResetPlaysTask(context).executeAsyncTask()
                        VER_PLAYS_HARD_RESET -> {
                            it.dropTable(Tables.PLAYS)
                            it.dropTable(Tables.PLAY_PLAYERS)
                            buildPlaysTable().create(it)
                            buildPlayPlayersTable().create(it)
                            syncPrefs.clearPlaysTimestamps()
                            sync(context, SyncService.FLAG_SYNC_PLAYS)
                        }
                        VER_COLLECTION_VIEWS_SELECTED_COUNT -> {
                            addColumn(it, Tables.COLLECTION_VIEWS, CollectionViews.SELECTED_COUNT, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.COLLECTION_VIEWS, CollectionViews.SELECTED_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        }
                        VER_SUGGESTED_PLAYER_COUNT_POLL -> {
                            addColumn(it, Tables.GAMES, Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL, COLUMN_TYPE.INTEGER)
                            buildGameSuggestedPlayerCountPollResultsTable().create(it)
                        }
                        VER_SUGGESTED_PLAYER_COUNT_RECOMMENDATION -> addColumn(
                            db,
                            Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS,
                            GameSuggestedPlayerCountPollPollResults.RECOMMENDATION,
                            COLUMN_TYPE.INTEGER
                        )
                        VER_MIN_MAX_PLAYING_TIME -> {
                            addColumn(it, Tables.GAMES, Games.MIN_PLAYING_TIME, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.GAMES, Games.MAX_PLAYING_TIME, COLUMN_TYPE.INTEGER)
                        }
                        VER_SUGGESTED_PLAYER_COUNT_RESYNC -> ResetGamesTask(context).executeAsyncTask()
                        VER_GAME_HERO_IMAGE_URL -> addColumn(it, Tables.GAMES, Games.HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
                        VER_COLLECTION_HERO_IMAGE_URL -> addColumn(it, Tables.COLLECTION, Collection.COLLECTION_HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
                        VER_GAME_PALETTE_COLORS -> {
                            addColumn(it, Tables.GAMES, Games.ICON_COLOR, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.GAMES, Games.DARK_COLOR, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.GAMES, Games.WINS_COLOR, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.GAMES, Games.WINNABLE_PLAYS_COLOR, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.GAMES, Games.ALL_PLAYS_COLOR, COLUMN_TYPE.INTEGER)
                        }
                        VER_PRIVATE_INFO_INVENTORY_LOCATION -> addColumn(
                            db,
                            Tables.COLLECTION,
                            Collection.PRIVATE_INFO_INVENTORY_LOCATION,
                            COLUMN_TYPE.TEXT
                        )
                        VER_ARTIST_IMAGES -> {
                            addColumn(it, Tables.ARTISTS, Artists.ARTIST_IMAGE_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.ARTISTS, Artists.ARTIST_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.ARTISTS, Artists.ARTIST_HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.ARTISTS, Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        }
                        VER_DESIGNER_IMAGES -> {
                            addColumn(it, Tables.DESIGNERS, Designers.DESIGNER_IMAGE_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.DESIGNERS, Designers.DESIGNER_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.DESIGNERS, Designers.DESIGNER_HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.DESIGNERS, Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        }
                        VER_PUBLISHER_IMAGES -> {
                            addColumn(it, Tables.PUBLISHERS, Publishers.PUBLISHER_IMAGE_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.PUBLISHERS, Publishers.PUBLISHER_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.PUBLISHERS, Publishers.PUBLISHER_HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.PUBLISHERS, Publishers.PUBLISHER_SORT_NAME, COLUMN_TYPE.INTEGER)
                        }
                        VER_WHITMORE_SCORE -> {
                            addColumn(it, Tables.DESIGNERS, Designers.WHITMORE_SCORE, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.ARTISTS, Artists.WHITMORE_SCORE, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.PUBLISHERS, Publishers.WHITMORE_SCORE, COLUMN_TYPE.INTEGER)
                        }
                        VER_DAP_STATS_UPDATED_TIMESTAMP -> {
                            addColumn(it, Tables.DESIGNERS, Designers.DESIGNER_STATS_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.ARTISTS, Artists.ARTIST_STATS_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)
                            addColumn(it, Tables.PUBLISHERS, Publishers.PUBLISHER_STATS_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)
                        }
                        VER_RECOMMENDED_PLAYER_COUNTS -> {
                            addColumn(it, Tables.GAMES, Games.PLAYER_COUNTS_BEST, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.GAMES, Games.PLAYER_COUNTS_RECOMMENDED, COLUMN_TYPE.TEXT)
                            addColumn(it, Tables.GAMES, Games.PLAYER_COUNTS_NOT_RECOMMENDED, COLUMN_TYPE.TEXT)
                            ResetGamesTask(context).executeAsyncTask()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            recreateDatabase(db)
        }
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        if (db?.isReadOnly == false) {
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    }

    private fun buildDesignersTable() = TableBuilder()
        .setTable(Tables.DESIGNERS)
        .useDefaultPrimaryKey()
        .addColumn(Designers.UPDATED, COLUMN_TYPE.INTEGER)
        .addColumn(Designers.DESIGNER_ID, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(Designers.DESIGNER_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Designers.DESIGNER_DESCRIPTION, COLUMN_TYPE.TEXT)
        .addColumn(Designers.DESIGNER_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Designers.DESIGNER_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
        .addColumn(Designers.DESIGNER_HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Designers.DESIGNER_IMAGES_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Designers.WHITMORE_SCORE, COLUMN_TYPE.INTEGER)
        .addColumn(Designers.DESIGNER_STATS_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)

    private fun buildArtistsTable() = TableBuilder()
        .setTable(Tables.ARTISTS)
        .useDefaultPrimaryKey()
        .addColumn(Artists.UPDATED, COLUMN_TYPE.INTEGER)
        .addColumn(Artists.ARTIST_ID, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(Artists.ARTIST_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Artists.ARTIST_DESCRIPTION, COLUMN_TYPE.TEXT)
        .addColumn(Artists.ARTIST_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Artists.ARTIST_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
        .addColumn(Artists.ARTIST_HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Artists.ARTIST_IMAGES_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Artists.WHITMORE_SCORE, COLUMN_TYPE.INTEGER)
        .addColumn(Artists.ARTIST_STATS_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)

    private fun buildPublishersTable() = TableBuilder()
        .setTable(Tables.PUBLISHERS)
        .useDefaultPrimaryKey()
        .addColumn(Publishers.UPDATED, COLUMN_TYPE.INTEGER)
        .addColumn(Publishers.PUBLISHER_ID, COLUMN_TYPE.INTEGER, true, unique = true)
        .addColumn(Publishers.PUBLISHER_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Publishers.PUBLISHER_DESCRIPTION, COLUMN_TYPE.TEXT)
        .addColumn(Publishers.PUBLISHER_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Publishers.PUBLISHER_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
        .addColumn(Publishers.PUBLISHER_HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Publishers.PUBLISHER_SORT_NAME, COLUMN_TYPE.TEXT)
        .addColumn(Publishers.WHITMORE_SCORE, COLUMN_TYPE.INTEGER)
        .addColumn(Publishers.PUBLISHER_STATS_UPDATED_TIMESTAMP, COLUMN_TYPE.INTEGER)

    private fun buildMechanicsTable() = TableBuilder()
        .setTable(Tables.MECHANICS)
        .useDefaultPrimaryKey()
        .addColumn(Mechanics.MECHANIC_ID, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(Mechanics.MECHANIC_NAME, COLUMN_TYPE.TEXT, true)

    private fun buildCategoriesTable() = TableBuilder()
        .setTable(Tables.CATEGORIES)
        .useDefaultPrimaryKey()
        .addColumn(Categories.CATEGORY_ID, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(Categories.CATEGORY_NAME, COLUMN_TYPE.TEXT, true)

    private fun buildGamesTable() = TableBuilder()
        .setTable(Tables.GAMES)
        .useDefaultPrimaryKey()
        .addColumn(Games.UPDATED, COLUMN_TYPE.INTEGER)
        .addColumn(Games.UPDATED_LIST, COLUMN_TYPE.INTEGER, true)
        .addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(Games.GAME_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Games.GAME_SORT_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Games.YEAR_PUBLISHED, COLUMN_TYPE.INTEGER)
        .addColumn(Games.IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Games.THUMBNAIL_URL, COLUMN_TYPE.TEXT)
        .addColumn(Games.MIN_PLAYERS, COLUMN_TYPE.INTEGER)
        .addColumn(Games.MAX_PLAYERS, COLUMN_TYPE.INTEGER)
        .addColumn(Games.PLAYING_TIME, COLUMN_TYPE.INTEGER)
        .addColumn(Games.MIN_PLAYING_TIME, COLUMN_TYPE.INTEGER)
        .addColumn(Games.MAX_PLAYING_TIME, COLUMN_TYPE.INTEGER)
        .addColumn(Games.NUM_PLAYS, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Games.MINIMUM_AGE, COLUMN_TYPE.INTEGER)
        .addColumn(Games.DESCRIPTION, COLUMN_TYPE.TEXT)
        .addColumn(Games.SUBTYPE, COLUMN_TYPE.TEXT)
        .addColumn(Games.STATS_USERS_RATED, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STATS_AVERAGE, COLUMN_TYPE.REAL)
        .addColumn(Games.STATS_BAYES_AVERAGE, COLUMN_TYPE.REAL)
        .addColumn(Games.STATS_STANDARD_DEVIATION, COLUMN_TYPE.REAL)
        .addColumn(Games.STATS_MEDIAN, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STATS_NUMBER_OWNED, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STATS_NUMBER_TRADING, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STATS_NUMBER_WANTING, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STATS_NUMBER_WISHING, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STATS_NUMBER_COMMENTS, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STATS_NUMBER_WEIGHTS, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STATS_AVERAGE_WEIGHT, COLUMN_TYPE.REAL)
        .addColumn(Games.LAST_VIEWED, COLUMN_TYPE.INTEGER)
        .addColumn(Games.STARRED, COLUMN_TYPE.INTEGER)
        .addColumn(Games.UPDATED_PLAYS, COLUMN_TYPE.INTEGER)
        .addColumn(Games.CUSTOM_PLAYER_SORT, COLUMN_TYPE.INTEGER)
        .addColumn(Games.GAME_RANK, COLUMN_TYPE.INTEGER)
        .addColumn(Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL, COLUMN_TYPE.INTEGER)
        .addColumn(Games.HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Games.ICON_COLOR, COLUMN_TYPE.INTEGER)
        .addColumn(Games.DARK_COLOR, COLUMN_TYPE.INTEGER)
        .addColumn(Games.WINS_COLOR, COLUMN_TYPE.INTEGER)
        .addColumn(Games.WINNABLE_PLAYS_COLOR, COLUMN_TYPE.INTEGER)
        .addColumn(Games.ALL_PLAYS_COLOR, COLUMN_TYPE.INTEGER)
        .addColumn(Games.PLAYER_COUNTS_BEST, COLUMN_TYPE.TEXT)
        .addColumn(Games.PLAYER_COUNTS_RECOMMENDED, COLUMN_TYPE.TEXT)
        .addColumn(Games.PLAYER_COUNTS_NOT_RECOMMENDED, COLUMN_TYPE.TEXT)
        .setConflictResolution(CONFLICT_RESOLUTION.ABORT)

    private fun buildGameRanksTable() = TableBuilder()
        .setTable(Tables.GAME_RANKS)
        .useDefaultPrimaryKey()
        .addColumn(GameRanks.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GameRanks.GAME_RANK_ID, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(GameRanks.GAME_RANK_TYPE, COLUMN_TYPE.TEXT, true)
        .addColumn(GameRanks.GAME_RANK_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(GameRanks.GAME_RANK_FRIENDLY_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(GameRanks.GAME_RANK_VALUE, COLUMN_TYPE.INTEGER, true)
        .addColumn(GameRanks.GAME_RANK_BAYES_AVERAGE, COLUMN_TYPE.REAL, true)
        .setConflictResolution(CONFLICT_RESOLUTION.REPLACE)

    private fun buildGamesDesignersTable() = TableBuilder()
        .setTable(Tables.GAMES_DESIGNERS)
        .useDefaultPrimaryKey()
        .addColumn(GamesDesigners.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamesDesigners.DESIGNER_ID, COLUMN_TYPE.INTEGER, true,
            unique = true,
            referenceTable = Tables.DESIGNERS,
            referenceColumn = Designers.DESIGNER_ID
        )

    private fun buildGamesArtistsTable() = TableBuilder().setTable(Tables.GAMES_ARTISTS).useDefaultPrimaryKey()
        .addColumn(GamesArtists.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamesArtists.ARTIST_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.ARTISTS,
            referenceColumn = Artists.ARTIST_ID
        )

    private fun buildGamesPublishersTable() = TableBuilder()
        .setTable(Tables.GAMES_PUBLISHERS)
        .useDefaultPrimaryKey()
        .addColumn(GamesPublishers.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamesPublishers.PUBLISHER_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.PUBLISHERS,
            referenceColumn = Publishers.PUBLISHER_ID
        )

    private fun buildGamesMechanicsTable() = TableBuilder()
        .setTable(Tables.GAMES_MECHANICS)
        .useDefaultPrimaryKey()
        .addColumn(GamesMechanics.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamesMechanics.MECHANIC_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.MECHANICS,
            referenceColumn = Mechanics.MECHANIC_ID
        )

    private fun buildGamesCategoriesTable() = TableBuilder()
        .setTable(Tables.GAMES_CATEGORIES)
        .useDefaultPrimaryKey()
        .addColumn(GamesCategories.GAME_ID, COLUMN_TYPE.INTEGER, true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamesCategories.CATEGORY_ID, COLUMN_TYPE.INTEGER, true,
            unique = true,
            referenceTable = Tables.CATEGORIES,
            referenceColumn = Categories.CATEGORY_ID
        )

    private fun buildGameExpansionsTable() = TableBuilder()
        .setTable(Tables.GAMES_EXPANSIONS)
        .useDefaultPrimaryKey()
        .addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamesExpansions.EXPANSION_ID, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(GamesExpansions.EXPANSION_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(GamesExpansions.INBOUND, COLUMN_TYPE.INTEGER)

    private fun buildGamePollsTable() = TableBuilder()
        .setTable(Tables.GAME_POLLS)
        .useDefaultPrimaryKey()
        .addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamePolls.POLL_NAME, COLUMN_TYPE.TEXT, notNull = true, unique = true)
        .addColumn(GamePolls.POLL_TITLE, COLUMN_TYPE.TEXT, true)
        .addColumn(GamePolls.POLL_TOTAL_VOTES, COLUMN_TYPE.INTEGER, true)

    private fun buildGamePollResultsTable() = TableBuilder()
        .setTable(Tables.GAME_POLL_RESULTS)
        .useDefaultPrimaryKey()
        .addColumn(GamePollResults.POLL_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAME_POLLS,
            referenceColumn = GamePolls._ID,
            onCascadeDelete = true
        )
        .addColumn(GamePollResults.POLL_RESULTS_KEY, COLUMN_TYPE.TEXT, notNull = true, unique = true)
        .addColumn(GamePollResults.POLL_RESULTS_PLAYERS, COLUMN_TYPE.TEXT)
        .addColumn(GamePollResults.POLL_RESULTS_SORT_INDEX, COLUMN_TYPE.INTEGER, true)

    private fun buildGamePollResultsResultTable() = TableBuilder()
        .setTable(Tables.GAME_POLL_RESULTS_RESULT)
        .useDefaultPrimaryKey()
        .addColumn(GamePollResultsResult.POLL_RESULTS_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAME_POLL_RESULTS,
            referenceColumn = GamePollResults._ID,
            onCascadeDelete = true
        )
        .addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_KEY, COLUMN_TYPE.TEXT, notNull = true, unique = true)
        .addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL, COLUMN_TYPE.INTEGER)
        .addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE, COLUMN_TYPE.TEXT, true)
        .addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_VOTES, COLUMN_TYPE.INTEGER, true)
        .addColumn(GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX, COLUMN_TYPE.INTEGER, true)

    private fun buildGameSuggestedPlayerCountPollResultsTable() = TableBuilder()
        .setTable(Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS)
        .useDefaultPrimaryKey()
        .addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(GameSuggestedPlayerCountPollPollResults.SORT_INDEX, COLUMN_TYPE.INTEGER)
        .addColumn(GameSuggestedPlayerCountPollPollResults.BEST_VOTE_COUNT, COLUMN_TYPE.INTEGER)
        .addColumn(GameSuggestedPlayerCountPollPollResults.RECOMMENDED_VOTE_COUNT, COLUMN_TYPE.INTEGER)
        .addColumn(GameSuggestedPlayerCountPollPollResults.NOT_RECOMMENDED_VOTE_COUNT, COLUMN_TYPE.INTEGER)
        .addColumn(GameSuggestedPlayerCountPollPollResults.RECOMMENDATION, COLUMN_TYPE.INTEGER)

    private fun buildGameColorsTable() = TableBuilder()
        .setTable(Tables.GAME_COLORS)
        .useDefaultPrimaryKey()
        .addColumn(Games.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GameColors.COLOR, COLUMN_TYPE.TEXT, notNull = true, unique = true)

    private fun buildPlaysTable() = TableBuilder()
        .setTable(Tables.PLAYS)
        .useDefaultPrimaryKey()
        .addColumn(Plays.SYNC_TIMESTAMP, COLUMN_TYPE.INTEGER, true)
        .addColumn(Plays.PLAY_ID, COLUMN_TYPE.INTEGER)
        .addColumn(Plays.DATE, COLUMN_TYPE.TEXT, true)
        .addColumn(Plays.QUANTITY, COLUMN_TYPE.INTEGER, true)
        .addColumn(Plays.LENGTH, COLUMN_TYPE.INTEGER, true)
        .addColumn(Plays.INCOMPLETE, COLUMN_TYPE.INTEGER, true)
        .addColumn(Plays.NO_WIN_STATS, COLUMN_TYPE.INTEGER, true)
        .addColumn(Plays.LOCATION, COLUMN_TYPE.TEXT)
        .addColumn(Plays.COMMENTS, COLUMN_TYPE.TEXT)
        .addColumn(Plays.START_TIME, COLUMN_TYPE.INTEGER)
        .addColumn(Plays.PLAYER_COUNT, COLUMN_TYPE.INTEGER)
        .addColumn(Plays.SYNC_HASH_CODE, COLUMN_TYPE.INTEGER)
        .addColumn(Plays.ITEM_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Plays.OBJECT_ID, COLUMN_TYPE.INTEGER, true)
        .addColumn(Plays.DELETE_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Plays.UPDATE_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Plays.DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)

    private fun buildPlayPlayersTable() = TableBuilder()
        .setTable(Tables.PLAY_PLAYERS)
        .useDefaultPrimaryKey()
        .addColumn(PlayPlayers._PLAY_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = false,
            referenceTable = Tables.PLAYS,
            referenceColumn = Plays._ID,
            onCascadeDelete = true
        )
        .addColumn(PlayPlayers.USER_NAME, COLUMN_TYPE.TEXT)
        .addColumn(PlayPlayers.USER_ID, COLUMN_TYPE.INTEGER)
        .addColumn(PlayPlayers.NAME, COLUMN_TYPE.TEXT)
        .addColumn(PlayPlayers.START_POSITION, COLUMN_TYPE.TEXT)
        .addColumn(PlayPlayers.COLOR, COLUMN_TYPE.TEXT)
        .addColumn(PlayPlayers.SCORE, COLUMN_TYPE.TEXT)
        .addColumn(PlayPlayers.NEW, COLUMN_TYPE.INTEGER)
        .addColumn(PlayPlayers.RATING, COLUMN_TYPE.REAL)
        .addColumn(PlayPlayers.WIN, COLUMN_TYPE.INTEGER)

    private fun buildCollectionTable(): TableBuilder = TableBuilder()
        .setTable(Tables.COLLECTION)
        .useDefaultPrimaryKey()
        .addColumn(Collection.UPDATED, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.UPDATED_LIST, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.GAME_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = false,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(Collection.COLLECTION_ID, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.COLLECTION_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Collection.COLLECTION_SORT_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Collection.STATUS_OWN, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Collection.STATUS_PREVIOUSLY_OWNED, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Collection.STATUS_FOR_TRADE, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Collection.STATUS_WANT, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Collection.STATUS_WANT_TO_PLAY, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Collection.STATUS_WANT_TO_BUY, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Collection.STATUS_WISHLIST_PRIORITY, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.STATUS_WISHLIST, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Collection.STATUS_PREORDERED, COLUMN_TYPE.INTEGER, true, 0)
        .addColumn(Collection.COMMENT, COLUMN_TYPE.TEXT)
        .addColumn(Collection.LAST_MODIFIED, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, COLUMN_TYPE.TEXT)
        .addColumn(Collection.PRIVATE_INFO_PRICE_PAID, COLUMN_TYPE.REAL)
        .addColumn(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, COLUMN_TYPE.TEXT)
        .addColumn(Collection.PRIVATE_INFO_CURRENT_VALUE, COLUMN_TYPE.REAL)
        .addColumn(Collection.PRIVATE_INFO_QUANTITY, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.PRIVATE_INFO_ACQUISITION_DATE, COLUMN_TYPE.TEXT)
        .addColumn(Collection.PRIVATE_INFO_ACQUIRED_FROM, COLUMN_TYPE.TEXT)
        .addColumn(Collection.PRIVATE_INFO_COMMENT, COLUMN_TYPE.TEXT)
        .addColumn(Collection.CONDITION, COLUMN_TYPE.TEXT)
        .addColumn(Collection.HASPARTS_LIST, COLUMN_TYPE.TEXT)
        .addColumn(Collection.WANTPARTS_LIST, COLUMN_TYPE.TEXT)
        .addColumn(Collection.WISHLIST_COMMENT, COLUMN_TYPE.TEXT)
        .addColumn(Collection.COLLECTION_YEAR_PUBLISHED, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.RATING, COLUMN_TYPE.REAL)
        .addColumn(Collection.COLLECTION_THUMBNAIL_URL, COLUMN_TYPE.TEXT)
        .addColumn(Collection.COLLECTION_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Collection.STATUS_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.RATING_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.COMMENT_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.COLLECTION_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.COLLECTION_DELETE_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.TRADE_CONDITION_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.WANT_PARTS_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.HAS_PARTS_DIRTY_TIMESTAMP, COLUMN_TYPE.INTEGER)
        .addColumn(Collection.COLLECTION_HERO_IMAGE_URL, COLUMN_TYPE.TEXT)
        .addColumn(Collection.PRIVATE_INFO_INVENTORY_LOCATION, COLUMN_TYPE.TEXT)
        .setConflictResolution(CONFLICT_RESOLUTION.ABORT)

    private fun buildBuddiesTable() = TableBuilder().setTable(Tables.BUDDIES).useDefaultPrimaryKey()
        .addColumn(Buddies.UPDATED, COLUMN_TYPE.INTEGER)
        .addColumn(Buddies.UPDATED_LIST, COLUMN_TYPE.INTEGER, true)
        .addColumn(Buddies.BUDDY_ID, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(Buddies.BUDDY_NAME, COLUMN_TYPE.TEXT, true)
        .addColumn(Buddies.BUDDY_FIRSTNAME, COLUMN_TYPE.TEXT)
        .addColumn(Buddies.BUDDY_LASTNAME, COLUMN_TYPE.TEXT)
        .addColumn(Buddies.AVATAR_URL, COLUMN_TYPE.TEXT)
        .addColumn(Buddies.PLAY_NICKNAME, COLUMN_TYPE.TEXT)
        .addColumn(Buddies.BUDDY_FLAG, COLUMN_TYPE.INTEGER)
        .addColumn(Buddies.SYNC_HASH_CODE, COLUMN_TYPE.INTEGER)

    private fun buildPlayerColorsTable() = TableBuilder().setTable(Tables.PLAYER_COLORS)
        .setConflictResolution(CONFLICT_RESOLUTION.REPLACE)
        .useDefaultPrimaryKey()
        .addColumn(PlayerColors.PLAYER_TYPE, COLUMN_TYPE.INTEGER, notNull = true, unique = true)
        .addColumn(PlayerColors.PLAYER_NAME, COLUMN_TYPE.TEXT, notNull = true, unique = true)
        .addColumn(PlayerColors.PLAYER_COLOR, COLUMN_TYPE.TEXT, notNull = true, unique = true)
        .addColumn(PlayerColors.PLAYER_COLOR_SORT_ORDER, COLUMN_TYPE.INTEGER, true)

    private fun buildCollectionViewsTable() = TableBuilder()
        .setTable(Tables.COLLECTION_VIEWS)
        .useDefaultPrimaryKey()
        .addColumn(CollectionViews.NAME, COLUMN_TYPE.TEXT)
        .addColumn(CollectionViews.STARRED, COLUMN_TYPE.INTEGER)
        .addColumn(CollectionViews.SORT_TYPE, COLUMN_TYPE.INTEGER)
        .addColumn(CollectionViews.SELECTED_COUNT, COLUMN_TYPE.INTEGER)
        .addColumn(CollectionViews.SELECTED_TIMESTAMP, COLUMN_TYPE.INTEGER)

    private fun buildCollectionViewFiltersTable() = TableBuilder()
        .setTable(Tables.COLLECTION_VIEW_FILTERS)
        .useDefaultPrimaryKey()
        .addColumn(CollectionViewFilters.VIEW_ID, COLUMN_TYPE.INTEGER,
            notNull = true,
            unique = false,
            referenceTable = Tables.COLLECTION_VIEWS,
            referenceColumn = CollectionViews._ID,
            onCascadeDelete = true
        )
        .addColumn(CollectionViewFilters.TYPE, COLUMN_TYPE.INTEGER)
        .addColumn(CollectionViewFilters.DATA, COLUMN_TYPE.TEXT)

    private fun recreateDatabase(db: SQLiteDatabase?) {
        if (db == null) return
        db.dropTable(Tables.DESIGNERS)
        db.dropTable(Tables.ARTISTS)
        db.dropTable(Tables.PUBLISHERS)
        db.dropTable(Tables.MECHANICS)
        db.dropTable(Tables.CATEGORIES)
        db.dropTable(Tables.GAMES)
        db.dropTable(Tables.GAME_RANKS)
        db.dropTable(Tables.GAMES_DESIGNERS)
        db.dropTable(Tables.GAMES_ARTISTS)
        db.dropTable(Tables.GAMES_PUBLISHERS)
        db.dropTable(Tables.GAMES_MECHANICS)
        db.dropTable(Tables.GAMES_CATEGORIES)
        db.dropTable(Tables.GAMES_EXPANSIONS)
        db.dropTable(Tables.COLLECTION)
        db.dropTable(Tables.BUDDIES)
        db.dropTable(Tables.GAME_POLLS)
        db.dropTable(Tables.GAME_POLL_RESULTS)
        db.dropTable(Tables.GAME_POLL_RESULTS_RESULT)
        db.dropTable(Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS)
        db.dropTable(Tables.GAME_COLORS)
        db.dropTable(Tables.PLAYS)
        db.dropTable(Tables.PLAY_PLAYERS)
        db.dropTable(Tables.COLLECTION_VIEWS)
        db.dropTable(Tables.COLLECTION_VIEW_FILTERS)
        db.dropTable(Tables.PLAYER_COLORS)
        onCreate(db)
    }

    private fun SQLiteDatabase.dropTable(tableName: String) = this.execSQL("DROP TABLE IF EXISTS $tableName")

    private fun addColumn(db: SQLiteDatabase, table: String, column: String, type: COLUMN_TYPE) {
        try {
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $type")
        } catch (e: SQLException) {
            Timber.w(e, "Probably just trying to add an existing column.")
        }
    }

    companion object {
        private const val DATABASE_NAME = "bgg.db"
        private const val VER_INITIAL: Int = 1
        private const val VER_WISHLIST_PRIORITY = 2
        private const val VER_GAME_COLORS = 3
        private const val VER_EXPANSIONS = 4
        private const val VER_VARIOUS = 5
        private const val VER_PLAYS = 6
        private const val VER_PLAY_NICKNAME = 7
        private const val VER_PLAY_SYNC_STATUS = 8
        private const val VER_COLLECTION_VIEWS = 9
        private const val VER_COLLECTION_VIEWS_SORT = 10
        private const val VER_CASCADING_DELETE = 11
        private const val VER_IMAGE_CACHE = 12
        private const val VER_GAMES_UPDATED_PLAYS = 13
        private const val VER_COLLECTION = 14
        private const val VER_GAME_COLLECTION_CONFLICT = 15
        private const val VER_PLAYS_START_TIME = 16
        private const val VER_PLAYS_PLAYER_COUNT = 17
        private const val VER_GAMES_SUBTYPE = 18
        private const val VER_COLLECTION_ID_NULLABLE = 19
        private const val VER_GAME_CUSTOM_PLAYER_SORT = 20
        private const val VER_BUDDY_FLAG = 21
        private const val VER_GAME_RANK = 22
        private const val VER_BUDDY_SYNC_HASH_CODE = 23
        private const val VER_PLAY_SYNC_HASH_CODE = 24
        private const val VER_PLAYER_COLORS = 25
        private const val VER_RATING_DIRTY_TIMESTAMP = 27
        private const val VER_COMMENT_DIRTY_TIMESTAMP = 28
        private const val VER_PRIVATE_INFO_DIRTY_TIMESTAMP = 29
        private const val VER_STATUS_DIRTY_TIMESTAMP = 30
        private const val VER_COLLECTION_DIRTY_TIMESTAMP = 31
        private const val VER_COLLECTION_DELETE_TIMESTAMP = 32
        private const val VER_COLLECTION_TIMESTAMPS = 33
        private const val VER_PLAY_ITEMS_COLLAPSE = 34
        private const val VER_PLAY_PLAYERS_KEY = 36
        private const val VER_PLAY_DELETE_TIMESTAMP = 37
        private const val VER_PLAY_UPDATE_TIMESTAMP = 38
        private const val VER_PLAY_DIRTY_TIMESTAMP = 39
        private const val VER_PLAY_PLAY_ID_NOT_REQUIRED = 40
        private const val VER_PLAYS_RESET = 41
        private const val VER_PLAYS_HARD_RESET = 42
        private const val VER_COLLECTION_VIEWS_SELECTED_COUNT = 43
        private const val VER_SUGGESTED_PLAYER_COUNT_POLL = 44
        private const val VER_SUGGESTED_PLAYER_COUNT_RECOMMENDATION = 45
        private const val VER_MIN_MAX_PLAYING_TIME = 46
        private const val VER_SUGGESTED_PLAYER_COUNT_RESYNC = 47
        private const val VER_GAME_HERO_IMAGE_URL = 48
        private const val VER_COLLECTION_HERO_IMAGE_URL = 49
        private const val VER_GAME_PALETTE_COLORS = 50
        private const val VER_PRIVATE_INFO_INVENTORY_LOCATION = 51
        private const val VER_ARTIST_IMAGES = 52
        private const val VER_DESIGNER_IMAGES = 53
        private const val VER_PUBLISHER_IMAGES = 54
        private const val VER_WHITMORE_SCORE = 55
        private const val VER_DAP_STATS_UPDATED_TIMESTAMP = 56
        private const val VER_RECOMMENDED_PLAYER_COUNTS = 57
        private const val DATABASE_VERSION = VER_RECOMMENDED_PLAYER_COUNTS
    }
}
