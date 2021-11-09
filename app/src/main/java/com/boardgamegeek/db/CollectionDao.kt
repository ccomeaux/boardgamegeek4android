package com.boardgamegeek.db

import android.content.ContentResolver
import android.content.ContentValues
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.List
import kotlin.collections.arrayListOf
import kotlin.collections.forEach
import kotlin.collections.plusAssign
import kotlin.collections.toMutableList

class CollectionDao(private val context: BggApplication) {
    private val resolver = context.contentResolver
    private val prefs: SharedPreferences by lazy { context.preferences() }
    private val playDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun load(collectionId: Int): CollectionItemEntity? = withContext(Dispatchers.IO) {
        resolver.load(
            Collection.CONTENT_URI,
            projection(),
            "collection.${Collection.COLLECTION_ID}=?",
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
                    Collection._ID,
                    Collection.GAME_ID,
                    Collection.COLLECTION_ID,
                    Collection.COLLECTION_NAME,
                    Collection.COLLECTION_SORT_NAME,
                    Collection.GAME_NAME, // 5
                    Collection.COLLECTION_YEAR_PUBLISHED,
                    Collection.YEAR_PUBLISHED,
                    Collection.IMAGE_URL,
                    Collection.THUMBNAIL_URL,
                    Collection.HERO_IMAGE_URL, // 10
                    Collection.COLLECTION_IMAGE_URL,
                    Collection.COLLECTION_THUMBNAIL_URL,
                    Collection.COLLECTION_HERO_IMAGE_URL,
                    Collection.COMMENT,
                    Collection.NUM_PLAYS, // 15
                    Collection.RATING,
                    Collection.STATUS_OWN,
                    Collection.STATUS_PREVIOUSLY_OWNED,
                    Collection.STATUS_FOR_TRADE,
                    Collection.STATUS_WANT, // 20
                    Collection.STATUS_WANT_TO_BUY,
                    Collection.STATUS_WISHLIST,
                    Collection.STATUS_WANT_TO_PLAY,
                    Collection.STATUS_PREORDERED,
                    Collection.STATUS_WISHLIST_PRIORITY, // 25
                    Collection.UPDATED,
                    Collection.COLLECTION_DELETE_TIMESTAMP,
                    Collection.COLLECTION_DIRTY_TIMESTAMP,
                    Collection.STATUS_DIRTY_TIMESTAMP,
                    Collection.RATING_DIRTY_TIMESTAMP, //30
                    Collection.COMMENT_DIRTY_TIMESTAMP,
                    Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
                    Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
                    Collection.TRADE_CONDITION_DIRTY_TIMESTAMP,
                    Collection.HAS_PARTS_DIRTY_TIMESTAMP, // 35
                    Collection.WANT_PARTS_DIRTY_TIMESTAMP,
                    Collection.PRIVATE_INFO_PRICE_PAID,
                    Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
                    Collection.PRIVATE_INFO_CURRENT_VALUE,
                    Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, // 40
                    Collection.PRIVATE_INFO_QUANTITY,
                    Collection.PRIVATE_INFO_ACQUIRED_FROM,
                    Collection.PRIVATE_INFO_ACQUISITION_DATE,
                    Collection.PRIVATE_INFO_INVENTORY_LOCATION,
                    Collection.PRIVATE_INFO_COMMENT, // 45
                    Games.WINS_COLOR,
                    Games.WINNABLE_PLAYS_COLOR,
                    Games.ALL_PLAYS_COLOR,
                    Games.PLAYING_TIME,
                    Games.CUSTOM_PLAYER_SORT, // 50
                )
                resolver.load(
                    uri,
                    projection,
                    "collection.${Collection.GAME_ID}=?",
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
                    COLLECTION_STATUS_OWN -> Collection.STATUS_OWN.isTrue()
                    COLLECTION_STATUS_PREVIOUSLY_OWNED -> Collection.STATUS_PREVIOUSLY_OWNED.isTrue()
                    COLLECTION_STATUS_PREORDERED -> Collection.STATUS_PREORDERED.isTrue()
                    COLLECTION_STATUS_FOR_TRADE -> Collection.STATUS_FOR_TRADE.isTrue()
                    COLLECTION_STATUS_WANT_IN_TRADE -> Collection.STATUS_WANT.isTrue()
                    COLLECTION_STATUS_WANT_TO_BUY -> Collection.STATUS_WANT_TO_BUY.isTrue()
                    COLLECTION_STATUS_WANT_TO_PLAY -> Collection.STATUS_WANT_TO_PLAY.isTrue()
                    COLLECTION_STATUS_WISHLIST -> Collection.STATUS_WISHLIST.isTrue()
                    COLLECTION_STATUS_RATED -> Collection.RATING.greaterThanZero()
                    COLLECTION_STATUS_PLAYED -> Collection.NUM_PLAYS.greaterThanZero()
                    COLLECTION_STATUS_COMMENTED -> Collection.COMMENT.notBlank()
                    COLLECTION_STATUS_HAS_PARTS -> Collection.HASPARTS_LIST.notBlank()
                    COLLECTION_STATUS_WANT_PARTS -> Collection.WANTPARTS_LIST.notBlank()
                    else -> ""
                }
            }.filter { it.isNotBlank() }.joinToString(" OR ")

