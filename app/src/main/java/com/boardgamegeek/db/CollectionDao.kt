package com.boardgamegeek.db

import android.content.ContentResolver
import android.content.ContentValues
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.*
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.AbsentLiveData
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.util.SelectionBuilder
import hugo.weaving.DebugLog
import timber.log.Timber

class CollectionDao(private val context: BggApplication) {
    private val resolver = context.contentResolver
    private val prefs: SharedPreferences by lazy { context.preferences() }

    fun loadAsLiveData(collectionId: Int): LiveData<CollectionItemEntity> {
        return RegisteredLiveData(context, Collection.CONTENT_URI, true) {
            return@RegisteredLiveData load(collectionId)
        }
    }

    fun load(collectionId: Int): CollectionItemEntity? {
        val uri = Collection.CONTENT_URI
        return resolver.load(uri,
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
                        list.add(item)
                } while (it.moveToNext())
            }
        }
        return list
    }

    private fun projection(): Array<String> {
        return arrayOf(
                Collection._ID,
                Collection.GAME_ID,
                Collection.COLLECTION_ID,
                Collection.COLLECTION_NAME,
                Collection.COLLECTION_YEAR_PUBLISHED,
                Collection.COLLECTION_THUMBNAIL_URL,
                Collection.COLLECTION_IMAGE_URL,
                Collection.COLLECTION_HERO_IMAGE_URL,
                Collection.STATUS_OWN,
                Collection.STATUS_PREVIOUSLY_OWNED,
                Collection.STATUS_FOR_TRADE,
                Collection.STATUS_WANT,
                Collection.STATUS_WANT_TO_BUY,
                Collection.STATUS_WISHLIST,
                Collection.STATUS_WANT_TO_PLAY,
                Collection.STATUS_PREORDERED,
                Collection.STATUS_WISHLIST_PRIORITY,
                Collection.NUM_PLAYS,
                Collection.COMMENT,
                Collection.YEAR_PUBLISHED,
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
                Collection.CONDITION
        )
    }

    private fun entityFromCursor(cursor: Cursor): CollectionItemEntity {
        return CollectionItemEntity(
                internalId = cursor.getLong(Collection._ID),
                gameId = cursor.getInt(Collection.GAME_ID),
                collectionId = cursor.getInt(Collection.COLLECTION_ID),
                collectionName = cursor.getStringOrEmpty(Collection.COLLECTION_NAME),
                gameName = cursor.getStringOrEmpty(Collection.GAME_NAME),
                yearPublished = cursor.getIntOrNull(Collection.COLLECTION_YEAR_PUBLISHED)
                        ?: YEAR_UNKNOWN,
                imageUrl = cursor.getStringOrEmpty(Collection.COLLECTION_IMAGE_URL),
                thumbnailUrl = cursor.getStringOrEmpty(Collection.COLLECTION_THUMBNAIL_URL),
                heroImageUrl = cursor.getStringOrEmpty(Collection.COLLECTION_HERO_IMAGE_URL),
                comment = cursor.getStringOrEmpty(Collection.COMMENT),
                numberOfPlays = cursor.getIntOrZero(Games.NUM_PLAYS),
                rating = cursor.getDoubleOrZero(Collection.RATING),
                syncTimestamp = cursor.getLongOrZero(Collection.UPDATED),
                lastModifiedDate = cursor.getLongOrZero(Collection.LAST_MODIFIED),
                deleteTimestamp = cursor.getLongOrZero(Collection.COLLECTION_DELETE_TIMESTAMP),
                own = cursor.getBoolean(Collection.STATUS_OWN),
                previouslyOwned = cursor.getBoolean(Collection.STATUS_PREVIOUSLY_OWNED),
                preOrdered = cursor.getBoolean(Collection.STATUS_PREORDERED),
                forTrade = cursor.getBoolean(Collection.STATUS_FOR_TRADE),
                wantInTrade = cursor.getBoolean(Collection.STATUS_WANT),
                wantToPlay = cursor.getBoolean(Collection.STATUS_WANT_TO_PLAY),
                wantToBuy = cursor.getBoolean(Collection.STATUS_WANT_TO_BUY),
                wishList = cursor.getBoolean(Collection.STATUS_WISHLIST),
                wishListPriority = cursor.getIntOrNull(Collection.STATUS_WISHLIST_PRIORITY)
                        ?: WISHLIST_PRIORITY_UNKNOWN,
                dirtyTimestamp = cursor.getLongOrZero(Collection.COLLECTION_DIRTY_TIMESTAMP),
                statusDirtyTimestamp = cursor.getLongOrZero(Collection.STATUS_DIRTY_TIMESTAMP),
                ratingDirtyTimestamp = cursor.getLongOrZero(Collection.RATING_DIRTY_TIMESTAMP),
                commentDirtyTimestamp = cursor.getLongOrZero(Collection.COMMENT_DIRTY_TIMESTAMP),
                privateInfoDirtyTimestamp = cursor.getLongOrZero(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP),
                wishListDirtyTimestamp = cursor.getLongOrZero(Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP),
                tradeConditionDirtyTimestamp = cursor.getLongOrZero(Collection.TRADE_CONDITION_DIRTY_TIMESTAMP),
                hasPartsDirtyTimestamp = cursor.getLongOrZero(Collection.HAS_PARTS_DIRTY_TIMESTAMP),
                wantPartsDirtyTimestamp = cursor.getLongOrZero(Collection.WANT_PARTS_DIRTY_TIMESTAMP),
                quantity = cursor.getIntOrZero(Collection.PRIVATE_INFO_QUANTITY),
                pricePaid = cursor.getDoubleOrZero(Collection.PRIVATE_INFO_PRICE_PAID),
                pricePaidCurrency = cursor.getString(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY),
                currentValue = cursor.getDoubleOrZero(Collection.PRIVATE_INFO_CURRENT_VALUE),
                currentValueCurrency = cursor.getString(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY),
                acquisitionDate = cursor.getString(Collection.PRIVATE_INFO_ACQUISITION_DATE),
                acquiredFrom = cursor.getString(Collection.PRIVATE_INFO_ACQUIRED_FROM),
                inventoryLocation = cursor.getString(Collection.PRIVATE_INFO_INVENTORY_LOCATION),
                privateComment = cursor.getString(Collection.PRIVATE_INFO_COMMENT),
                wishListComment = cursor.getString(Collection.WISHLIST_COMMENT),
                wantPartsList = cursor.getString(Collection.WANTPARTS_LIST),
                hasPartsList = cursor.getString(Collection.HASPARTS_LIST),
                conditionText = cursor.getString(Collection.CONDITION)
        )
    }

    fun loadByGame(gameId: Int, includeDeletedItems: Boolean = false): LiveData<List<CollectionItemEntity>> {
        if (gameId == INVALID_ID) return AbsentLiveData.create()
        val uri = Collection.CONTENT_URI
        val projection = arrayOf(
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
                Collection.STATUS_PREVIOUSLY_OWNED,
                Collection.STATUS_FOR_TRADE,
                Collection.STATUS_WANT,
                Collection.STATUS_WANT_TO_BUY,
                Collection.STATUS_WISHLIST,
                Collection.STATUS_WANT_TO_PLAY,
                Collection.STATUS_PREORDERED,
                Collection.STATUS_WISHLIST_PRIORITY,
                Collection.NUM_PLAYS,
                Collection.COMMENT,
                Collection.YEAR_PUBLISHED,
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
                Collection.PRIVATE_INFO_PRICE_PAID,
                Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
                Collection.PRIVATE_INFO_CURRENT_VALUE,
                Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
                Collection.PRIVATE_INFO_ACQUIRED_FROM,
                Collection.PRIVATE_INFO_ACQUISITION_DATE,
                Collection.PRIVATE_INFO_QUANTITY,
                Collection.PRIVATE_INFO_INVENTORY_LOCATION,
                Collection.PRIVATE_INFO_COMMENT,
                Games.WINS_COLOR,
                Games.WINNABLE_PLAYS_COLOR,
                Games.ALL_PLAYS_COLOR,
                Games.PLAYING_TIME
        )
        return RegisteredLiveData(context, uri, true) {
            val list = arrayListOf<CollectionItemEntity>()
            resolver.load(
                    uri,
                    projection,
                    "collection.${Collection.GAME_ID}=?",
                    arrayOf(gameId.toString()))?.use {
                if (it.moveToFirst()) {
                    do {
                        val item = CollectionItemEntity(
                                internalId = it.getLong(Collection._ID),
                                gameId = it.getInt(Collection.GAME_ID),
                                collectionId = it.getIntOrNull(Collection.COLLECTION_ID)
                                        ?: INVALID_ID,
                                collectionName = it.getStringOrEmpty(Collection.COLLECTION_NAME),
                                sortName = it.getStringOrEmpty(Collection.COLLECTION_SORT_NAME),
                                gameName = it.getStringOrEmpty(Collection.GAME_NAME),
                                yearPublished = it.getIntOrNull(Collection.COLLECTION_YEAR_PUBLISHED)
                                        ?: YEAR_UNKNOWN,
                                imageUrl = it.getStringOrEmpty(Collection.COLLECTION_IMAGE_URL),
                                thumbnailUrl = it.getStringOrEmpty(Collection.COLLECTION_THUMBNAIL_URL),
                                heroImageUrl = it.getStringOrEmpty(Collection.COLLECTION_HERO_IMAGE_URL),
                                comment = it.getStringOrEmpty(Collection.COMMENT),
                                numberOfPlays = it.getIntOrZero(Games.NUM_PLAYS),
                                rating = it.getDoubleOrZero(Collection.RATING),
                                syncTimestamp = it.getLongOrZero(Collection.UPDATED),
                                deleteTimestamp = it.getLongOrZero(Collection.COLLECTION_DELETE_TIMESTAMP),
                                own = it.getBoolean(Collection.STATUS_OWN),
                                previouslyOwned = it.getBoolean(Collection.STATUS_PREVIOUSLY_OWNED),
                                preOrdered = it.getBoolean(Collection.STATUS_PREORDERED),
                                forTrade = it.getBoolean(Collection.STATUS_FOR_TRADE),
                                wantInTrade = it.getBoolean(Collection.STATUS_WANT),
                                wantToPlay = it.getBoolean(Collection.STATUS_WANT_TO_PLAY),
                                wantToBuy = it.getBoolean(Collection.STATUS_WANT_TO_BUY),
                                wishList = it.getBoolean(Collection.STATUS_WISHLIST),
                                wishListPriority = it.getIntOrNull(Collection.STATUS_WISHLIST_PRIORITY)
                                        ?: WISHLIST_PRIORITY_UNKNOWN,
                                dirtyTimestamp = it.getLongOrZero(Collection.COLLECTION_DIRTY_TIMESTAMP),
                                statusDirtyTimestamp = it.getLongOrZero(Collection.STATUS_DIRTY_TIMESTAMP),
                                ratingDirtyTimestamp = it.getLongOrZero(Collection.RATING_DIRTY_TIMESTAMP),
                                commentDirtyTimestamp = it.getLongOrZero(Collection.COMMENT_DIRTY_TIMESTAMP),
                                privateInfoDirtyTimestamp = it.getLongOrZero(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP),
                                wishListDirtyTimestamp = it.getLongOrZero(Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP),
                                tradeConditionDirtyTimestamp = it.getLongOrZero(Collection.TRADE_CONDITION_DIRTY_TIMESTAMP),
                                hasPartsDirtyTimestamp = it.getLongOrZero(Collection.HAS_PARTS_DIRTY_TIMESTAMP),
                                wantPartsDirtyTimestamp = it.getLongOrZero(Collection.WANT_PARTS_DIRTY_TIMESTAMP),
                                pricePaid = it.getDoubleOrZero(Collection.PRIVATE_INFO_PRICE_PAID),
                                pricePaidCurrency = it.getStringOrEmpty(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY),
                                currentValue = it.getDoubleOrZero(Collection.PRIVATE_INFO_CURRENT_VALUE),
                                currentValueCurrency = it.getStringOrEmpty(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY),
                                quantity = it.getIntOrZero(Collection.PRIVATE_INFO_QUANTITY),
                                acquiredFrom = it.getStringOrEmpty(Collection.PRIVATE_INFO_ACQUIRED_FROM),
                                acquisitionDate = it.getStringOrEmpty(Collection.PRIVATE_INFO_ACQUISITION_DATE),
                                inventoryLocation = it.getStringOrEmpty(Collection.PRIVATE_INFO_INVENTORY_LOCATION),
                                privateComment = it.getStringOrEmpty(Collection.PRIVATE_INFO_COMMENT),
                                winsColor = it.getIntOrZero(Games.WINS_COLOR),
                                winnablePlaysColor = it.getIntOrZero(Games.WINNABLE_PLAYS_COLOR),
                                allPlaysColor = it.getIntOrZero(Games.ALL_PLAYS_COLOR),
                                playingTime = it.getIntOrZero(Games.PLAYING_TIME)
                        )
                        if (includeDeletedItems || item.deleteTimestamp == 0L)
                            list.add(item)
                    } while (it.moveToNext())
                }
            }
            return@RegisteredLiveData list
        }
    }

    enum class SortType {
        NAME, RATING
    }

    fun loadLinkedCollection(uri: Uri, sortBy: SortType = SortType.RATING): List<BriefGameEntity> {
        val list = arrayListOf<BriefGameEntity>()

        val selection = StringBuilder()
        val statuses = prefs.getSyncStatusesOrDefault()
        for (status in statuses) {
            if (status.isBlank()) continue
            if (selection.isNotBlank()) selection.append(" OR ")
            selection.append(when (status) {
                COLLECTION_STATUS_OWN -> Collection.STATUS_OWN.isTrue()
                COLLECTION_STATUS_PREVIOUSLY_OWNED -> Collection.STATUS_PREVIOUSLY_OWNED.isTrue()
                COLLECTION_STATUS_PREORDERED -> Collection.STATUS_PREORDERED.isTrue()
                COLLECTION_STATUS_FOR_TRADE -> Collection.STATUS_FOR_TRADE.isTrue()
                COLLECTION_STATUS_WANT -> Collection.STATUS_WANT.isTrue()
                COLLECTION_STATUS_WANT_TO_BUY -> Collection.STATUS_WANT_TO_BUY.isTrue()
                COLLECTION_STATUS_WANT_TO_PLAY -> Collection.STATUS_WANT_TO_PLAY.isTrue()
                COLLECTION_STATUS_WISHLIST -> Collection.STATUS_WISHLIST.isTrue()
                COLLECTION_STATUS_RATED -> Collection.RATING.greaterThanZero()
                COLLECTION_STATUS_PLAYED -> Collection.NUM_PLAYS.greaterThanZero()
                COLLECTION_STATUS_COMMENTED -> Collection.COMMENT.notBlank()
                COLLECTION_STATUS_HAS_PARTS -> Collection.HASPARTS_LIST.notBlank()
                COLLECTION_STATUS_WANT_PARTS -> Collection.WANTPARTS_LIST.notBlank()
                else -> ""
            })
        }

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
                selection.toString(),
                emptyArray(),
                sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list += BriefGameEntity(
                            it.getLong(Collection._ID),
                            it.getInt(Collection.GAME_ID),
                            it.getStringOrEmpty(Collection.GAME_NAME),
                            it.getStringOrEmpty(Collection.COLLECTION_NAME),
                            it.getIntOrNull(Collection.YEAR_PUBLISHED) ?: YEAR_UNKNOWN,
                            it.getIntOrNull(Collection.COLLECTION_YEAR_PUBLISHED) ?: YEAR_UNKNOWN,
                            it.getStringOrEmpty(Collection.COLLECTION_THUMBNAIL_URL),
                            it.getStringOrEmpty(Collection.THUMBNAIL_URL),
                            it.getStringOrEmpty(Collection.HERO_IMAGE_URL),
                            it.getDoubleOrZero(Collection.RATING),
                            it.getBoolean(Collection.STARRED),
                            it.getStringOrEmpty(Collection.SUBTYPE),
                            it.getIntOrZero(Collection.NUM_PLAYS)
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun update(internalId: Long, values: ContentValues): Int {
        return resolver.update(Collection.buildUri(internalId), values, null, null)
    }

    /**
     * Remove all collection items belonging to a game, except the ones in the specified list.
     *
     * @param gameId                 delete collection items with this game ID.
     * @param protectedCollectionIds list of collection IDs not to delete.
     * @return the number or rows deleted.
     */
    @DebugLog
    fun delete(gameId: Int, protectedCollectionIds: List<Int>): Int {
        // determine the collection IDs that are no longer in the collection
        val collectionIdsToDelete = resolver.queryInts(
                Collection.CONTENT_URI,
                Collection.COLLECTION_ID,
                "collection.${Collection.GAME_ID}=?",
                arrayOf(gameId.toString()))
                .toMutableList()
        collectionIdsToDelete.removeAll(protectedCollectionIds)
        // remove them
        if (collectionIdsToDelete.size > 0) {
            for (collectionId in collectionIdsToDelete) {
                resolver.delete(Collection.CONTENT_URI,
                        "${Collection.COLLECTION_ID}=?",
                        arrayOf(collectionId.toString()))
            }
        }

        return collectionIdsToDelete.size
    }

    @DebugLog
    fun saveItem(item: CollectionItemEntity, game: CollectionItemGameEntity, timestamp: Long, includeStats: Boolean = true, includePrivateInfo: Boolean = true, isBrief: Boolean = false): Int {
        val candidate = SyncCandidate.find(resolver, item.collectionId, item.gameId)
        if (candidate.dirtyTimestamp != NOT_DIRTY) {
            Timber.i("Local copy of the collection item is dirty, skipping sync.")
        } else {
            upsertGame(item.gameId, toGameValues(game, includeStats, isBrief, timestamp), isBrief)
            upsertItem(candidate, toCollectionValues(item, includeStats, includePrivateInfo, isBrief, timestamp), isBrief)
            Timber.i("Saved collection item '%s' [ID=%s, collection ID=%s]", item.gameName, item.gameId, item.collectionId)
        }
        return item.collectionId
    }

    @DebugLog
    private fun toGameValues(game: CollectionItemGameEntity, includeStats: Boolean, isBrief: Boolean, timestamp: Long): ContentValues {
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

    @DebugLog
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

    @DebugLog
    private fun toCollectionValues(item: CollectionItemEntity, includeStats: Boolean, includePrivateInfo: Boolean, isBrief: Boolean, timestamp: Long): ContentValues {
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
            values.put(Collection.COLLECTION_YEAR_PUBLISHED, item.yearPublished)
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
                values.put(Collection.PRIVATE_INFO_ACQUISITION_DATE, item.acquisitionDate)
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

    @DebugLog
    private fun upsertItem(candidate: SyncCandidate, values: ContentValues, isBrief: Boolean) {
        if (candidate.internalId != INVALID_ID.toLong()) {
            removeDirtyValues(values, candidate)
            val uri = Collection.buildUri(candidate.internalId)
            if (!isBrief) maybeDeleteThumbnail(values, uri)
            resolver.update(uri, values, null, null)
        } else {
            resolver.insert(Collection.CONTENT_URI, values)
        }
    }

    @DebugLog
    private fun removeDirtyValues(values: ContentValues, candidate: SyncCandidate) {
        removeValuesIfDirty(values, candidate.statusDirtyTimestamp,
                Collection.STATUS_OWN,
                Collection.STATUS_PREVIOUSLY_OWNED,
                Collection.STATUS_FOR_TRADE,
                Collection.STATUS_WANT,
                Collection.STATUS_WANT_TO_BUY,
                Collection.STATUS_WISHLIST,
                Collection.STATUS_WANT_TO_PLAY,
                Collection.STATUS_PREORDERED,
                Collection.STATUS_WISHLIST_PRIORITY)
        removeValuesIfDirty(values, candidate.ratingDirtyTimestamp, Collection.RATING)
        removeValuesIfDirty(values, candidate.commentDirtyTimestamp, Collection.COMMENT)
        removeValuesIfDirty(values, candidate.privateInfoDirtyTimestamp,
                Collection.PRIVATE_INFO_ACQUIRED_FROM,
                Collection.PRIVATE_INFO_ACQUISITION_DATE,
                Collection.PRIVATE_INFO_COMMENT,
                Collection.PRIVATE_INFO_CURRENT_VALUE,
                Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
                Collection.PRIVATE_INFO_PRICE_PAID,
                Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
                Collection.PRIVATE_INFO_QUANTITY,
                Collection.PRIVATE_INFO_INVENTORY_LOCATION)
        removeValuesIfDirty(values, candidate.wishListCommentDirtyTimestamp, Collection.WISHLIST_COMMENT)
        removeValuesIfDirty(values, candidate.tradeConditionDirtyTimestamp, Collection.CONDITION)
        removeValuesIfDirty(values, candidate.wantPartsDirtyTimestamp, Collection.WANTPARTS_LIST)
        removeValuesIfDirty(values, candidate.hasPartsDirtyTimestamp, Collection.HASPARTS_LIST)
    }

    @DebugLog
    private fun removeValuesIfDirty(values: ContentValues, dirtyFlag: Long, vararg columns: String) {
        if (dirtyFlag != NOT_DIRTY) columns.forEach { values.remove(it) }
    }

    @DebugLog
    private fun maybeDeleteThumbnail(values: ContentValues, uri: Uri) {
        val newThumbnailUrl: String = values.getAsString(Collection.COLLECTION_THUMBNAIL_URL) ?: ""
        val oldThumbnailUrl = resolver.queryString(uri, Collection.COLLECTION_THUMBNAIL_URL) ?: ""
        if (newThumbnailUrl == oldThumbnailUrl) return // nothing to do - thumbnail hasn't changed

        val thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl)
        if (!thumbnailFileName.isNullOrBlank()) {
            resolver.delete(Thumbnails.buildUri(thumbnailFileName), null, null)
        }
    }

    internal class SyncCandidate(
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
            val PROJECTION = arrayOf(Collection._ID, Collection.COLLECTION_DIRTY_TIMESTAMP, Collection.STATUS_DIRTY_TIMESTAMP, Collection.RATING_DIRTY_TIMESTAMP, Collection.COMMENT_DIRTY_TIMESTAMP, Collection.PRIVATE_INFO_DIRTY_TIMESTAMP, Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP, Collection.TRADE_CONDITION_DIRTY_TIMESTAMP, Collection.WANT_PARTS_DIRTY_TIMESTAMP, Collection.HAS_PARTS_DIRTY_TIMESTAMP)

            fun find(resolver: ContentResolver, collectionId: Int, gameId: Int): SyncCandidate {
                if (collectionId != INVALID_ID) {
                    resolver.query(Collection.CONTENT_URI,
                            PROJECTION,
                            Collection.COLLECTION_ID + "=?",
                            arrayOf(collectionId.toString()),
                            null)?.use {
                        if (it.moveToFirst()) return fromCursor(it)
                    }
                }
                resolver.query(Collection.CONTENT_URI,
                        PROJECTION,
                        "collection.${Collection.GAME_ID}=? AND ${SelectionBuilder.whereNullOrEmpty(Collection.COLLECTION_ID)}",
                        arrayOf(gameId.toString()),
                        null)?.use {
                    if (it.moveToFirst()) return fromCursor(it)
                }
                return SyncCandidate()
            }

            fun fromCursor(cursor: Cursor): SyncCandidate {
                return SyncCandidate(
                        cursor.getLongOrNull(Collection._ID) ?: INVALID_ID.toLong(),
                        cursor.getLongOrZero(Collection.COLLECTION_DIRTY_TIMESTAMP),
                        cursor.getLongOrZero(Collection.STATUS_DIRTY_TIMESTAMP),
                        cursor.getLongOrZero(Collection.RATING_DIRTY_TIMESTAMP),
                        cursor.getLongOrZero(Collection.COMMENT_DIRTY_TIMESTAMP),
                        cursor.getLongOrZero(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP),
                        cursor.getLongOrZero(Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP),
                        cursor.getLongOrZero(Collection.TRADE_CONDITION_DIRTY_TIMESTAMP),
                        cursor.getLongOrZero(Collection.WANT_PARTS_DIRTY_TIMESTAMP),
                        cursor.getLongOrZero(Collection.HAS_PARTS_DIRTY_TIMESTAMP)
                )
            }
        }
    }

    companion object {
        private const val NOT_DIRTY = 0L
    }
}
