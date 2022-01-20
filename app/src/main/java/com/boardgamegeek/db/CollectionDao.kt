package com.boardgamegeek.db

import android.content.ContentResolver
import android.content.ContentValues
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.CollectionItemGameEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class CollectionDao(private val context: BggApplication) {
    private val resolver = context.contentResolver
    private val prefs: SharedPreferences by lazy { context.preferences() }
    private val playDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun load(collectionId: Int): CollectionItemEntity? = withContext(Dispatchers.IO) {
        resolver.load(
            Collection.CONTENT_URI,
            projection(),
            "collection.${Collection.Columns.COLLECTION_ID}=?",
            arrayOf(collectionId.toString())
        )?.use {
            if (it.moveToFirst()) {
                entityFromCursor(it)
            } else null
        }
    }

    fun load(includeDeletedItems: Boolean = false): List<CollectionItemEntity> {
        val uri = Collection.CONTENT_URI
        val list = arrayListOf<CollectionItemEntity>()
        resolver.load(uri, projection())?.use {
            if (it.moveToFirst()) {
                do {
                    val item = entityFromCursor(it)
                    if (includeDeletedItems || item.deleteTimestamp == 0L)
                        list += item
                } while (it.moveToNext())
            }
        }
        return list
    }

    private fun entityFromCursor(cursor: Cursor): CollectionItemEntity {
        return CollectionItemEntity(
            internalId = cursor.getLong(COLUMN_ID),
            gameId = cursor.getInt(COLUMN_GAME_ID),
            collectionId = cursor.getInt(COLUMN_COLLECTION_ID),
            collectionName = cursor.getStringOrNull(COLUMN_COLLECTION_NAME).orEmpty(),
            sortName = cursor.getStringOrNull(COLUMN_COLLECTION_SORT_NAME).orEmpty(),
            gameName = cursor.getStringOrNull(COLUMN_GAME_NAME).orEmpty(),
            gameYearPublished = cursor.getIntOrNull(COLUMN_YEAR_PUBLISHED) ?: CollectionItemEntity.YEAR_UNKNOWN,
            collectionYearPublished = cursor.getIntOrNull(COLUMN_COLLECTION_YEAR_PUBLISHED) ?: CollectionItemEntity.YEAR_UNKNOWN,
            imageUrl = cursor.getStringOrNull(COLUMN_COLLECTION_IMAGE_URL).orEmpty(),
            thumbnailUrl = cursor.getStringOrNull(COLUMN_COLLECTION_THUMBNAIL_URL).orEmpty(),
            heroImageUrl = cursor.getStringOrNull(COLUMN_COLLECTION_HERO_IMAGE_URL).orEmpty(),
            comment = cursor.getStringOrNull(COLUMN_COMMENT).orEmpty(),
            numberOfPlays = cursor.getIntOrNull(COLUMN_NUM_PLAYS) ?: 0,
            averageRating = cursor.getDoubleOrNull(COLUMN_STATS_AVERAGE) ?: 0.0,
            rating = cursor.getDoubleOrNull(COLUMN_RATING) ?: 0.0,
            syncTimestamp = cursor.getLongOrNull(COLUMN_UPDATED) ?: 0L,
            lastModifiedDate = cursor.getLongOrNull(COLUMN_LAST_MODIFIED) ?: 0L,
            lastViewedDate = cursor.getLongOrNull(COLUMN_LAST_VIEWED) ?: 0L,
            deleteTimestamp = cursor.getLongOrNull(COLUMN_COLLECTION_DELETE_TIMESTAMP) ?: 0L,
            own = cursor.getBoolean(COLUMN_STATUS_OWN),
            previouslyOwned = cursor.getBoolean(COLUMN_STATUS_PREVIOUSLY_OWNED),
            preOrdered = cursor.getBoolean(COLUMN_STATUS_PRE_ORDERED),
            forTrade = cursor.getBoolean(COLUMN_STATUS_FOR_TRADE),
            wantInTrade = cursor.getBoolean(COLUMN_STATUS_WANT),
            wantToPlay = cursor.getBoolean(COLUMN_STATUS_WANT_TO_PLAY),
            wantToBuy = cursor.getBoolean(COLUMN_STATUS_WANT_TO_BUY),
            wishList = cursor.getBoolean(COLUMN_STATUS_WISHLIST),
            wishListPriority = cursor.getIntOrNull(COLUMN_STATUS_WISHLIST_PRIORITY) ?: CollectionItemEntity.WISHLIST_PRIORITY_UNKNOWN,
            dirtyTimestamp = cursor.getLongOrNull(COLUMN_COLLECTION_DIRTY_TIMESTAMP) ?: 0L,
            statusDirtyTimestamp = cursor.getLongOrNull(COLUMN_STATUS_DIRTY_TIMESTAMP) ?: 0L,
            ratingDirtyTimestamp = cursor.getLongOrNull(COLUMN_RATING_DIRTY_TIMESTAMP) ?: 0L,
            commentDirtyTimestamp = cursor.getLongOrNull(COLUMN_COMMENT_DIRTY_TIMESTAMP) ?: 0L,
            privateInfoDirtyTimestamp = cursor.getLongOrNull(COLUMN_PRIVATE_INFO_DIRTY_TIMESTAMP) ?: 0L,
            wishListDirtyTimestamp = cursor.getLongOrNull(COLUMN_WISHLIST_COMMENT_DIRTY_TIMESTAMP) ?: 0L,
            tradeConditionDirtyTimestamp = cursor.getLongOrNull(COLUMN_TRADE_CONDITION_DIRTY_TIMESTAMP) ?: 0L,
            hasPartsDirtyTimestamp = cursor.getLongOrNull(COLUMN_HAS_PARTS_DIRTY_TIMESTAMP) ?: 0L,
            wantPartsDirtyTimestamp = cursor.getLongOrNull(COLUMN_WANT_PARTS_DIRTY_TIMESTAMP) ?: 0L,
            quantity = cursor.getIntOrNull(COLUMN_PRIVATE_INFO_QUANTITY) ?: 0,
            pricePaid = cursor.getDoubleOrNull(COLUMN_PRIVATE_INFO_PRICE_PAID) ?: 0.0,
            pricePaidCurrency = cursor.getString(COLUMN_PRIVATE_INFO_PRICE_PAID_CURRENCY).orEmpty(),
            currentValue = cursor.getDoubleOrNull(COLUMN_PRIVATE_INFO_CURRENT_VALUE) ?: 0.0,
            currentValueCurrency = cursor.getString(COLUMN_PRIVATE_INFO_CURRENT_VALUE_CURRENCY).orEmpty(),
            acquisitionDate = cursor.getString(COLUMN_PRIVATE_INFO_ACQUISITION_DATE).orEmpty().toMillis(playDateFormat),
            acquiredFrom = cursor.getString(COLUMN_PRIVATE_INFO_ACQUIRED_FROM).orEmpty(),
            inventoryLocation = cursor.getString(COLUMN_PRIVATE_INFO_INVENTORY_LOCATION).orEmpty(),
            privateComment = cursor.getString(COLUMN_PRIVATE_INFO_COMMENT).orEmpty(),
            wishListComment = cursor.getString(COLUMN_WISHLIST_COMMENT).orEmpty(),
            wantPartsList = cursor.getString(COLUMN_WANT_PARTS_LIST).orEmpty(),
            hasPartsList = cursor.getString(COLUMN_HAS_PARTS_LIST).orEmpty(),
            conditionText = cursor.getString(COLUMN_CONDITION).orEmpty(),
            playingTime = cursor.getIntOrNull(COLUMN_PLAYING_TIME) ?: 0,
            minimumAge = cursor.getIntOrNull(COLUMN_MINIMUM_AGE) ?: 0,
            rank = cursor.getIntOrNull(COLUMN_GAME_RANK) ?: CollectionItemEntity.RANK_UNKNOWN,
            geekRating = cursor.getDoubleOrNull(COLUMN_STATS_BAYES_AVERAGE) ?: 0.0,
            averageWeight = cursor.getDoubleOrNull(COLUMN_STATS_AVERAGE_WEIGHT) ?: 0.0,
            isFavorite = cursor.getBoolean(COLUMN_STARRED),
            lastPlayDate = cursor.getString(COLUMN_MAX_DATE).orEmpty().toMillis(playDateFormat),
            minPlayerCount = cursor.getIntOrNull(COLUMN_MIN_PLAYERS) ?: 0,
            maxPlayerCount = cursor.getIntOrNull(COLUMN_MAX_PLAYERS) ?: 0,
            subType = cursor.getString(COLUMN_SUBTYPE).orEmpty(),
            bestPlayerCounts = cursor.getString(COLUMN_PLAYER_COUNTS_BEST).orEmpty(),
            recommendedPlayerCounts = cursor.getString(COLUMN_PLAYER_COUNTS_RECOMMENDED).orEmpty(),
        )
    }

    suspend fun loadByGame(gameId: Int, includeDeletedItems: Boolean = false): List<CollectionItemEntity> =
        withContext(Dispatchers.IO) {
            val list = mutableListOf<CollectionItemEntity>()
            if (gameId != INVALID_ID) {
                val uri = Collection.CONTENT_URI
                val projection = arrayOf(
                    BaseColumns._ID,
                    Collection.Columns.GAME_ID,
                    Collection.Columns.COLLECTION_ID,
                    Collection.Columns.COLLECTION_NAME,
                    Collection.Columns.COLLECTION_SORT_NAME,
                    Games.Columns.GAME_NAME, // 5
                    Collection.Columns.COLLECTION_YEAR_PUBLISHED,
                    Games.Columns.YEAR_PUBLISHED,
                    Games.Columns.IMAGE_URL,
                    Games.Columns.THUMBNAIL_URL,
                    Games.Columns.HERO_IMAGE_URL, // 10
                    Collection.Columns.COLLECTION_IMAGE_URL,
                    Collection.Columns.COLLECTION_THUMBNAIL_URL,
                    Collection.Columns.COLLECTION_HERO_IMAGE_URL,
                    Collection.Columns.COMMENT,
                    Games.Columns.NUM_PLAYS, // 15
                    Collection.Columns.RATING,
                    Collection.Columns.STATUS_OWN,
                    Collection.Columns.STATUS_PREVIOUSLY_OWNED,
                    Collection.Columns.STATUS_FOR_TRADE,
                    Collection.Columns.STATUS_WANT, // 20
                    Collection.Columns.STATUS_WANT_TO_BUY,
                    Collection.Columns.STATUS_WISHLIST,
                    Collection.Columns.STATUS_WANT_TO_PLAY,
                    Collection.Columns.STATUS_PREORDERED,
                    Collection.Columns.STATUS_WISHLIST_PRIORITY, // 25
                    Collection.Columns.UPDATED,
                    Collection.Columns.COLLECTION_DELETE_TIMESTAMP,
                    Collection.Columns.COLLECTION_DIRTY_TIMESTAMP,
                    Collection.Columns.STATUS_DIRTY_TIMESTAMP,
                    Collection.Columns.RATING_DIRTY_TIMESTAMP, //30
                    Collection.Columns.COMMENT_DIRTY_TIMESTAMP,
                    Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP,
                    Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
                    Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP,
                    Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP, // 35
                    Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP,
                    Collection.Columns.PRIVATE_INFO_PRICE_PAID,
                    Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY,
                    Collection.Columns.PRIVATE_INFO_CURRENT_VALUE,
                    Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, // 40
                    Collection.Columns.PRIVATE_INFO_QUANTITY,
                    Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM,
                    Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE,
                    Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION,
                    Collection.Columns.PRIVATE_INFO_COMMENT, // 45
                    Games.Columns.WINS_COLOR,
                    Games.Columns.WINNABLE_PLAYS_COLOR,
                    Games.Columns.ALL_PLAYS_COLOR,
                    Games.Columns.PLAYING_TIME,
                    Games.Columns.CUSTOM_PLAYER_SORT, // 50
                )
                resolver.load(
                    uri,
                    projection,
                    "collection.${Collection.Columns.GAME_ID}=?",
                    arrayOf(gameId.toString())
                )?.use {
                    if (it.moveToFirst()) {
                        do {
                            val item = CollectionItemEntity(
                                internalId = it.getLong(0),
                                gameId = it.getInt(1),
                                collectionId = it.getIntOrNull(2) ?: INVALID_ID,
                                collectionName = it.getStringOrNull(3).orEmpty(),
                                sortName = it.getStringOrNull(4).orEmpty(),
                                gameName = it.getStringOrNull(5).orEmpty(),
                                gameYearPublished = it.getIntOrNull(6) ?: CollectionItemEntity.YEAR_UNKNOWN,
                                collectionYearPublished = it.getIntOrNull(7) ?: CollectionItemEntity.YEAR_UNKNOWN,
                                imageUrl = it.getStringOrNull(11).orEmpty().ifBlank { it.getStringOrNull(8) }.orEmpty(),
                                thumbnailUrl = it.getStringOrNull(12).orEmpty().ifBlank { it.getStringOrNull(9) }.orEmpty(),
                                heroImageUrl = it.getStringOrNull(13).orEmpty().ifBlank { it.getStringOrNull(10) }.orEmpty(),
                                comment = it.getStringOrNull(14).orEmpty(),
                                numberOfPlays = it.getIntOrNull(15) ?: 0,
                                rating = it.getDoubleOrNull(16) ?: 0.0,
                                own = it.getBoolean(17),
                                previouslyOwned = it.getBoolean(18),
                                forTrade = it.getBoolean(19),
                                wantInTrade = it.getBoolean(2),
                                wantToBuy = it.getBoolean(21),
                                wishList = it.getBoolean(22),
                                wantToPlay = it.getBoolean(23),
                                preOrdered = it.getBoolean(24),
                                wishListPriority = it.getIntOrNull(25) ?: CollectionItemEntity.WISHLIST_PRIORITY_UNKNOWN,
                                syncTimestamp = it.getLongOrNull(26) ?: 0L,
                                deleteTimestamp = it.getLongOrNull(27) ?: 0L,
                                dirtyTimestamp = it.getLongOrNull(28) ?: 0L,
                                statusDirtyTimestamp = it.getLongOrNull(29) ?: 0L,
                                ratingDirtyTimestamp = it.getLongOrNull(30) ?: 0L,
                                commentDirtyTimestamp = it.getLongOrNull(31) ?: 0L,
                                privateInfoDirtyTimestamp = it.getLongOrNull(32) ?: 0L,
                                wishListDirtyTimestamp = it.getLongOrNull(33) ?: 0L,
                                tradeConditionDirtyTimestamp = it.getLongOrNull(34) ?: 0L,
                                hasPartsDirtyTimestamp = it.getLongOrNull(35) ?: 0L,
                                wantPartsDirtyTimestamp = it.getLongOrNull(36) ?: 0L,
                                pricePaid = it.getDoubleOrNull(37) ?: 0.0,
                                pricePaidCurrency = it.getStringOrNull(38).orEmpty(),
                                currentValue = it.getDoubleOrNull(39) ?: 0.0,
                                currentValueCurrency = it.getStringOrNull(40).orEmpty(),
                                quantity = it.getIntOrNull(41) ?: 0,
                                acquiredFrom = it.getStringOrNull(42).orEmpty(),
                                acquisitionDate = it.getStringOrNull(43).toMillis(playDateFormat),
                                inventoryLocation = it.getStringOrNull(44).orEmpty(),
                                privateComment = it.getStringOrNull(45).orEmpty(),
                                winsColor = it.getIntOrNull(46) ?: Color.TRANSPARENT,
                                winnablePlaysColor = it.getIntOrNull(47) ?: Color.TRANSPARENT,
                                allPlaysColor = it.getIntOrNull(48) ?: Color.TRANSPARENT,
                                playingTime = it.getIntOrNull(49) ?: 0,
                                arePlayersCustomSorted = it.getBoolean(50),
                            )
                            if (includeDeletedItems || item.deleteTimestamp == 0L)
                                list += item
                        } while (it.moveToNext())
                    }
                }
            }
            list
        }

    enum class SortType {
        NAME, RATING
    }

    suspend fun loadLinkedCollection(uri: Uri, sortBy: SortType = SortType.RATING): List<BriefGameEntity> =
        withContext(Dispatchers.IO) {
            val list = arrayListOf<BriefGameEntity>()

            val selection = prefs.getSyncStatusesOrDefault().map {
                when (it) {
                    COLLECTION_STATUS_OWN -> Collection.Columns.STATUS_OWN.isTrue()
                    COLLECTION_STATUS_PREVIOUSLY_OWNED -> Collection.Columns.STATUS_PREVIOUSLY_OWNED.isTrue()
                    COLLECTION_STATUS_PREORDERED -> Collection.Columns.STATUS_PREORDERED.isTrue()
                    COLLECTION_STATUS_FOR_TRADE -> Collection.Columns.STATUS_FOR_TRADE.isTrue()
                    COLLECTION_STATUS_WANT_IN_TRADE -> Collection.Columns.STATUS_WANT.isTrue()
                    COLLECTION_STATUS_WANT_TO_BUY -> Collection.Columns.STATUS_WANT_TO_BUY.isTrue()
                    COLLECTION_STATUS_WANT_TO_PLAY -> Collection.Columns.STATUS_WANT_TO_PLAY.isTrue()
                    COLLECTION_STATUS_WISHLIST -> Collection.Columns.STATUS_WISHLIST.isTrue()
                    COLLECTION_STATUS_RATED -> Collection.Columns.RATING.greaterThanZero()
                    COLLECTION_STATUS_PLAYED -> Games.Columns.NUM_PLAYS.greaterThanZero()
                    COLLECTION_STATUS_COMMENTED -> Collection.Columns.COMMENT.notBlank()
                    COLLECTION_STATUS_HAS_PARTS -> Collection.Columns.HASPARTS_LIST.notBlank()
                    COLLECTION_STATUS_WANT_PARTS -> Collection.Columns.WANTPARTS_LIST.notBlank()
                    else -> ""
                }
            }.filter { it.isNotBlank() }.joinToString(" OR ")

            val sortByName = Games.Columns.GAME_SORT_NAME.collateNoCase().ascending()
            val sortOrder = when (sortBy) {
                SortType.NAME -> sortByName
                SortType.RATING -> Collection.Columns.RATING.descending()
                    .plus(", ${Games.Columns.STARRED}").descending()
                    .plus(", $sortByName")
            }
            context.contentResolver.load(
                uri,
                arrayOf(
                    BaseColumns._ID,
                    Collection.Columns.GAME_ID,
                    Games.Columns.GAME_NAME,
                    Collection.Columns.COLLECTION_NAME,
                    Games.Columns.YEAR_PUBLISHED,
                    Collection.Columns.COLLECTION_YEAR_PUBLISHED,
                    Collection.Columns.COLLECTION_THUMBNAIL_URL,
                    Games.Columns.THUMBNAIL_URL,
                    Games.Columns.HERO_IMAGE_URL,
                    Collection.Columns.RATING,
                    Games.Columns.STARRED,
                    Games.Columns.SUBTYPE,
                    Games.Columns.NUM_PLAYS,
                ),
                selection,
                emptyArray(),
                sortOrder
            )?.use {
                if (it.moveToFirst()) {
                    do {
                        list += BriefGameEntity(
                            it.getLong(0),
                            it.getInt(1),
                            it.getStringOrNull(2).orEmpty(),
                            it.getStringOrNull(3).orEmpty(),
                            it.getIntOrNull(4) ?: BriefGameEntity.YEAR_UNKNOWN,
                            it.getIntOrNull(5) ?: BriefGameEntity.YEAR_UNKNOWN,
                            it.getStringOrNull(6).orEmpty(),
                            it.getStringOrNull(7).orEmpty(),
                            it.getStringOrNull(8).orEmpty(),
                            it.getDoubleOrNull(9) ?: 0.0,
                            it.getBoolean(10),
                            it.getStringOrNull(11).orEmpty(),
                            it.getIntOrNull(12) ?: 0
                        )
                    } while (it.moveToNext())
                }
            }
            list
        }

    suspend fun loadAcquiredFrom(): List<String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        resolver.load(
            Collection.buildAcquiredFromUri(),
            arrayOf(Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM)
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list += it.getStringOrNull(0).orEmpty()
                } while (it.moveToNext())
            }
        }
        list
    }

    suspend fun loadInventoryLocation(): List<String> = withContext(Dispatchers.IO) {
        resolver.queryStrings(Collection.buildInventoryLocationUri(), Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION)
    }

    suspend fun update(internalId: Long, values: ContentValues): Int = withContext(Dispatchers.IO) {
        if (internalId != INVALID_ID.toLong()) {
            resolver.update(Collection.buildUri(internalId), values, null, null)
        } else 0
    }

    /**
     * Remove all collection items belonging to a game, except the ones in the specified list.
     *
     * @param gameId                 delete collection items with this game ID.
     * @param protectedCollectionIds list of collection IDs not to delete.
     * @return the number or rows deleted.
     */
    fun delete(gameId: Int, protectedCollectionIds: List<Int>): Int {
        // determine the collection IDs that are no longer in the collection
        val collectionIdsToDelete = resolver.queryInts(
            Collection.CONTENT_URI,
            Collection.Columns.COLLECTION_ID,
            "collection.${Collection.Columns.GAME_ID}=?",
            arrayOf(gameId.toString())
        )
            .toMutableList()
        collectionIdsToDelete.removeAll(protectedCollectionIds.toSet())
        // remove them
        if (collectionIdsToDelete.size > 0) {
            for (collectionId in collectionIdsToDelete) {
                resolver.delete(
                    Collection.CONTENT_URI,
                    "${Collection.Columns.COLLECTION_ID}=?",
                    arrayOf(collectionId.toString())
                )
            }
        }

        return collectionIdsToDelete.size
    }

    suspend fun saveItem(
        item: CollectionItemEntity,
        game: CollectionItemGameEntity,
        timestamp: Long,
        includeStats: Boolean = true,
        includePrivateInfo: Boolean = true,
        isBrief: Boolean = false
    ): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var internalId = INVALID_ID.toLong()
        val candidate = SyncCandidate.find(resolver, item.collectionId, item.gameId)
        if (candidate.dirtyTimestamp != NOT_DIRTY) {
            Timber.i("Local copy of the collection item is dirty, skipping sync.")
        } else {
            upsertGame(item.gameId, toGameValues(game, includeStats, isBrief, timestamp), isBrief)
            internalId = upsertItem(
                toCollectionValues(item, includeStats, includePrivateInfo, isBrief, timestamp),
                isBrief,
                candidate
            )
            Timber.i(
                "Saved collection item '%s' [ID=%s, collection ID=%s]",
                item.gameName,
                item.gameId,
                item.collectionId
            )
        }
        item.collectionId to internalId
    }

    private fun toGameValues(
        game: CollectionItemGameEntity,
        includeStats: Boolean,
        isBrief: Boolean,
        timestamp: Long
    ): ContentValues {
        val values = ContentValues()
        values.put(Games.Columns.UPDATED_LIST, timestamp)
        values.put(Games.Columns.GAME_ID, game.gameId)
        values.put(Games.Columns.GAME_NAME, game.gameName)
        values.put(Games.Columns.GAME_SORT_NAME, game.sortName)
        if (!isBrief) {
            values.put(Games.Columns.NUM_PLAYS, game.numberOfPlays)
        }
        if (includeStats) {
            values.put(Games.Columns.MIN_PLAYERS, game.minNumberOfPlayers)
            values.put(Games.Columns.MAX_PLAYERS, game.maxNumberOfPlayers)
            values.put(Games.Columns.PLAYING_TIME, game.playingTime)
            values.put(Games.Columns.MIN_PLAYING_TIME, game.minPlayingTime)
            values.put(Games.Columns.MAX_PLAYING_TIME, game.maxPlayingTime)
            values.put(Games.Columns.STATS_NUMBER_OWNED, game.numberOwned)
            values.put(Games.Columns.STATS_AVERAGE, game.average)
            values.put(Games.Columns.STATS_BAYES_AVERAGE, game.bayesAverage)
            if (!isBrief) {
                values.put(Games.Columns.STATS_USERS_RATED, game.numberOfUsersRated)
                values.put(Games.Columns.STATS_STANDARD_DEVIATION, game.standardDeviation)
                values.put(Games.Columns.STATS_MEDIAN, game.median)
            }
        }
        return values
    }

    private fun upsertGame(gameId: Int, values: ContentValues, isBrief: Boolean) {
        val uri = Games.buildGameUri(gameId)
        if (resolver.rowExists(uri)) {
            values.remove(Games.Columns.GAME_ID)
            if (isBrief) {
                values.remove(Games.Columns.GAME_NAME)
                values.remove(Games.Columns.GAME_SORT_NAME)
            }
            resolver.update(uri, values, null, null)
        } else {
            resolver.insert(Games.CONTENT_URI, values)
        }
    }

    private fun toCollectionValues(
        item: CollectionItemEntity,
        includeStats: Boolean,
        includePrivateInfo: Boolean,
        isBrief: Boolean,
        timestamp: Long
    ): ContentValues {
        val values = ContentValues()
        if (!isBrief && includePrivateInfo && includeStats) {
            values.put(Collection.Columns.UPDATED, timestamp)
        }
        values.put(Collection.Columns.UPDATED_LIST, timestamp)
        values.put(Collection.Columns.GAME_ID, item.gameId)
        if (item.collectionId != INVALID_ID) {
            values.put(Collection.Columns.COLLECTION_ID, item.collectionId)
        }
        values.put(Collection.Columns.COLLECTION_NAME, item.collectionName)
        values.put(Collection.Columns.COLLECTION_SORT_NAME, item.sortName)
        values.put(Collection.Columns.STATUS_OWN, item.own)
        values.put(Collection.Columns.STATUS_PREVIOUSLY_OWNED, item.previouslyOwned)
        values.put(Collection.Columns.STATUS_FOR_TRADE, item.forTrade)
        values.put(Collection.Columns.STATUS_WANT, item.wantInTrade)
        values.put(Collection.Columns.STATUS_WANT_TO_PLAY, item.wantToPlay)
        values.put(Collection.Columns.STATUS_WANT_TO_BUY, item.wantToBuy)
        values.put(Collection.Columns.STATUS_WISHLIST, item.wishList)
        values.put(Collection.Columns.STATUS_WISHLIST_PRIORITY, item.wishListPriority)
        values.put(Collection.Columns.STATUS_PREORDERED, item.preOrdered)
        values.put(Collection.Columns.LAST_MODIFIED, item.lastModifiedDate)
        if (!isBrief) {
            values.put(Collection.Columns.COLLECTION_YEAR_PUBLISHED, item.collectionYearPublished)
            values.put(Collection.Columns.COLLECTION_IMAGE_URL, item.imageUrl)
            values.put(Collection.Columns.COLLECTION_THUMBNAIL_URL, item.thumbnailUrl)
            values.put(Collection.Columns.COMMENT, item.comment)
            values.put(Collection.Columns.CONDITION, item.conditionText)
            values.put(Collection.Columns.WANTPARTS_LIST, item.wantPartsList)
            values.put(Collection.Columns.HASPARTS_LIST, item.hasPartsList)
            values.put(Collection.Columns.WISHLIST_COMMENT, item.wishListComment)
            if (includePrivateInfo) {
                values.put(Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY, item.pricePaidCurrency)
                values.put(Collection.Columns.PRIVATE_INFO_PRICE_PAID, item.pricePaid)
                values.put(Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, item.currentValueCurrency)
                values.put(Collection.Columns.PRIVATE_INFO_CURRENT_VALUE, item.currentValue)
                values.put(Collection.Columns.PRIVATE_INFO_QUANTITY, item.quantity)
                values.put(Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE, item.acquisitionDate.asDateForApi())
                values.put(Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM, item.acquiredFrom)
                values.put(Collection.Columns.PRIVATE_INFO_COMMENT, item.privateComment)
                values.put(Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION, item.inventoryLocation)
            }
        }
        if (includeStats) {
            values.put(Collection.Columns.RATING, item.rating)
        }
        return values
    }

    suspend fun upsertItem(
        values: ContentValues,
        isBrief: Boolean = false,
        candidate: SyncCandidate = SyncCandidate()
    ): Long =
        withContext(Dispatchers.IO) {
            if (candidate.internalId != INVALID_ID.toLong()) {
                removeDirtyValues(values, candidate)
                val uri = Collection.buildUri(candidate.internalId)
                if (!isBrief) maybeDeleteThumbnail(values, uri)
                resolver.update(uri, values, null, null)
                candidate.internalId
            } else {
                val url = resolver.insert(Collection.CONTENT_URI, values)
                url?.lastPathSegment?.toLongOrNull() ?: INVALID_ID.toLong()
            }
        }

    private fun removeDirtyValues(values: ContentValues, candidate: SyncCandidate) {
        removeValuesIfDirty(
            values, candidate.statusDirtyTimestamp,
            Collection.Columns.STATUS_OWN,
            Collection.Columns.STATUS_PREVIOUSLY_OWNED,
            Collection.Columns.STATUS_FOR_TRADE,
            Collection.Columns.STATUS_WANT,
            Collection.Columns.STATUS_WANT_TO_BUY,
            Collection.Columns.STATUS_WISHLIST,
            Collection.Columns.STATUS_WANT_TO_PLAY,
            Collection.Columns.STATUS_PREORDERED,
            Collection.Columns.STATUS_WISHLIST_PRIORITY,
        )
        removeValuesIfDirty(values, candidate.ratingDirtyTimestamp, Collection.Columns.RATING)
        removeValuesIfDirty(values, candidate.commentDirtyTimestamp, Collection.Columns.COMMENT)
        removeValuesIfDirty(
            values, candidate.privateInfoDirtyTimestamp,
            Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM,
            Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE,
            Collection.Columns.PRIVATE_INFO_COMMENT,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY,
            Collection.Columns.PRIVATE_INFO_QUANTITY,
            Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION,
        )
        removeValuesIfDirty(values, candidate.wishListCommentDirtyTimestamp, Collection.Columns.WISHLIST_COMMENT)
        removeValuesIfDirty(values, candidate.tradeConditionDirtyTimestamp, Collection.Columns.CONDITION)
        removeValuesIfDirty(values, candidate.wantPartsDirtyTimestamp, Collection.Columns.WANTPARTS_LIST)
        removeValuesIfDirty(values, candidate.hasPartsDirtyTimestamp, Collection.Columns.HASPARTS_LIST)
    }

    private fun removeValuesIfDirty(values: ContentValues, dirtyFlag: Long, vararg columns: String) {
        if (dirtyFlag != NOT_DIRTY) columns.forEach { values.remove(it) }
    }

    private fun maybeDeleteThumbnail(values: ContentValues, uri: Uri) {
        val newThumbnailUrl: String = values.getAsString(Collection.Columns.COLLECTION_THUMBNAIL_URL).orEmpty()
        val oldThumbnailUrl = resolver.queryString(uri, Collection.Columns.COLLECTION_THUMBNAIL_URL).orEmpty()
        if (newThumbnailUrl == oldThumbnailUrl) return // nothing to do - thumbnail hasn't changed

        val thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl)
        if (thumbnailFileName.isNotBlank()) {
            resolver.delete(Thumbnails.buildUri(thumbnailFileName), null, null)
        }
    }

    class SyncCandidate(
        val internalId: Long = INVALID_ID.toLong(),
        val dirtyTimestamp: Long = 0,
        val statusDirtyTimestamp: Long = 0,
        val ratingDirtyTimestamp: Long = 0,
        val commentDirtyTimestamp: Long = 0,
        val privateInfoDirtyTimestamp: Long = 0,
        val wishListCommentDirtyTimestamp: Long = 0,
        val tradeConditionDirtyTimestamp: Long = 0,
        val wantPartsDirtyTimestamp: Long = 0,
        val hasPartsDirtyTimestamp: Long = 0,
    ) {
        companion object {
            private val projection = arrayOf(
                BaseColumns._ID,
                Collection.Columns.COLLECTION_DIRTY_TIMESTAMP,
                Collection.Columns.STATUS_DIRTY_TIMESTAMP,
                Collection.Columns.RATING_DIRTY_TIMESTAMP,
                Collection.Columns.COMMENT_DIRTY_TIMESTAMP,
                Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP,
                Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
                Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP,
                Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP,
                Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP,
            )

            fun find(resolver: ContentResolver, collectionId: Int, gameId: Int): SyncCandidate {
                if (collectionId != INVALID_ID) {
                    resolver.query(
                        Collection.CONTENT_URI,
                        projection,
                        "${Collection.Columns.COLLECTION_ID}=?",
                        arrayOf(collectionId.toString()),
                        null
                    )?.use {
                        if (it.moveToFirst()) return fromCursor(it)
                    }
                }
                resolver.query(
                    Collection.CONTENT_URI,
                    projection,
                    "collection.${Collection.Columns.GAME_ID}=? AND ${Collection.Columns.COLLECTION_ID.whereNullOrBlank()}",
                    arrayOf(gameId.toString()),
                    null
                )?.use {
                    if (it.moveToFirst()) return fromCursor(it)
                }
                return SyncCandidate()
            }

            private fun fromCursor(cursor: Cursor): SyncCandidate {
                return SyncCandidate(
                    cursor.getLongOrNull(0) ?: INVALID_ID.toLong(),
                    cursor.getLongOrNull(1) ?: 0L,
                    cursor.getLongOrNull(2) ?: 0L,
                    cursor.getLongOrNull(3) ?: 0L,
                    cursor.getLongOrNull(4) ?: 0L,
                    cursor.getLongOrNull(5) ?: 0L,
                    cursor.getLongOrNull(6) ?: 0L,
                    cursor.getLongOrNull(7) ?: 0L,
                    cursor.getLongOrNull(8) ?: 0L,
                    cursor.getLongOrNull(9) ?: 0L,
                )
            }
        }
    }

    private fun projection(): Array<String> {
        return arrayOf(
            BaseColumns._ID,
            Games.Columns.GAME_ID,
            Collection.Columns.COLLECTION_ID,
            Collection.Columns.COLLECTION_NAME,
            Collection.Columns.COLLECTION_SORT_NAME,
            Collection.Columns.COLLECTION_YEAR_PUBLISHED,
            Collection.Columns.COLLECTION_THUMBNAIL_URL,
            Collection.Columns.COLLECTION_IMAGE_URL,
            Collection.Columns.COLLECTION_HERO_IMAGE_URL,
            Collection.Columns.STATUS_OWN,
            Collection.Columns.STATUS_PREVIOUSLY_OWNED, // 10
            Collection.Columns.STATUS_FOR_TRADE,
            Collection.Columns.STATUS_WANT,
            Collection.Columns.STATUS_WANT_TO_BUY,
            Collection.Columns.STATUS_WISHLIST,
            Collection.Columns.STATUS_WANT_TO_PLAY,
            Collection.Columns.STATUS_PREORDERED,
            Collection.Columns.STATUS_WISHLIST_PRIORITY,
            Games.Columns.NUM_PLAYS,
            Collection.Columns.COMMENT,
            Games.Columns.YEAR_PUBLISHED, // 20
            Games.Columns.STATS_AVERAGE,
            Collection.Columns.RATING,
            Games.Columns.IMAGE_URL,
            Collection.Columns.UPDATED,
            Games.Columns.GAME_NAME,
            Collection.Columns.COLLECTION_DELETE_TIMESTAMP,
            Collection.Columns.COLLECTION_DIRTY_TIMESTAMP,
            Collection.Columns.STATUS_DIRTY_TIMESTAMP,
            Collection.Columns.RATING_DIRTY_TIMESTAMP,
            Collection.Columns.COMMENT_DIRTY_TIMESTAMP,
            Collection.Columns.PRIVATE_INFO_DIRTY_TIMESTAMP,
            Collection.Columns.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
            Collection.Columns.TRADE_CONDITION_DIRTY_TIMESTAMP,
            Collection.Columns.HAS_PARTS_DIRTY_TIMESTAMP,
            Collection.Columns.WANT_PARTS_DIRTY_TIMESTAMP,
            Collection.Columns.LAST_MODIFIED,
            Games.Columns.LAST_VIEWED,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY,
            Collection.Columns.PRIVATE_INFO_PRICE_PAID,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
            Collection.Columns.PRIVATE_INFO_CURRENT_VALUE,
            Collection.Columns.PRIVATE_INFO_QUANTITY,
            Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE,
            Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM,
            Collection.Columns.PRIVATE_INFO_COMMENT,
            Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION,
            Collection.Columns.WISHLIST_COMMENT,
            Collection.Columns.WANTPARTS_LIST,
            Collection.Columns.HASPARTS_LIST,
            Collection.Columns.CONDITION,
            Games.Columns.PLAYING_TIME,
            Games.Columns.MINIMUM_AGE,
            Games.Columns.GAME_RANK,
            Games.Columns.STATS_BAYES_AVERAGE,
            Games.Columns.STATS_AVERAGE_WEIGHT,
            Games.Columns.STARRED,
            Plays.Columns.MAX_DATE,
            Games.Columns.MIN_PLAYERS,
            Games.Columns.MAX_PLAYERS,
            Games.Columns.SUBTYPE,
            Games.Columns.PLAYER_COUNTS_BEST,
            Games.Columns.PLAYER_COUNTS_RECOMMENDED,
        )
    }

    companion object {
        private const val NOT_DIRTY = 0L

        private const val COLUMN_ID = 0
        private const val COLUMN_GAME_ID = 1
        private const val COLUMN_COLLECTION_ID = 2
        private const val COLUMN_COLLECTION_NAME = 3
        private const val COLUMN_COLLECTION_SORT_NAME = 4
        private const val COLUMN_COLLECTION_YEAR_PUBLISHED = 5
        private const val COLUMN_COLLECTION_THUMBNAIL_URL = 6
        private const val COLUMN_COLLECTION_IMAGE_URL = 7
        private const val COLUMN_COLLECTION_HERO_IMAGE_URL = 8
        private const val COLUMN_STATUS_OWN = 9
        private const val COLUMN_STATUS_PREVIOUSLY_OWNED = 10
        private const val COLUMN_STATUS_FOR_TRADE = 11
        private const val COLUMN_STATUS_WANT = 12
        private const val COLUMN_STATUS_WANT_TO_BUY = 13
        private const val COLUMN_STATUS_WISHLIST = 14
        private const val COLUMN_STATUS_WANT_TO_PLAY = 15
        private const val COLUMN_STATUS_PRE_ORDERED = 16
        private const val COLUMN_STATUS_WISHLIST_PRIORITY = 17
        private const val COLUMN_NUM_PLAYS = 18
        private const val COLUMN_COMMENT = 19
        private const val COLUMN_YEAR_PUBLISHED = 20
        private const val COLUMN_STATS_AVERAGE = 21
        private const val COLUMN_RATING = 22

        // private const val COLUMN_IMAGE_URL = 23
        private const val COLUMN_UPDATED = 24
        private const val COLUMN_GAME_NAME = 25
        private const val COLUMN_COLLECTION_DELETE_TIMESTAMP = 26
        private const val COLUMN_COLLECTION_DIRTY_TIMESTAMP = 27
        private const val COLUMN_STATUS_DIRTY_TIMESTAMP = 28
        private const val COLUMN_RATING_DIRTY_TIMESTAMP = 29
        private const val COLUMN_COMMENT_DIRTY_TIMESTAMP = 30
        private const val COLUMN_PRIVATE_INFO_DIRTY_TIMESTAMP = 31
        private const val COLUMN_WISHLIST_COMMENT_DIRTY_TIMESTAMP = 32
        private const val COLUMN_TRADE_CONDITION_DIRTY_TIMESTAMP = 33
        private const val COLUMN_HAS_PARTS_DIRTY_TIMESTAMP = 34
        private const val COLUMN_WANT_PARTS_DIRTY_TIMESTAMP = 35
        private const val COLUMN_LAST_MODIFIED = 36
        private const val COLUMN_LAST_VIEWED = 37
        private const val COLUMN_PRIVATE_INFO_PRICE_PAID_CURRENCY = 38
        private const val COLUMN_PRIVATE_INFO_PRICE_PAID = 39
        private const val COLUMN_PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 40
        private const val COLUMN_PRIVATE_INFO_CURRENT_VALUE = 41
        private const val COLUMN_PRIVATE_INFO_QUANTITY = 42
        private const val COLUMN_PRIVATE_INFO_ACQUISITION_DATE = 43
        private const val COLUMN_PRIVATE_INFO_ACQUIRED_FROM = 44
        private const val COLUMN_PRIVATE_INFO_COMMENT = 45
        private const val COLUMN_PRIVATE_INFO_INVENTORY_LOCATION = 46
        private const val COLUMN_WISHLIST_COMMENT = 47
        private const val COLUMN_WANT_PARTS_LIST = 48
        private const val COLUMN_HAS_PARTS_LIST = 49
        private const val COLUMN_CONDITION = 50
        private const val COLUMN_PLAYING_TIME = 51
        private const val COLUMN_MINIMUM_AGE = 52
        private const val COLUMN_GAME_RANK = 53
        private const val COLUMN_STATS_BAYES_AVERAGE = 54
        private const val COLUMN_STATS_AVERAGE_WEIGHT = 55
        private const val COLUMN_STARRED = 56
        private const val COLUMN_MAX_DATE = 57
        private const val COLUMN_MIN_PLAYERS = 58
        private const val COLUMN_MAX_PLAYERS = 59
        private const val COLUMN_SUBTYPE = 60
        private const val COLUMN_PLAYER_COUNTS_BEST = 61
        private const val COLUMN_PLAYER_COUNTS_RECOMMENDED = 62
    }
}