            val sortByName = Collection.GAME_SORT_NAME.collateNoCase().ascending()
            val sortOrder = when (sortBy) {
                SortType.NAME -> sortByName
                SortType.RATING -> Collection.RATING.descending()
                    .plus(", ${Collection.STARRED}").descending()
                    .plus(", $sortByName")
            }
            context.contentResolver.load(
                uri,
                arrayOf(
                    Collection._ID,
                    Collection.GAME_ID,
                    Collection.GAME_NAME,
                    Collection.COLLECTION_NAME,
                    Collection.YEAR_PUBLISHED,
                    Collection.COLLECTION_YEAR_PUBLISHED,
                    Collection.COLLECTION_THUMBNAIL_URL,
                    Collection.THUMBNAIL_URL,
                    Collection.HERO_IMAGE_URL,
                    Collection.RATING,
                    Collection.STARRED,
                    Collection.SUBTYPE,
                    Collection.NUM_PLAYS
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
            Collection.COLLECTION_ID,
            "collection.${Collection.GAME_ID}=?",
            arrayOf(gameId.toString())
        )
            .toMutableList()
        collectionIdsToDelete.removeAll(protectedCollectionIds)
        // remove them
        if (collectionIdsToDelete.size > 0) {
            for (collectionId in collectionIdsToDelete) {
                resolver.delete(
                    Collection.CONTENT_URI,
                    "${Collection.COLLECTION_ID}=?",
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
        values.put(Games.UPDATED_LIST, timestamp)
        values.put(Games.GAME_ID, game.gameId)
        values.put(Games.GAME_NAME, game.gameName)
        values.put(Games.GAME_SORT_NAME, game.sortName)
        if (!isBrief) {
            values.put(Games.NUM_PLAYS, game.numberOfPlays)
        }
        if (includeStats) {
            values.put(Games.MIN_PLAYERS, game.minNumberOfPlayers)
            values.put(Games.MAX_PLAYERS, game.maxNumberOfPlayers)
            values.put(Games.PLAYING_TIME, game.playingTime)
            values.put(Games.MIN_PLAYING_TIME, game.minPlayingTime)
            values.put(Games.MAX_PLAYING_TIME, game.maxPlayingTime)
            values.put(Games.STATS_NUMBER_OWNED, game.numberOwned)
            values.put(Games.STATS_AVERAGE, game.average)
            values.put(Games.STATS_BAYES_AVERAGE, game.bayesAverage)
            if (!isBrief) {
                values.put(Games.STATS_USERS_RATED, game.numberOfUsersRated)
                values.put(Games.STATS_STANDARD_DEVIATION, game.standardDeviation)
                values.put(Games.STATS_MEDIAN, game.median)
            }
        }
        return values
    }

    private fun upsertGame(gameId: Int, values: ContentValues, isBrief: Boolean) {
        val uri = Games.buildGameUri(gameId)
        if (resolver.rowExists(uri)) {
            values.remove(Games.GAME_ID)
            if (isBrief) {
                values.remove(Games.GAME_NAME)
                values.remove(Games.GAME_SORT_NAME)
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
            values.put(Collection.UPDATED, timestamp)
        }
        values.put(Collection.UPDATED_LIST, timestamp)
        values.put(Collection.GAME_ID, item.gameId)
        if (item.collectionId != INVALID_ID) {
            values.put(Collection.COLLECTION_ID, item.collectionId)
        }
        values.put(Collection.COLLECTION_NAME, item.collectionName)
        values.put(Collection.COLLECTION_SORT_NAME, item.sortName)
        values.put(Collection.STATUS_OWN, item.own)
        values.put(Collection.STATUS_PREVIOUSLY_OWNED, item.previouslyOwned)
        values.put(Collection.STATUS_FOR_TRADE, item.forTrade)
        values.put(Collection.STATUS_WANT, item.wantInTrade)
        values.put(Collection.STATUS_WANT_TO_PLAY, item.wantToPlay)
        values.put(Collection.STATUS_WANT_TO_BUY, item.wantToBuy)
        values.put(Collection.STATUS_WISHLIST, item.wishList)
        values.put(Collection.STATUS_WISHLIST_PRIORITY, item.wishListPriority)
        values.put(Collection.STATUS_PREORDERED, item.preOrdered)
        values.put(Collection.LAST_MODIFIED, item.lastModifiedDate)
        if (!isBrief) {
            values.put(Collection.COLLECTION_YEAR_PUBLISHED, item.collectionYearPublished)
            values.put(Collection.COLLECTION_IMAGE_URL, item.imageUrl)
            values.put(Collection.COLLECTION_THUMBNAIL_URL, item.thumbnailUrl)
            values.put(Collection.COMMENT, item.comment)
            values.put(Collection.CONDITION, item.conditionText)
            values.put(Collection.WANTPARTS_LIST, item.wantPartsList)
            values.put(Collection.HASPARTS_LIST, item.hasPartsList)
            values.put(Collection.WISHLIST_COMMENT, item.wishListComment)
            if (includePrivateInfo) {
                values.put(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, item.pricePaidCurrency)
                values.put(Collection.PRIVATE_INFO_PRICE_PAID, item.pricePaid)
                values.put(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, item.currentValueCurrency)
                values.put(Collection.PRIVATE_INFO_CURRENT_VALUE, item.currentValue)
                values.put(Collection.PRIVATE_INFO_QUANTITY, item.quantity)
                values.put(Collection.PRIVATE_INFO_ACQUISITION_DATE, item.acquisitionDate.asDateForApi())
                values.put(Collection.PRIVATE_INFO_ACQUIRED_FROM, item.acquiredFrom)
                values.put(Collection.PRIVATE_INFO_COMMENT, item.privateComment)
                values.put(Collection.PRIVATE_INFO_INVENTORY_LOCATION, item.inventoryLocation)
            }
        }
        if (includeStats) {
            values.put(Collection.RATING, item.rating)
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
            Collection.STATUS_OWN,
            Collection.STATUS_PREVIOUSLY_OWNED,
            Collection.STATUS_FOR_TRADE,
            Collection.STATUS_WANT,
            Collection.STATUS_WANT_TO_BUY,
            Collection.STATUS_WISHLIST,
            Collection.STATUS_WANT_TO_PLAY,
            Collection.STATUS_PREORDERED,
            Collection.STATUS_WISHLIST_PRIORITY
        )
        removeValuesIfDirty(values, candidate.ratingDirtyTimestamp, Collection.RATING)
        removeValuesIfDirty(values, candidate.commentDirtyTimestamp, Collection.COMMENT)
        removeValuesIfDirty(
            values, candidate.privateInfoDirtyTimestamp,
            Collection.PRIVATE_INFO_ACQUIRED_FROM,
            Collection.PRIVATE_INFO_ACQUISITION_DATE,
            Collection.PRIVATE_INFO_COMMENT,
            Collection.PRIVATE_INFO_CURRENT_VALUE,
            Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
            Collection.PRIVATE_INFO_PRICE_PAID,
            Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
            Collection.PRIVATE_INFO_QUANTITY,
            Collection.PRIVATE_INFO_INVENTORY_LOCATION
        )
        removeValuesIfDirty(values, candidate.wishListCommentDirtyTimestamp, Collection.WISHLIST_COMMENT)
        removeValuesIfDirty(values, candidate.tradeConditionDirtyTimestamp, Collection.CONDITION)
        removeValuesIfDirty(values, candidate.wantPartsDirtyTimestamp, Collection.WANTPARTS_LIST)
        removeValuesIfDirty(values, candidate.hasPartsDirtyTimestamp, Collection.HASPARTS_LIST)
    }

    private fun removeValuesIfDirty(values: ContentValues, dirtyFlag: Long, vararg columns: String) {
        if (dirtyFlag != NOT_DIRTY) columns.forEach { values.remove(it) }
    }

    private fun maybeDeleteThumbnail(values: ContentValues, uri: Uri) {
        val newThumbnailUrl: String = values.getAsString(Collection.COLLECTION_THUMBNAIL_URL) ?: ""
        val oldThumbnailUrl = resolver.queryString(uri, Collection.COLLECTION_THUMBNAIL_URL) ?: ""
        if (newThumbnailUrl == oldThumbnailUrl) return // nothing to do - thumbnail hasn't changed

        val thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl)
        if (!thumbnailFileName.isNullOrBlank()) {
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
        val hasPartsDirtyTimestamp: Long = 0
    ) {
        companion object {
            private val projection = arrayOf(
                Collection._ID,
                Collection.COLLECTION_DIRTY_TIMESTAMP,
                Collection.STATUS_DIRTY_TIMESTAMP,
                Collection.RATING_DIRTY_TIMESTAMP,
                Collection.COMMENT_DIRTY_TIMESTAMP,
                Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
                Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
                Collection.TRADE_CONDITION_DIRTY_TIMESTAMP,
                Collection.WANT_PARTS_DIRTY_TIMESTAMP,
                Collection.HAS_PARTS_DIRTY_TIMESTAMP
            )

            fun find(resolver: ContentResolver, collectionId: Int, gameId: Int): SyncCandidate {
                if (collectionId != INVALID_ID) {
                    resolver.query(
                        Collection.CONTENT_URI,
                        projection,
                        Collection.COLLECTION_ID + "=?",
                        arrayOf(collectionId.toString()),
                        null
                    )?.use {
                        if (it.moveToFirst()) return fromCursor(it)
                    }
                }
                resolver.query(
                    Collection.CONTENT_URI,
                    projection,
                    "collection.${Collection.GAME_ID}=? AND ${Collection.COLLECTION_ID.whereNullOrBlank()}",
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
            Collection._ID,
            Collection.GAME_ID,
            Collection.COLLECTION_ID,
            Collection.COLLECTION_NAME,
            Collection.COLLECTION_SORT_NAME,
            Collection.COLLECTION_YEAR_PUBLISHED,
            Collection.COLLECTION_THUMBNAIL_URL,
            Collection.COLLECTION_IMAGE_URL,
            Collection.COLLECTION_HERO_IMAGE_URL,
            Collection.STATUS_OWN,
            Collection.STATUS_PREVIOUSLY_OWNED, // 10
            Collection.STATUS_FOR_TRADE,
            Collection.STATUS_WANT,
            Collection.STATUS_WANT_TO_BUY,
            Collection.STATUS_WISHLIST,
            Collection.STATUS_WANT_TO_PLAY,
            Collection.STATUS_PREORDERED,
            Collection.STATUS_WISHLIST_PRIORITY,
            Collection.NUM_PLAYS,
            Collection.COMMENT,
            Collection.YEAR_PUBLISHED, // 20
            Collection.STATS_AVERAGE,
            Collection.RATING,
            Collection.IMAGE_URL,
            Collection.UPDATED,
            Collection.GAME_NAME,
            Collection.COLLECTION_DELETE_TIMESTAMP,
            Collection.COLLECTION_DIRTY_TIMESTAMP,
            Collection.STATUS_DIRTY_TIMESTAMP,
            Collection.RATING_DIRTY_TIMESTAMP,
            Collection.COMMENT_DIRTY_TIMESTAMP,
            Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
            Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
            Collection.TRADE_CONDITION_DIRTY_TIMESTAMP,
            Collection.HAS_PARTS_DIRTY_TIMESTAMP,
            Collection.WANT_PARTS_DIRTY_TIMESTAMP,
            Collection.LAST_MODIFIED,
            Collection.LAST_VIEWED,
            Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
            Collection.PRIVATE_INFO_PRICE_PAID,
            Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
            Collection.PRIVATE_INFO_CURRENT_VALUE,
            Collection.PRIVATE_INFO_QUANTITY,
            Collection.PRIVATE_INFO_ACQUISITION_DATE,
            Collection.PRIVATE_INFO_ACQUIRED_FROM,
            Collection.PRIVATE_INFO_COMMENT,
            Collection.PRIVATE_INFO_INVENTORY_LOCATION,
            Collection.WISHLIST_COMMENT,
            Collection.WANTPARTS_LIST,
            Collection.HASPARTS_LIST,
            Collection.CONDITION,
            Collection.PLAYING_TIME,
            Collection.MINIMUM_AGE,
            Collection.GAME_RANK,
            Collection.STATS_BAYES_AVERAGE,
            Collection.STATS_AVERAGE_WEIGHT,
            Collection.STARRED,
            Plays.MAX_DATE,
            Collection.MIN_PLAYERS,
            Collection.MAX_PLAYERS,
            Collection.SUBTYPE,
            Collection.PLAYER_COUNTS_BEST,
            Collection.PLAYER_COUNTS_RECOMMENDED,
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
