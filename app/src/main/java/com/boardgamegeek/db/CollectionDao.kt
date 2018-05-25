package com.boardgamegeek.db

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.CursorUtils
import com.boardgamegeek.util.FileUtils
import com.boardgamegeek.util.ResolverUtils
import com.boardgamegeek.util.SelectionBuilder
import hugo.weaving.DebugLog
import timber.log.Timber

private const val NOT_DIRTY = 0L

class CollectionDao(context: Context) {
    private val resolver = context.contentResolver

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
        val collectionIdsToDelete = ResolverUtils.queryInts(resolver,
                Collection.CONTENT_URI,
                Collection.COLLECTION_ID,
                "collection.${Collection.GAME_ID}=?",
                arrayOf(gameId.toString()))
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
    fun saveItem(item: CollectionItemEntity, timestamp: Long, includeStats: Boolean = true, includePrivateInfo: Boolean = true, isBrief: Boolean = false): Int {
        val candidate = SyncCandidate.find(resolver, item.collectionId, item.gameId)
        if (candidate.dirtyTimestamp != NOT_DIRTY) {
            Timber.i("Local copy of the collection item is dirty, skipping sync.")
        } else {
            upsertGame(item.gameId, toGameValues(item, includeStats, isBrief, timestamp))
            upsertItem(candidate, toCollectionValues(item, includeStats, includePrivateInfo, isBrief, timestamp), isBrief)
            Timber.i("Saved collection item '%s' [ID=%s, collection ID=%s]", item.gameName, item.gameId, item.collectionId)
        }
        return item.collectionId
    }

    @DebugLog
    private fun toGameValues(item: CollectionItemEntity, includeStats: Boolean, isBrief: Boolean, timestamp: Long): ContentValues {
        val values = ContentValues()
        values.put(Games.UPDATED_LIST, timestamp)
        values.put(Games.GAME_ID, item.gameId)
        values.put(Games.GAME_NAME, item.gameName)
        values.put(Games.GAME_SORT_NAME, item.sortName)
        if (!isBrief) {
            values.put(Games.NUM_PLAYS, item.numberOfPlays)
        }
        if (includeStats) {
            values.put(Games.MIN_PLAYERS, item.minNumberOfPlayers)
            values.put(Games.MAX_PLAYERS, item.maxNumberOfPlayers)
            values.put(Games.PLAYING_TIME, item.playingTime)
            values.put(Games.MIN_PLAYING_TIME, item.minPlayingTime)
            values.put(Games.MAX_PLAYING_TIME, item.maxPlayingTime)
            values.put(Games.STATS_NUMBER_OWNED, item.numberOwned)
        }
        return values
    }

    @DebugLog
    private fun upsertGame(gameId: Int, values: ContentValues) {
        val uri = Games.buildGameUri(gameId)
        if (ResolverUtils.rowExists(resolver, uri)) {
            values.remove(Games.GAME_ID)
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
        if (item.collectionId != BggContract.INVALID_ID) {
            values.put(Collection.COLLECTION_ID, item.collectionId)
        }
        values.put(Collection.COLLECTION_NAME, item.collectionName)
        values.put(Collection.COLLECTION_SORT_NAME, item.sortName)
        values.put(Collection.STATUS_OWN, item.own)
        values.put(Collection.STATUS_PREVIOUSLY_OWNED, item.previouslyOwned)
        values.put(Collection.STATUS_FOR_TRADE, item.forTrade)
        values.put(Collection.STATUS_WANT, item.want)
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
            }
        }
        if (includeStats) {
            values.put(Collection.RATING, item.rating)
        }
        return values
    }

    @DebugLog
    private fun upsertItem(candidate: SyncCandidate, values: ContentValues, isBrief: Boolean) {
        if (candidate.internalId != BggContract.INVALID_ID.toLong()) {
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
                Collection.PRIVATE_INFO_QUANTITY)
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
        val oldThumbnailUrl = ResolverUtils.queryString(resolver, uri, Collection.COLLECTION_THUMBNAIL_URL) ?: ""
        if (newThumbnailUrl == oldThumbnailUrl) return // nothing to do - thumbnail hasn't changed

        val thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl)
        if (!thumbnailFileName.isNullOrBlank()) {
            resolver.delete(Thumbnails.buildUri(thumbnailFileName), null, null)
        }
    }

    internal class SyncCandidate(
            val internalId: Long = BggContract.INVALID_ID.toLong(),
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
                if (collectionId != BggContract.INVALID_ID) {
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
                        CursorUtils.getLong(cursor, Collection._ID, INVALID_ID.toLong()),
                        CursorUtils.getLong(cursor, Collection.COLLECTION_DIRTY_TIMESTAMP),
                        CursorUtils.getLong(cursor, Collection.STATUS_DIRTY_TIMESTAMP),
                        CursorUtils.getLong(cursor, Collection.RATING_DIRTY_TIMESTAMP),
                        CursorUtils.getLong(cursor, Collection.COMMENT_DIRTY_TIMESTAMP),
                        CursorUtils.getLong(cursor, Collection.PRIVATE_INFO_DIRTY_TIMESTAMP),
                        CursorUtils.getLong(cursor, Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP),
                        CursorUtils.getLong(cursor, Collection.TRADE_CONDITION_DIRTY_TIMESTAMP),
                        CursorUtils.getLong(cursor, Collection.WANT_PARTS_DIRTY_TIMESTAMP),
                        CursorUtils.getLong(cursor, Collection.HAS_PARTS_DIRTY_TIMESTAMP)
                )
            }
        }
    }
}
