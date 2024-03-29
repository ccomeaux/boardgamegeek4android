package com.boardgamegeek.provider

import android.content.Context
import android.content.SharedPreferences
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.provider.BaseColumns
import com.boardgamegeek.pref.SyncPrefs
import com.boardgamegeek.pref.clearCollection
import com.boardgamegeek.pref.clearPlaysTimestamps
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.FileUtils.deleteContents
import com.boardgamegeek.util.TableBuilder
import com.boardgamegeek.util.TableBuilder.ColumnType
import com.boardgamegeek.util.TableBuilder.ConflictResolution
import com.boardgamegeek.work.SyncCollectionWorker
import com.boardgamegeek.work.SyncPlaysWorker
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

class BggDatabase(private val context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    var syncPrefs: SharedPreferences = SyncPrefs.getPrefs(context!!)

    object GamesDesigners {
        const val GAME_ID = Games.Columns.GAME_ID
        const val DESIGNER_ID = Designers.Columns.DESIGNER_ID
    }

    object GamesArtists {
        const val GAME_ID = Games.Columns.GAME_ID
        const val ARTIST_ID = Artists.Columns.ARTIST_ID
    }

    object GamesPublishers {
        const val GAME_ID = Games.Columns.GAME_ID
        const val PUBLISHER_ID = Publishers.Columns.PUBLISHER_ID
    }

    object GamesMechanics {
        const val GAME_ID = Games.Columns.GAME_ID
        const val MECHANIC_ID = Mechanics.Columns.MECHANIC_ID
    }

    object GamesCategories {
        const val GAME_ID = Games.Columns.GAME_ID
        const val CATEGORY_ID = Categories.Columns.CATEGORY_ID
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

        val GAMES_JOIN_COLLECTION = createJoin(GAMES, COLLECTION, Games.Columns.GAME_ID)
        val GAMES_DESIGNERS_JOIN_DESIGNERS = createJoin(GAMES_DESIGNERS, DESIGNERS, Designers.Columns.DESIGNER_ID)
        val GAMES_ARTISTS_JOIN_ARTISTS = createJoin(GAMES_ARTISTS, ARTISTS, Artists.Columns.ARTIST_ID)
        val GAMES_PUBLISHERS_JOIN_PUBLISHERS = createJoin(GAMES_PUBLISHERS, PUBLISHERS, Publishers.Columns.PUBLISHER_ID)
        val GAMES_MECHANICS_JOIN_MECHANICS = createJoin(GAMES_MECHANICS, MECHANICS, Mechanics.Columns.MECHANIC_ID)
        val GAMES_CATEGORIES_JOIN_CATEGORIES = createJoin(GAMES_CATEGORIES, CATEGORIES, Categories.Columns.CATEGORY_ID)
        val GAMES_EXPANSIONS_JOIN_GAMES = createJoin(GAMES_EXPANSIONS, GAMES, GamesExpansions.Columns.EXPANSION_ID, Games.Columns.GAME_ID)
        val GAMES_RANKS_JOIN_GAMES = createJoin(GAME_RANKS, GAMES, GameRanks.Columns.GAME_ID, Games.Columns.GAME_ID)
        val POLLS_JOIN_POLL_RESULTS = createJoin(GAME_POLLS, GAME_POLL_RESULTS, BaseColumns._ID, GamePollResults.Columns.POLL_ID)
        val POLLS_JOIN_GAMES = createJoin(GAMES, GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS, Games.Columns.GAME_ID, GameSuggestedPlayerCountPollPollResults.Columns.GAME_ID)
        val POLL_RESULTS_JOIN_POLL_RESULTS_RESULT =
            createJoin(GAME_POLL_RESULTS, GAME_POLL_RESULTS_RESULT, BaseColumns._ID, GamePollResultsResult.Columns.POLL_RESULTS_ID)
        val COLLECTION_JOIN_GAMES = createJoin(COLLECTION, GAMES, Games.Columns.GAME_ID)
        val GAMES_JOIN_PLAYS = GAMES + createJoinSuffix(GAMES, PLAYS, Games.Columns.GAME_ID, Plays.Columns.OBJECT_ID)
        val PLAYS_JOIN_GAMES = PLAYS + createJoinSuffix(PLAYS, GAMES, Plays.Columns.OBJECT_ID, Games.Columns.GAME_ID)
        val PLAY_PLAYERS_JOIN_PLAYS = createJoin(PLAY_PLAYERS, PLAYS, PlayPlayers.Columns._PLAY_ID, BaseColumns._ID)
        val PLAY_PLAYERS_JOIN_PLAYS_JOIN_USERS = PLAY_PLAYERS +
                createJoinSuffix(PLAY_PLAYERS, PLAYS, PlayPlayers.Columns._PLAY_ID, BaseColumns._ID) +
                createJoinSuffix(PLAY_PLAYERS, BUDDIES, PlayPlayers.Columns.USER_NAME, Buddies.Columns.BUDDY_NAME)
        val PLAY_PLAYERS_JOIN_PLAYS_JOIN_GAMES = PLAY_PLAYERS +
                createJoinSuffix(PLAY_PLAYERS, PLAYS, PlayPlayers.Columns._PLAY_ID, BaseColumns._ID) +
                createJoinSuffix(PLAYS, GAMES, Plays.Columns.OBJECT_ID, Games.Columns.GAME_ID)
        val COLLECTION_VIEW_FILTERS_JOIN_COLLECTION_VIEWS =
            createJoin(COLLECTION_VIEWS, COLLECTION_VIEW_FILTERS, BaseColumns._ID, CollectionViewFilters.Columns.VIEW_ID)
        val POLLS_RESULTS_RESULT_JOIN_POLLS_RESULTS_JOIN_POLLS =
            createJoin(GAME_POLL_RESULTS_RESULT, GAME_POLL_RESULTS, GamePollResultsResult.Columns.POLL_RESULTS_ID, BaseColumns._ID) +
                    createJoinSuffix(GAME_POLL_RESULTS, GAME_POLLS, GamePollResults.Columns.POLL_ID, BaseColumns._ID)
        val ARTIST_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_ARTISTS, GAMES, Games.Columns.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.Columns.GAME_ID, Collection.Columns.GAME_ID)
        val DESIGNER_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_DESIGNERS, GAMES, Games.Columns.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.Columns.GAME_ID, Collection.Columns.GAME_ID)
        val PUBLISHER_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_PUBLISHERS, GAMES, Games.Columns.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.Columns.GAME_ID, Collection.Columns.GAME_ID)
        val MECHANIC_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_MECHANICS, GAMES, Games.Columns.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.Columns.GAME_ID, Collection.Columns.GAME_ID)
        val CATEGORY_JOIN_GAMES_JOIN_COLLECTION =
            createJoin(GAMES_CATEGORIES, GAMES, Games.Columns.GAME_ID) + createJoinSuffix(GAMES, COLLECTION, Games.Columns.GAME_ID, Collection.Columns.GAME_ID)
        val ARTISTS_JOIN_COLLECTION = createJoin(ARTISTS, GAMES_ARTISTS, Artists.Columns.ARTIST_ID) +
                createInnerJoinSuffix(GAMES_ARTISTS, COLLECTION, GamesArtists.GAME_ID, Collection.Columns.GAME_ID)
        val DESIGNERS_JOIN_COLLECTION = createJoin(DESIGNERS, GAMES_DESIGNERS, Designers.Columns.DESIGNER_ID) +
                createInnerJoinSuffix(GAMES_DESIGNERS, COLLECTION, GamesDesigners.GAME_ID, Collection.Columns.GAME_ID)
        val PUBLISHERS_JOIN_COLLECTION = createJoin(PUBLISHERS, GAMES_PUBLISHERS, Publishers.Columns.PUBLISHER_ID) +
                createInnerJoinSuffix(GAMES_PUBLISHERS, COLLECTION, GamesPublishers.GAME_ID, Collection.Columns.GAME_ID)
        val MECHANICS_JOIN_COLLECTION = createJoin(MECHANICS, GAMES_MECHANICS, Mechanics.Columns.MECHANIC_ID) +
                createInnerJoinSuffix(GAMES_MECHANICS, COLLECTION, GamesMechanics.GAME_ID, Collection.Columns.GAME_ID)
        val CATEGORIES_JOIN_COLLECTION = createJoin(CATEGORIES, GAMES_CATEGORIES, Categories.Columns.CATEGORY_ID) +
                createInnerJoinSuffix(GAMES_CATEGORIES, COLLECTION, GamesCategories.GAME_ID, Collection.Columns.GAME_ID)

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
        if (db == null) return
        var needsCollectionSync = false
        var needsPlaysSync = false
        try {
            for (version in oldVersion..newVersion) {
                when (version + 1) {
                    VER_INITIAL -> {}
                    VER_WISHLIST_PRIORITY -> addColumn(db, Tables.COLLECTION, Collection.Columns.STATUS_WISHLIST_PRIORITY, ColumnType.INTEGER)
                    VER_GAME_COLORS -> buildGameColorsTable().create(db)
                    VER_EXPANSIONS -> buildGameExpansionsTable().create(db)
                    VER_VARIOUS -> {
                        addColumn(db, Tables.COLLECTION, Collection.Columns.LAST_MODIFIED, ColumnType.INTEGER)
                        addColumn(db, Tables.GAMES, Games.Columns.LAST_VIEWED, ColumnType.INTEGER)
                        addColumn(db, Tables.GAMES, Games.Columns.STARRED, ColumnType.INTEGER)
                    }
                    VER_PLAYS -> {
                        buildPlaysTable().create(db)
                        buildPlayPlayersTable().create(db)
                    }
                    VER_PLAY_NICKNAME -> addColumn(db, Tables.BUDDIES, Buddies.Columns.PLAY_NICKNAME, ColumnType.TEXT)
                    VER_PLAY_SYNC_STATUS -> {
                        addColumn(db, Tables.PLAYS, "sync_status", ColumnType.INTEGER)
                        addColumn(db, Tables.PLAYS, "updated", ColumnType.INTEGER)
                    }
                    VER_COLLECTION_VIEWS -> {
                        buildCollectionViewsTable().create(db)
                        buildCollectionViewFiltersTable().create(db)
                    }
                    VER_COLLECTION_VIEWS_SORT -> addColumn(db, Tables.COLLECTION_VIEWS, CollectionViews.Columns.SORT_TYPE, ColumnType.INTEGER)
                    VER_CASCADING_DELETE -> {
                        buildGameRanksTable().replace(db)
                        buildGamesDesignersTable().replace(db)
                        buildGamesArtistsTable().replace(db)
                        buildGamesPublishersTable().replace(db)
                        buildGamesMechanicsTable().replace(db)
                        buildGamesCategoriesTable().replace(db)
                        buildGameExpansionsTable().replace(db)
                        buildGamePollsTable().replace(db)
                        buildGamePollResultsTable().replace(db)
                        buildGamePollResultsResultTable().replace(db)
                        buildGameColorsTable().replace(db)
                        buildPlayPlayersTable().replace(db)
                        buildCollectionViewFiltersTable().replace(db)
                    }
                    VER_IMAGE_CACHE -> {
                        try {
                            val oldCacheDirectory = File(Environment.getExternalStorageDirectory(), BggContract.CONTENT_AUTHORITY)
                            deleteContents(oldCacheDirectory)
                            val deleteSuccess = oldCacheDirectory.delete()
                            if (deleteSuccess) {
                                Timber.i("Deleted old cache directory")
                            } else {
                                Timber.i("Unable to delete old cache directory")
                            }
                        } catch (e: IOException) {
                            Timber.e(e, "Error clearing the cache")
                        }
                    }
                    VER_GAMES_UPDATED_PLAYS -> addColumn(db, Tables.GAMES, Games.Columns.UPDATED_PLAYS, ColumnType.INTEGER)
                    VER_COLLECTION -> {
                        addColumn(db, Tables.COLLECTION, Collection.Columns.CONDITION, ColumnType.TEXT)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.HASPARTS_LIST, ColumnType.TEXT)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.WANTPARTS_LIST, ColumnType.TEXT)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.WISHLIST_COMMENT, ColumnType.TEXT)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.COLLECTION_YEAR_PUBLISHED, ColumnType.INTEGER)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.RATING, ColumnType.REAL)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.COLLECTION_THUMBNAIL_URL, ColumnType.TEXT)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.COLLECTION_IMAGE_URL, ColumnType.TEXT)
                        buildCollectionTable().replace(db)
                    }
                    VER_GAME_COLLECTION_CONFLICT -> {
                        addColumn(db, Tables.GAMES, Games.Columns.SUBTYPE, ColumnType.TEXT)
                        addColumn(db, Tables.GAMES, Games.Columns.CUSTOM_PLAYER_SORT, ColumnType.INTEGER)
                        addColumn(db, Tables.GAMES, Games.Columns.GAME_RANK, ColumnType.INTEGER)
                        buildGamesTable().replace(db)
                        db.dropTable(Tables.COLLECTION)
                        buildCollectionTable().create(db)
                        syncPrefs.clearCollection()
                        needsCollectionSync = true
                    }
                    VER_PLAYS_START_TIME -> addColumn(db, Tables.PLAYS, Plays.Columns.START_TIME, ColumnType.INTEGER)
                    VER_PLAYS_PLAYER_COUNT -> {
                        addColumn(db, Tables.PLAYS, Plays.Columns.PLAYER_COUNT, ColumnType.INTEGER)
                        db.execSQL(
                            "UPDATE ${Tables.PLAYS} SET ${Plays.Columns.PLAYER_COUNT}=(SELECT COUNT(${PlayPlayers.Columns.USER_ID}) FROM ${Tables.PLAY_PLAYERS} WHERE ${Tables.PLAYS}.${Plays.Columns.PLAY_ID}=${Tables.PLAY_PLAYERS}.${Plays.Columns.PLAY_ID})"
                        )
                    }
                    VER_GAMES_SUBTYPE -> addColumn(db, Tables.GAMES, Games.Columns.SUBTYPE, ColumnType.TEXT)
                    VER_COLLECTION_ID_NULLABLE -> buildCollectionTable().replace(db)
                    VER_GAME_CUSTOM_PLAYER_SORT -> addColumn(db, Tables.GAMES, Games.Columns.CUSTOM_PLAYER_SORT, ColumnType.INTEGER)
                    VER_BUDDY_FLAG -> addColumn(db, Tables.BUDDIES, Buddies.Columns.BUDDY_FLAG, ColumnType.INTEGER)
                    VER_GAME_RANK -> addColumn(db, Tables.GAMES, Games.Columns.GAME_RANK, ColumnType.INTEGER)
                    VER_BUDDY_SYNC_HASH_CODE -> addColumn(db, Tables.BUDDIES, Buddies.Columns.SYNC_HASH_CODE, ColumnType.INTEGER)
                    VER_PLAY_SYNC_HASH_CODE -> addColumn(db, Tables.PLAYS, Plays.Columns.SYNC_HASH_CODE, ColumnType.INTEGER)
                    VER_PLAYER_COLORS -> buildPlayerColorsTable().create(db)
                    VER_RATING_DIRTY_TIMESTAMP -> addColumn(db, Tables.COLLECTION, Collection.Columns.RATING_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                    VER_COMMENT_DIRTY_TIMESTAMP -> addColumn(db, Tables.COLLECTION, Collection.Columns.COMMENT_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                    VER_PRIVATE_INFO_DIRTY_TIMESTAMP -> addColumn(db, Tables.COLLECTION, Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                    VER_STATUS_DIRTY_TIMESTAMP -> addColumn(db, Tables.COLLECTION, Collection.Columns.STATUS_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                    VER_COLLECTION_DIRTY_TIMESTAMP -> addColumn(db, Tables.COLLECTION, Collection.Columns.COLLECTION_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                    VER_COLLECTION_DELETE_TIMESTAMP -> addColumn(db, Tables.COLLECTION, Collection.Columns.COLLECTION_DELETE_TIMESTAMP, ColumnType.INTEGER)
                    VER_COLLECTION_TIMESTAMPS -> {
                        addColumn(db, Tables.COLLECTION, Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                        addColumn(db, Tables.COLLECTION, Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP, ColumnType.INTEGER)
                    }
                    VER_PLAY_ITEMS_COLLAPSE -> {
                        addColumn(db, Tables.PLAYS, Plays.Columns.ITEM_NAME, ColumnType.TEXT)
                        addColumn(db, Tables.PLAYS, Plays.Columns.OBJECT_ID, ColumnType.INTEGER)
                        db.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.Columns.OBJECT_ID} = (SELECT play_items.object_id FROM play_items WHERE play_items.${Plays.Columns.PLAY_ID} = ${Tables.PLAYS}.${Plays.Columns.PLAY_ID}), ${Plays.Columns.ITEM_NAME} = (SELECT play_items.name FROM play_items WHERE play_items.${Plays.Columns.PLAY_ID} = ${Tables.PLAYS}.${Plays.Columns.PLAY_ID})")
                        db.dropTable("play_items")
                    }
                    VER_PLAY_PLAYERS_KEY -> {
                        val columnMap: MutableMap<String, String> = HashMap()
                        columnMap[PlayPlayers.Columns._PLAY_ID] = "${Tables.PLAYS}.${BaseColumns._ID}"
                        buildPlayPlayersTable().replace(db, columnMap, Tables.PLAYS, Plays.Columns.PLAY_ID)
                    }
                    VER_PLAY_DELETE_TIMESTAMP -> {
                        addColumn(db, Tables.PLAYS, Plays.Columns.DELETE_TIMESTAMP, ColumnType.INTEGER)
                        db.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.Columns.DELETE_TIMESTAMP}=${System.currentTimeMillis()}, sync_status=0 WHERE sync_status=3") // 3 = deleted sync status
                    }
                    VER_PLAY_UPDATE_TIMESTAMP -> {
                        addColumn(db, Tables.PLAYS, Plays.Columns.UPDATE_TIMESTAMP, ColumnType.INTEGER)
                        db.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.Columns.UPDATE_TIMESTAMP}=${System.currentTimeMillis()}, sync_status=0 WHERE sync_status=1") // 1 = update sync status
                    }
                    VER_PLAY_DIRTY_TIMESTAMP -> {
                        addColumn(db, Tables.PLAYS, Plays.Columns.DIRTY_TIMESTAMP, ColumnType.INTEGER)
                        db.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.Columns.DIRTY_TIMESTAMP}=${System.currentTimeMillis()}, sync_status=0 WHERE sync_status=2") // 2 = in progress
                    }
                    VER_PLAY_PLAY_ID_NOT_REQUIRED -> {
                        buildPlaysTable().replace(db)
                        db.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.Columns.PLAY_ID}=null WHERE ${Plays.Columns.PLAY_ID}>=100000000 AND (${Plays.Columns.DIRTY_TIMESTAMP}>0 OR ${Plays.Columns.UPDATE_TIMESTAMP}>0 OR ${Plays.Columns.DELETE_TIMESTAMP}>0)")
                        db.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.Columns.DIRTY_TIMESTAMP}=${System.currentTimeMillis()}, ${Plays.Columns.PLAY_ID}=null WHERE ${Plays.Columns.PLAY_ID}>=100000000")
                    }
                    VER_PLAYS_RESET -> {
                        syncPrefs.clearPlaysTimestamps()
                        db.execSQL("UPDATE ${Tables.PLAYS} SET ${Plays.Columns.SYNC_HASH_CODE}=0")
                        needsPlaysSync = true
                    }
                    VER_PLAYS_HARD_RESET -> {
                        db.dropTable(Tables.PLAYS)
                        db.dropTable(Tables.PLAY_PLAYERS)
                        buildPlaysTable().create(db)
                        buildPlayPlayersTable().create(db)
                        syncPrefs.clearPlaysTimestamps()
                        needsPlaysSync = true
                    }
                    VER_COLLECTION_VIEWS_SELECTED_COUNT -> {
                        addColumn(db, Tables.COLLECTION_VIEWS, CollectionViews.Columns.SELECTED_COUNT, ColumnType.INTEGER)
                        addColumn(db, Tables.COLLECTION_VIEWS, CollectionViews.Columns.SELECTED_TIMESTAMP, ColumnType.INTEGER)
                    }
                    VER_SUGGESTED_PLAYER_COUNT_POLL -> {
                        addColumn(db, Tables.GAMES, Games.Columns.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL, ColumnType.INTEGER)
                        buildGameSuggestedPlayerCountPollResultsTable().create(db)
                    }
                    VER_SUGGESTED_PLAYER_COUNT_RECOMMENDATION -> addColumn(
                        db,
                        Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS,
                        GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDATION,
                        ColumnType.INTEGER
                    )
                    VER_MIN_MAX_PLAYING_TIME -> {
                        addColumn(db, Tables.GAMES, Games.Columns.MIN_PLAYING_TIME, ColumnType.INTEGER)
                        addColumn(db, Tables.GAMES, Games.Columns.MAX_PLAYING_TIME, ColumnType.INTEGER)
                    }
                    VER_SUGGESTED_PLAYER_COUNT_RE_SYNC -> {
                        db.execSQL("UPDATE ${Tables.GAMES} SET ${Games.Columns.UPDATED_LIST}=0, ${Games.Columns.UPDATED}=0, ${Games.Columns.UPDATED_PLAYS}=0")
                        needsCollectionSync = true
                    }
                    VER_GAME_HERO_IMAGE_URL -> addColumn(db, Tables.GAMES, Games.Columns.HERO_IMAGE_URL, ColumnType.TEXT)
                    VER_COLLECTION_HERO_IMAGE_URL -> addColumn(db, Tables.COLLECTION, Collection.Columns.COLLECTION_HERO_IMAGE_URL, ColumnType.TEXT)
                    VER_GAME_PALETTE_COLORS -> {
                        addColumn(db, Tables.GAMES, Games.Columns.ICON_COLOR, ColumnType.INTEGER)
                        addColumn(db, Tables.GAMES, Games.Columns.DARK_COLOR, ColumnType.INTEGER)
                        addColumn(db, Tables.GAMES, Games.Columns.WINS_COLOR, ColumnType.INTEGER)
                        addColumn(db, Tables.GAMES, Games.Columns.WINNABLE_PLAYS_COLOR, ColumnType.INTEGER)
                        addColumn(db, Tables.GAMES, Games.Columns.ALL_PLAYS_COLOR, ColumnType.INTEGER)
                    }
                    VER_PRIVATE_INFO_INVENTORY_LOCATION -> addColumn(db, Tables.COLLECTION, Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION, ColumnType.TEXT)
                    VER_ARTIST_IMAGES -> {
                        addColumn(db, Tables.ARTISTS, Artists.Columns.ARTIST_IMAGE_URL, ColumnType.TEXT)
                        addColumn(db, Tables.ARTISTS, Artists.Columns.ARTIST_THUMBNAIL_URL, ColumnType.TEXT)
                        addColumn(db, Tables.ARTISTS, Artists.Columns.ARTIST_HERO_IMAGE_URL, ColumnType.TEXT)
                        addColumn(db, Tables.ARTISTS, Artists.Columns.ARTIST_IMAGES_UPDATED_TIMESTAMP, ColumnType.INTEGER)
                    }
                    VER_DESIGNER_IMAGES -> {
                        addColumn(db, Tables.DESIGNERS, Designers.Columns.DESIGNER_IMAGE_URL, ColumnType.TEXT)
                        addColumn(db, Tables.DESIGNERS, Designers.Columns.DESIGNER_THUMBNAIL_URL, ColumnType.TEXT)
                        addColumn(db, Tables.DESIGNERS, Designers.Columns.DESIGNER_HERO_IMAGE_URL, ColumnType.TEXT)
                        addColumn(db, Tables.DESIGNERS, Designers.Columns.DESIGNER_IMAGES_UPDATED_TIMESTAMP, ColumnType.INTEGER)
                    }
                    VER_PUBLISHER_IMAGES -> {
                        addColumn(db, Tables.PUBLISHERS, Publishers.Columns.PUBLISHER_IMAGE_URL, ColumnType.TEXT)
                        addColumn(db, Tables.PUBLISHERS, Publishers.Columns.PUBLISHER_THUMBNAIL_URL, ColumnType.TEXT)
                        addColumn(db, Tables.PUBLISHERS, Publishers.Columns.PUBLISHER_HERO_IMAGE_URL, ColumnType.TEXT)
                        addColumn(db, Tables.PUBLISHERS, Publishers.Columns.PUBLISHER_SORT_NAME, ColumnType.INTEGER)
                    }
                    VER_WHITMORE_SCORE -> {
                        addColumn(db, Tables.DESIGNERS, Designers.Columns.WHITMORE_SCORE, ColumnType.INTEGER)
                        addColumn(db, Tables.ARTISTS, Artists.Columns.WHITMORE_SCORE, ColumnType.INTEGER)
                        addColumn(db, Tables.PUBLISHERS, Publishers.Columns.WHITMORE_SCORE, ColumnType.INTEGER)
                    }
                    VER_DAP_STATS_UPDATED_TIMESTAMP -> {
                        addColumn(db, Tables.DESIGNERS, Designers.Columns.DESIGNER_STATS_UPDATED_TIMESTAMP, ColumnType.INTEGER)
                        addColumn(db, Tables.ARTISTS, Artists.Columns.ARTIST_STATS_UPDATED_TIMESTAMP, ColumnType.INTEGER)
                        addColumn(db, Tables.PUBLISHERS, Publishers.Columns.PUBLISHER_STATS_UPDATED_TIMESTAMP, ColumnType.INTEGER)
                    }
                    VER_RECOMMENDED_PLAYER_COUNTS -> {
                        addColumn(db, Tables.GAMES, Games.Columns.PLAYER_COUNTS_BEST, ColumnType.TEXT)
                        addColumn(db, Tables.GAMES, Games.Columns.PLAYER_COUNTS_RECOMMENDED, ColumnType.TEXT)
                        addColumn(db, Tables.GAMES, Games.Columns.PLAYER_COUNTS_NOT_RECOMMENDED, ColumnType.TEXT)
                        db.execSQL("UPDATE ${Tables.GAMES} SET ${Games.Columns.UPDATED_LIST}=0, ${Games.Columns.UPDATED}=0, ${Games.Columns.UPDATED_PLAYS}=0")
                        needsCollectionSync = true
                    }
                }
            }
            if (needsCollectionSync) context?.let { ctx -> SyncCollectionWorker.requestSync(ctx) }
            if (needsPlaysSync) context?.let { ctx -> SyncPlaysWorker.requestSync(ctx) }
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
        .addColumn(Designers.Columns.UPDATED, ColumnType.INTEGER)
        .addColumn(Designers.Columns.DESIGNER_ID, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(Designers.Columns.DESIGNER_NAME, ColumnType.TEXT, true)
        .addColumn(Designers.Columns.DESIGNER_DESCRIPTION, ColumnType.TEXT)
        .addColumn(Designers.Columns.DESIGNER_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Designers.Columns.DESIGNER_THUMBNAIL_URL, ColumnType.TEXT)
        .addColumn(Designers.Columns.DESIGNER_HERO_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Designers.Columns.DESIGNER_IMAGES_UPDATED_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Designers.Columns.WHITMORE_SCORE, ColumnType.INTEGER)
        .addColumn(Designers.Columns.DESIGNER_STATS_UPDATED_TIMESTAMP, ColumnType.INTEGER)

    private fun buildArtistsTable() = TableBuilder()
        .setTable(Tables.ARTISTS)
        .useDefaultPrimaryKey()
        .addColumn(Artists.Columns.UPDATED, ColumnType.INTEGER)
        .addColumn(Artists.Columns.ARTIST_ID, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(Artists.Columns.ARTIST_NAME, ColumnType.TEXT, true)
        .addColumn(Artists.Columns.ARTIST_DESCRIPTION, ColumnType.TEXT)
        .addColumn(Artists.Columns.ARTIST_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Artists.Columns.ARTIST_THUMBNAIL_URL, ColumnType.TEXT)
        .addColumn(Artists.Columns.ARTIST_HERO_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Artists.Columns.ARTIST_IMAGES_UPDATED_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Artists.Columns.WHITMORE_SCORE, ColumnType.INTEGER)
        .addColumn(Artists.Columns.ARTIST_STATS_UPDATED_TIMESTAMP, ColumnType.INTEGER)

    private fun buildPublishersTable() = TableBuilder()
        .setTable(Tables.PUBLISHERS)
        .useDefaultPrimaryKey()
        .addColumn(Publishers.Columns.UPDATED, ColumnType.INTEGER)
        .addColumn(Publishers.Columns.PUBLISHER_ID, ColumnType.INTEGER, true, unique = true)
        .addColumn(Publishers.Columns.PUBLISHER_NAME, ColumnType.TEXT, true)
        .addColumn(Publishers.Columns.PUBLISHER_DESCRIPTION, ColumnType.TEXT)
        .addColumn(Publishers.Columns.PUBLISHER_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Publishers.Columns.PUBLISHER_THUMBNAIL_URL, ColumnType.TEXT)
        .addColumn(Publishers.Columns.PUBLISHER_HERO_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Publishers.Columns.PUBLISHER_SORT_NAME, ColumnType.TEXT)
        .addColumn(Publishers.Columns.WHITMORE_SCORE, ColumnType.INTEGER)
        .addColumn(Publishers.Columns.PUBLISHER_STATS_UPDATED_TIMESTAMP, ColumnType.INTEGER)

    private fun buildMechanicsTable() = TableBuilder()
        .setTable(Tables.MECHANICS)
        .useDefaultPrimaryKey()
        .addColumn(Mechanics.Columns.MECHANIC_ID, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(Mechanics.Columns.MECHANIC_NAME, ColumnType.TEXT, true)

    private fun buildCategoriesTable() = TableBuilder()
        .setTable(Tables.CATEGORIES)
        .useDefaultPrimaryKey()
        .addColumn(Categories.Columns.CATEGORY_ID, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(Categories.Columns.CATEGORY_NAME, ColumnType.TEXT, true)

    private fun buildGamesTable() = TableBuilder()
        .setTable(Tables.GAMES)
        .useDefaultPrimaryKey()
        .addColumn(Games.Columns.UPDATED, ColumnType.INTEGER)
        .addColumn(Games.Columns.UPDATED_LIST, ColumnType.INTEGER, true)
        .addColumn(Games.Columns.GAME_ID, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(Games.Columns.GAME_NAME, ColumnType.TEXT, true)
        .addColumn(Games.Columns.GAME_SORT_NAME, ColumnType.TEXT, true)
        .addColumn(Games.Columns.YEAR_PUBLISHED, ColumnType.INTEGER)
        .addColumn(Games.Columns.IMAGE_URL, ColumnType.TEXT)
        .addColumn(Games.Columns.THUMBNAIL_URL, ColumnType.TEXT)
        .addColumn(Games.Columns.MIN_PLAYERS, ColumnType.INTEGER)
        .addColumn(Games.Columns.MAX_PLAYERS, ColumnType.INTEGER)
        .addColumn(Games.Columns.PLAYING_TIME, ColumnType.INTEGER)
        .addColumn(Games.Columns.MIN_PLAYING_TIME, ColumnType.INTEGER)
        .addColumn(Games.Columns.MAX_PLAYING_TIME, ColumnType.INTEGER)
        .addColumn(Games.Columns.NUM_PLAYS, ColumnType.INTEGER, true, 0)
        .addColumn(Games.Columns.MINIMUM_AGE, ColumnType.INTEGER)
        .addColumn(Games.Columns.DESCRIPTION, ColumnType.TEXT)
        .addColumn(Games.Columns.SUBTYPE, ColumnType.TEXT)
        .addColumn(Games.Columns.STATS_USERS_RATED, ColumnType.INTEGER)
        .addColumn(Games.Columns.STATS_AVERAGE, ColumnType.REAL)
        .addColumn(Games.Columns.STATS_BAYES_AVERAGE, ColumnType.REAL)
        .addColumn(Games.Columns.STATS_STANDARD_DEVIATION, ColumnType.REAL)
        .addColumn(Games.Columns.STATS_MEDIAN, ColumnType.INTEGER)
        .addColumn(Games.Columns.STATS_NUMBER_OWNED, ColumnType.INTEGER)
        .addColumn(Games.Columns.STATS_NUMBER_TRADING, ColumnType.INTEGER)
        .addColumn(Games.Columns.STATS_NUMBER_WANTING, ColumnType.INTEGER)
        .addColumn(Games.Columns.STATS_NUMBER_WISHING, ColumnType.INTEGER)
        .addColumn(Games.Columns.STATS_NUMBER_COMMENTS, ColumnType.INTEGER)
        .addColumn(Games.Columns.STATS_NUMBER_WEIGHTS, ColumnType.INTEGER)
        .addColumn(Games.Columns.STATS_AVERAGE_WEIGHT, ColumnType.REAL)
        .addColumn(Games.Columns.LAST_VIEWED, ColumnType.INTEGER)
        .addColumn(Games.Columns.STARRED, ColumnType.INTEGER)
        .addColumn(Games.Columns.UPDATED_PLAYS, ColumnType.INTEGER)
        .addColumn(Games.Columns.CUSTOM_PLAYER_SORT, ColumnType.INTEGER)
        .addColumn(Games.Columns.GAME_RANK, ColumnType.INTEGER)
        .addColumn(Games.Columns.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL, ColumnType.INTEGER)
        .addColumn(Games.Columns.HERO_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Games.Columns.ICON_COLOR, ColumnType.INTEGER)
        .addColumn(Games.Columns.DARK_COLOR, ColumnType.INTEGER)
        .addColumn(Games.Columns.WINS_COLOR, ColumnType.INTEGER)
        .addColumn(Games.Columns.WINNABLE_PLAYS_COLOR, ColumnType.INTEGER)
        .addColumn(Games.Columns.ALL_PLAYS_COLOR, ColumnType.INTEGER)
        .addColumn(Games.Columns.PLAYER_COUNTS_BEST, ColumnType.TEXT)
        .addColumn(Games.Columns.PLAYER_COUNTS_RECOMMENDED, ColumnType.TEXT)
        .addColumn(Games.Columns.PLAYER_COUNTS_NOT_RECOMMENDED, ColumnType.TEXT)
        .setConflictResolution(ConflictResolution.ABORT)

    private fun buildGameRanksTable() = TableBuilder()
        .setTable(Tables.GAME_RANKS)
        .useDefaultPrimaryKey()
        .addColumn(
            GameRanks.Columns.GAME_ID, ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GameRanks.Columns.GAME_RANK_ID, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(GameRanks.Columns.GAME_RANK_TYPE, ColumnType.TEXT, true)
        .addColumn(GameRanks.Columns.GAME_RANK_NAME, ColumnType.TEXT, true)
        .addColumn(GameRanks.Columns.GAME_RANK_FRIENDLY_NAME, ColumnType.TEXT, true)
        .addColumn(GameRanks.Columns.GAME_RANK_VALUE, ColumnType.INTEGER, true)
        .addColumn(GameRanks.Columns.GAME_RANK_BAYES_AVERAGE, ColumnType.REAL, true)
        .setConflictResolution(ConflictResolution.REPLACE)

    private fun buildGamesDesignersTable() = TableBuilder()
        .setTable(Tables.GAMES_DESIGNERS)
        .useDefaultPrimaryKey()
        .addColumn(
            GamesDesigners.GAME_ID, ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(
            GamesDesigners.DESIGNER_ID, ColumnType.INTEGER, true,
            unique = true,
            referenceTable = Tables.DESIGNERS,
            referenceColumn = Designers.Columns.DESIGNER_ID
        )

    private fun buildGamesArtistsTable() = TableBuilder().setTable(Tables.GAMES_ARTISTS).useDefaultPrimaryKey()
        .addColumn(
            GamesArtists.GAME_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(
            GamesArtists.ARTIST_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.ARTISTS,
            referenceColumn = Artists.Columns.ARTIST_ID
        )

    private fun buildGamesPublishersTable() = TableBuilder()
        .setTable(Tables.GAMES_PUBLISHERS)
        .useDefaultPrimaryKey()
        .addColumn(
            GamesPublishers.GAME_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(
            GamesPublishers.PUBLISHER_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.PUBLISHERS,
            referenceColumn = Publishers.Columns.PUBLISHER_ID
        )

    private fun buildGamesMechanicsTable() = TableBuilder()
        .setTable(Tables.GAMES_MECHANICS)
        .useDefaultPrimaryKey()
        .addColumn(
            GamesMechanics.GAME_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(
            GamesMechanics.MECHANIC_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.MECHANICS,
            referenceColumn = Mechanics.Columns.MECHANIC_ID
        )

    private fun buildGamesCategoriesTable() = TableBuilder()
        .setTable(Tables.GAMES_CATEGORIES)
        .useDefaultPrimaryKey()
        .addColumn(
            GamesCategories.GAME_ID,
            ColumnType.INTEGER,
            true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(
            GamesCategories.CATEGORY_ID,
            ColumnType.INTEGER,
            true,
            unique = true,
            referenceTable = Tables.CATEGORIES,
            referenceColumn = Categories.Columns.CATEGORY_ID
        )

    private fun buildGameExpansionsTable() = TableBuilder()
        .setTable(Tables.GAMES_EXPANSIONS)
        .useDefaultPrimaryKey()
        .addColumn(
            GamesExpansions.Columns.GAME_ID, ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamesExpansions.Columns.EXPANSION_ID, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(GamesExpansions.Columns.EXPANSION_NAME, ColumnType.TEXT, true)
        .addColumn(GamesExpansions.Columns.INBOUND, ColumnType.INTEGER)

    private fun buildGamePollsTable() = TableBuilder()
        .setTable(Tables.GAME_POLLS)
        .useDefaultPrimaryKey()
        .addColumn(
            GamePolls.Columns.GAME_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GamePolls.Columns.POLL_NAME, ColumnType.TEXT, notNull = true, unique = true)
        .addColumn(GamePolls.Columns.POLL_TITLE, ColumnType.TEXT, true)
        .addColumn(GamePolls.Columns.POLL_TOTAL_VOTES, ColumnType.INTEGER, true)

    private fun buildGamePollResultsTable() = TableBuilder()
        .setTable(Tables.GAME_POLL_RESULTS)
        .useDefaultPrimaryKey()
        .addColumn(
            GamePollResults.Columns.POLL_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAME_POLLS,
            referenceColumn = BaseColumns._ID,
            onCascadeDelete = true
        )
        .addColumn(GamePollResults.Columns.POLL_RESULTS_KEY, ColumnType.TEXT, notNull = true, unique = true)
        .addColumn(GamePollResults.Columns.POLL_RESULTS_PLAYERS, ColumnType.TEXT)
        .addColumn(GamePollResults.Columns.POLL_RESULTS_SORT_INDEX, ColumnType.INTEGER, true)

    private fun buildGamePollResultsResultTable() = TableBuilder()
        .setTable(Tables.GAME_POLL_RESULTS_RESULT)
        .useDefaultPrimaryKey()
        .addColumn(
            GamePollResultsResult.Columns.POLL_RESULTS_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAME_POLL_RESULTS,
            referenceColumn = BaseColumns._ID,
            onCascadeDelete = true
        )
        .addColumn(GamePollResultsResult.Columns.POLL_RESULTS_RESULT_KEY, ColumnType.TEXT, notNull = true, unique = true)
        .addColumn(GamePollResultsResult.Columns.POLL_RESULTS_RESULT_LEVEL, ColumnType.INTEGER)
        .addColumn(GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VALUE, ColumnType.TEXT, true)
        .addColumn(GamePollResultsResult.Columns.POLL_RESULTS_RESULT_VOTES, ColumnType.INTEGER, true)
        .addColumn(GamePollResultsResult.Columns.POLL_RESULTS_RESULT_SORT_INDEX, ColumnType.INTEGER, true)

    private fun buildGameSuggestedPlayerCountPollResultsTable() = TableBuilder()
        .setTable(Tables.GAME_SUGGESTED_PLAYER_COUNT_POLL_RESULTS)
        .useDefaultPrimaryKey()
        .addColumn(
            GameSuggestedPlayerCountPollPollResults.Columns.GAME_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GameSuggestedPlayerCountPollPollResults.Columns.PLAYER_COUNT, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(GameSuggestedPlayerCountPollPollResults.Columns.SORT_INDEX, ColumnType.INTEGER)
        .addColumn(GameSuggestedPlayerCountPollPollResults.Columns.BEST_VOTE_COUNT, ColumnType.INTEGER)
        .addColumn(GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDED_VOTE_COUNT, ColumnType.INTEGER)
        .addColumn(GameSuggestedPlayerCountPollPollResults.Columns.NOT_RECOMMENDED_VOTE_COUNT, ColumnType.INTEGER)
        .addColumn(GameSuggestedPlayerCountPollPollResults.Columns.RECOMMENDATION, ColumnType.INTEGER)

    private fun buildGameColorsTable() = TableBuilder()
        .setTable(Tables.GAME_COLORS)
        .useDefaultPrimaryKey()
        .addColumn(
            GameColors.Columns.GAME_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = true,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(GameColors.Columns.COLOR, ColumnType.TEXT, notNull = true, unique = true)

    private fun buildPlaysTable() = TableBuilder()
        .setTable(Tables.PLAYS)
        .useDefaultPrimaryKey()
        .addColumn(Plays.Columns.SYNC_TIMESTAMP, ColumnType.INTEGER, true)
        .addColumn(Plays.Columns.PLAY_ID, ColumnType.INTEGER)
        .addColumn(Plays.Columns.DATE, ColumnType.TEXT, true)
        .addColumn(Plays.Columns.QUANTITY, ColumnType.INTEGER, true)
        .addColumn(Plays.Columns.LENGTH, ColumnType.INTEGER, true)
        .addColumn(Plays.Columns.INCOMPLETE, ColumnType.INTEGER, true)
        .addColumn(Plays.Columns.NO_WIN_STATS, ColumnType.INTEGER, true)
        .addColumn(Plays.Columns.LOCATION, ColumnType.TEXT)
        .addColumn(Plays.Columns.COMMENTS, ColumnType.TEXT)
        .addColumn(Plays.Columns.START_TIME, ColumnType.INTEGER)
        .addColumn(Plays.Columns.PLAYER_COUNT, ColumnType.INTEGER)
        .addColumn(Plays.Columns.SYNC_HASH_CODE, ColumnType.INTEGER)
        .addColumn(Plays.Columns.ITEM_NAME, ColumnType.TEXT, true)
        .addColumn(Plays.Columns.OBJECT_ID, ColumnType.INTEGER, true)
        .addColumn(Plays.Columns.DELETE_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Plays.Columns.UPDATE_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Plays.Columns.DIRTY_TIMESTAMP, ColumnType.INTEGER)

    private fun buildPlayPlayersTable() = TableBuilder()
        .setTable(Tables.PLAY_PLAYERS)
        .useDefaultPrimaryKey()
        .addColumn(
            PlayPlayers.Columns._PLAY_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = false,
            referenceTable = Tables.PLAYS,
            referenceColumn = BaseColumns._ID,
            onCascadeDelete = true
        )
        .addColumn(PlayPlayers.Columns.USER_NAME, ColumnType.TEXT)
        .addColumn(PlayPlayers.Columns.USER_ID, ColumnType.INTEGER)
        .addColumn(PlayPlayers.Columns.NAME, ColumnType.TEXT)
        .addColumn(PlayPlayers.Columns.START_POSITION, ColumnType.TEXT)
        .addColumn(PlayPlayers.Columns.COLOR, ColumnType.TEXT)
        .addColumn(PlayPlayers.Columns.SCORE, ColumnType.TEXT)
        .addColumn(PlayPlayers.Columns.NEW, ColumnType.INTEGER)
        .addColumn(PlayPlayers.Columns.RATING, ColumnType.REAL)
        .addColumn(PlayPlayers.Columns.WIN, ColumnType.INTEGER)

    private fun buildCollectionTable(): TableBuilder = TableBuilder()
        .setTable(Tables.COLLECTION)
        .useDefaultPrimaryKey()
        .addColumn(Collection.Columns.UPDATED, ColumnType.INTEGER)
        .addColumn(Collection.Columns.UPDATED_LIST, ColumnType.INTEGER)
        .addColumn(
            Collection.Columns.GAME_ID,
            ColumnType.INTEGER,
            notNull = true,
            unique = false,
            referenceTable = Tables.GAMES,
            referenceColumn = Games.Columns.GAME_ID,
            onCascadeDelete = true
        )
        .addColumn(Collection.Columns.COLLECTION_ID, ColumnType.INTEGER)
        .addColumn(Collection.Columns.COLLECTION_NAME, ColumnType.TEXT, true)
        .addColumn(Collection.Columns.COLLECTION_SORT_NAME, ColumnType.TEXT, true)
        .addColumn(Collection.Columns.STATUS_OWN, ColumnType.INTEGER, true, 0)
        .addColumn(Collection.Columns.STATUS_PREVIOUSLY_OWNED, ColumnType.INTEGER, true, 0)
        .addColumn(Collection.Columns.STATUS_FOR_TRADE, ColumnType.INTEGER, true, 0)
        .addColumn(Collection.Columns.STATUS_WANT, ColumnType.INTEGER, true, 0)
        .addColumn(Collection.Columns.STATUS_WANT_TO_PLAY, ColumnType.INTEGER, true, 0)
        .addColumn(Collection.Columns.STATUS_WANT_TO_BUY, ColumnType.INTEGER, true, 0)
        .addColumn(Collection.Columns.STATUS_WISHLIST_PRIORITY, ColumnType.INTEGER)
        .addColumn(Collection.Columns.STATUS_WISHLIST, ColumnType.INTEGER, true, 0)
        .addColumn(Collection.Columns.STATUS_PREORDERED, ColumnType.INTEGER, true, 0)
        .addColumn(Collection.Columns.COMMENT, ColumnType.TEXT)
        .addColumn(Collection.Columns.LAST_MODIFIED, ColumnType.INTEGER)
        .addColumn(Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY, ColumnType.TEXT)
        .addColumn(Collection.Columns.PRIVATE_INFO_PRICE_PAID, ColumnType.REAL)
        .addColumn(Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, ColumnType.TEXT)
        .addColumn(Collection.Columns.PRIVATE_INFO_CURRENT_VALUE, ColumnType.REAL)
        .addColumn(Collection.Columns.PRIVATE_INFO_QUANTITY, ColumnType.INTEGER)
        .addColumn(Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE, ColumnType.TEXT)
        .addColumn(Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM, ColumnType.TEXT)
        .addColumn(Collection.Columns.PRIVATE_INFO_COMMENT, ColumnType.TEXT)
        .addColumn(Collection.Columns.CONDITION, ColumnType.TEXT)
        .addColumn(Collection.Columns.HASPARTS_LIST, ColumnType.TEXT)
        .addColumn(Collection.Columns.WANTPARTS_LIST, ColumnType.TEXT)
        .addColumn(Collection.Columns.WISHLIST_COMMENT, ColumnType.TEXT)
        .addColumn(Collection.Columns.COLLECTION_YEAR_PUBLISHED, ColumnType.INTEGER)
        .addColumn(Collection.Columns.RATING, ColumnType.REAL)
        .addColumn(Collection.Columns.COLLECTION_THUMBNAIL_URL, ColumnType.TEXT)
        .addColumn(Collection.Columns.COLLECTION_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Collection.Columns.STATUS_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.RATING_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.COMMENT_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.COLLECTION_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.COLLECTION_DELETE_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP, ColumnType.INTEGER)
        .addColumn(Collection.Columns.COLLECTION_HERO_IMAGE_URL, ColumnType.TEXT)
        .addColumn(Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION, ColumnType.TEXT)
        .setConflictResolution(ConflictResolution.ABORT)

    private fun buildBuddiesTable() = TableBuilder().setTable(Tables.BUDDIES).useDefaultPrimaryKey()
        .addColumn(Buddies.Columns.UPDATED, ColumnType.INTEGER)
        .addColumn(Buddies.Columns.UPDATED_LIST, ColumnType.INTEGER, true)
        .addColumn(Buddies.Columns.BUDDY_ID, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(Buddies.Columns.BUDDY_NAME, ColumnType.TEXT, true)
        .addColumn(Buddies.Columns.BUDDY_FIRSTNAME, ColumnType.TEXT)
        .addColumn(Buddies.Columns.BUDDY_LASTNAME, ColumnType.TEXT)
        .addColumn(Buddies.Columns.AVATAR_URL, ColumnType.TEXT)
        .addColumn(Buddies.Columns.PLAY_NICKNAME, ColumnType.TEXT)
        .addColumn(Buddies.Columns.BUDDY_FLAG, ColumnType.INTEGER)
        .addColumn(Buddies.Columns.SYNC_HASH_CODE, ColumnType.INTEGER)

    private fun buildPlayerColorsTable() = TableBuilder().setTable(Tables.PLAYER_COLORS)
        .setConflictResolution(ConflictResolution.REPLACE)
        .useDefaultPrimaryKey()
        .addColumn(PlayerColors.Columns.PLAYER_TYPE, ColumnType.INTEGER, notNull = true, unique = true)
        .addColumn(PlayerColors.Columns.PLAYER_NAME, ColumnType.TEXT, notNull = true, unique = true)
        .addColumn(PlayerColors.Columns.PLAYER_COLOR, ColumnType.TEXT, notNull = true, unique = true)
        .addColumn(PlayerColors.Columns.PLAYER_COLOR_SORT_ORDER, ColumnType.INTEGER, true)

    private fun buildCollectionViewsTable() = TableBuilder()
        .setTable(Tables.COLLECTION_VIEWS)
        .useDefaultPrimaryKey()
        .addColumn(CollectionViews.Columns.NAME, ColumnType.TEXT)
        .addColumn(CollectionViews.Columns.STARRED, ColumnType.INTEGER)
        .addColumn(CollectionViews.Columns.SORT_TYPE, ColumnType.INTEGER)
        .addColumn(CollectionViews.Columns.SELECTED_COUNT, ColumnType.INTEGER)
        .addColumn(CollectionViews.Columns.SELECTED_TIMESTAMP, ColumnType.INTEGER)

    private fun buildCollectionViewFiltersTable() = TableBuilder()
        .setTable(Tables.COLLECTION_VIEW_FILTERS)
        .useDefaultPrimaryKey()
        .addColumn(
            CollectionViewFilters.Columns.VIEW_ID, ColumnType.INTEGER,
            notNull = true,
            unique = false,
            referenceTable = Tables.COLLECTION_VIEWS,
            referenceColumn = BaseColumns._ID,
            onCascadeDelete = true
        )
        .addColumn(CollectionViewFilters.Columns.TYPE, ColumnType.INTEGER)
        .addColumn(CollectionViewFilters.Columns.DATA, ColumnType.TEXT)

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

    private fun addColumn(db: SQLiteDatabase, table: String, column: String, type: ColumnType) {
        try {
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $type")
        } catch (e: SQLException) {
            Timber.w(e, "Probably just trying to add an existing column.")
        }
    }

    companion object {
        private const val DATABASE_NAME = "bgg.db"
        private const val VER_INITIAL = 1
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
        private const val VER_SUGGESTED_PLAYER_COUNT_RE_SYNC = 47
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
