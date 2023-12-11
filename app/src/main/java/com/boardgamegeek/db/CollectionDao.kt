package com.boardgamegeek.db

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import com.boardgamegeek.db.model.CollectionItemGame
import com.boardgamegeek.db.model.CollectionItemLocal
import com.boardgamegeek.db.model.GameEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import com.boardgamegeek.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class CollectionDao(private val context: Context) {
    private val resolver = context.contentResolver

    suspend fun delete(internalId: Long) = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Collection.buildUri(internalId), null, null)
    }

    /**
     * Remove all collection items belonging to a game, except the ones in the specified list.
     *
     * @param gameId                 delete collection items with this game ID.
     * @param protectedCollectionIds list of collection IDs not to delete.
     * @return the number or rows deleted.
     */
    suspend fun delete(gameId: Int, protectedCollectionIds: List<Int> = emptyList()): Int = withContext(Dispatchers.IO) {
        // determine the collection IDs that are no longer in the collection
        val collectionIdsToDelete = resolver.queryInts(
            Collection.CONTENT_URI,
            Collection.Columns.COLLECTION_ID,
            "collection.${Collection.Columns.GAME_ID}=?",
            arrayOf(gameId.toString()),
            valueIfNull = INVALID_ID,
        ).toMutableList()
        collectionIdsToDelete.removeAll(protectedCollectionIds.toSet())
        collectionIdsToDelete.removeAll(setOf(INVALID_ID))
        // delete them
        var numberOfDeletedRows = 0
        if (collectionIdsToDelete.isNotEmpty()) {
            for (collectionId in collectionIdsToDelete) {
                numberOfDeletedRows += resolver.delete(
                    Collection.CONTENT_URI,
                    "${Collection.Columns.COLLECTION_ID}=?",
                    arrayOf(collectionId.toString())
                )
            }
        }
        numberOfDeletedRows
    }

    suspend fun deleteUnupdatedItems(timestamp: Long): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(
            Collection.CONTENT_URI,
            "${Collection.Columns.UPDATED_LIST}<?",
            arrayOf(timestamp.toString())
        ).also { count ->
            Timber.d("Deleted $count old collection items")
        }
    }

    suspend fun saveItem(item: CollectionItemLocal, game: CollectionItemGame): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var internalId = INVALID_ID.toLong()
        val candidate = SyncCandidate.find(resolver, item.collectionId, item.gameId)
        if (candidate.dirtyTimestamp != NOT_DIRTY) {
            Timber.i("Local copy of the collection item is dirty, skipping sync.")
        } else {
            upsertGame(item.gameId, game.toGameValues())
            internalId = upsertItem(item.toCollectionValues(), candidate)
            Timber.i(
                "Saved collection item '%s' [ID=%s, collection ID=%s]",
                item.collectionName,
                item.gameId,
                item.collectionId
            )
        }
        item.collectionId to internalId
    }

    private fun CollectionItemGame.toGameValues() = contentValuesOf(
        Games.Columns.GAME_ID to gameId,
        Games.Columns.GAME_NAME to gameName,
        Games.Columns.GAME_SORT_NAME to sortName,
        Games.Columns.YEAR_PUBLISHED to yearPublished,
        Games.Columns.IMAGE_URL to imageUrl,
        Games.Columns.THUMBNAIL_URL to thumbnailUrl,
        Games.Columns.MIN_PLAYERS to minNumberOfPlayers,
        Games.Columns.MAX_PLAYERS to maxNumberOfPlayers,
        Games.Columns.PLAYING_TIME to playingTime,
        Games.Columns.MIN_PLAYING_TIME to minPlayingTime,
        Games.Columns.MAX_PLAYING_TIME to maxPlayingTime,
        Games.Columns.STATS_NUMBER_OWNED to numberOwned,
        Games.Columns.STATS_AVERAGE to rating,
        Games.Columns.STATS_USERS_RATED to numberOfUsersRated,
        Games.Columns.STATS_AVERAGE to average,
        Games.Columns.STATS_BAYES_AVERAGE to bayesAverage,
        Games.Columns.STATS_STANDARD_DEVIATION to standardDeviation,
        Games.Columns.STATS_MEDIAN to median,
        Games.Columns.NUM_PLAYS to numberOfPlays,
        Games.Columns.UPDATED_LIST to updatedListTimestamp,
    )

    private fun upsertGame(gameId: Int, values: ContentValues) { // TODO move to GameDao?
        val uri = Games.buildGameUri(gameId)
        if (resolver.rowExists(uri)) {
            values.remove(Games.Columns.GAME_ID)
            resolver.update(uri, values, null, null)
        } else {
            resolver.insert(Games.CONTENT_URI, values)
        }
    }

    private fun CollectionItemLocal.toCollectionValues() = contentValuesOf(
        Collection.Columns.UPDATED to updatedTimestamp,
        Collection.Columns.UPDATED_LIST to updatedListTimestamp,
        Collection.Columns.GAME_ID to gameId,
        Collection.Columns.COLLECTION_NAME to collectionName,
        Collection.Columns.COLLECTION_SORT_NAME to collectionSortName,
        Collection.Columns.STATUS_OWN to statusOwn,
        Collection.Columns.STATUS_PREVIOUSLY_OWNED to statusPreviouslyOwned,
        Collection.Columns.STATUS_FOR_TRADE to statusForTrade,
        Collection.Columns.STATUS_WANT to statusWant,
        Collection.Columns.STATUS_WANT_TO_PLAY to statusWantToPlay,
        Collection.Columns.STATUS_WANT_TO_BUY to statusWantToBuy,
        Collection.Columns.STATUS_WISHLIST to statusWishlist,
        Collection.Columns.STATUS_WISHLIST_PRIORITY to statusWishlistPriority,
        Collection.Columns.STATUS_PREORDERED to statusPreordered,
        Collection.Columns.LAST_MODIFIED to lastModified,
        Collection.Columns.COLLECTION_YEAR_PUBLISHED to collectionYearPublished,
        Collection.Columns.COLLECTION_IMAGE_URL to collectionImageUrl,
        Collection.Columns.COLLECTION_THUMBNAIL_URL to collectionThumbnailUrl,
        Collection.Columns.COMMENT to comment,
        Collection.Columns.CONDITION to condition,
        Collection.Columns.WANTPARTS_LIST to wantpartsList,
        Collection.Columns.HASPARTS_LIST to haspartsList,
        Collection.Columns.WISHLIST_COMMENT to wishlistComment,
        Collection.Columns.PRIVATE_INFO_PRICE_PAID_CURRENCY to privateInfoPricePaidCurrency,
        Collection.Columns.PRIVATE_INFO_PRICE_PAID to privateInfoPricePaid,
        Collection.Columns.PRIVATE_INFO_CURRENT_VALUE_CURRENCY to privateInfoCurrentValueCurrency,
        Collection.Columns.PRIVATE_INFO_CURRENT_VALUE to privateInfoCurrentValue,
        Collection.Columns.PRIVATE_INFO_QUANTITY to privateInfoQuantity,
        Collection.Columns.PRIVATE_INFO_ACQUISITION_DATE to privateInfoAcquisitionDate,
        Collection.Columns.PRIVATE_INFO_ACQUIRED_FROM to privateInfoAcquiredFrom,
        Collection.Columns.PRIVATE_INFO_COMMENT to privateInfoComment,
        Collection.Columns.PRIVATE_INFO_INVENTORY_LOCATION to privateInfoInventoryLocation,
        Collection.Columns.RATING to rating,
    ).apply {
        if (collectionId != INVALID_ID) {
            put(Collection.Columns.COLLECTION_ID, collectionId)
        }
    }

    suspend fun addNewCollectionItem(
        gameId: Int,
        game: GameEntity?,
        statuses: List<String>,
        wishListPriority: Int,
        timestamp: Long = System.currentTimeMillis(),
    ): Long {
        return if (gameId != INVALID_ID) {
            val values = contentValuesOf(
                Collection.Columns.GAME_ID to gameId,
                Collection.Columns.COLLECTION_DIRTY_TIMESTAMP to timestamp,
                Collection.Columns.STATUS_DIRTY_TIMESTAMP to timestamp,
            )
            values.putStatusValue(statuses, Collection.Columns.STATUS_OWN)
            values.putStatusValue(statuses, Collection.Columns.STATUS_PREORDERED)
            values.putStatusValue(statuses, Collection.Columns.STATUS_FOR_TRADE)
            values.putStatusValue(statuses, Collection.Columns.STATUS_WANT)
            values.putStatusValue(statuses, Collection.Columns.STATUS_WANT_TO_PLAY)
            values.putStatusValue(statuses, Collection.Columns.STATUS_WANT_TO_BUY)
            values.putStatusValue(statuses, Collection.Columns.STATUS_WISHLIST)
            values.putStatusValue(statuses, Collection.Columns.STATUS_PREVIOUSLY_OWNED)
            if (statuses.contains(Collection.Columns.STATUS_WISHLIST)) {
                values.put(Collection.Columns.STATUS_WISHLIST, 1)
                values.put(Collection.Columns.STATUS_WISHLIST_PRIORITY, wishListPriority)
            } else {
                values.put(Collection.Columns.STATUS_WISHLIST, 0)
            }
            game?.let {
                values.put(Collection.Columns.COLLECTION_NAME, it.gameName)
                values.put(Collection.Columns.COLLECTION_SORT_NAME, it.gameSortName)
                values.put(Collection.Columns.COLLECTION_YEAR_PUBLISHED, it.yearPublished)
                values.put(Collection.Columns.COLLECTION_IMAGE_URL, it.imageUrl)
                values.put(Collection.Columns.COLLECTION_THUMBNAIL_URL, it.thumbnailUrl)
                values.put(Collection.Columns.COLLECTION_HERO_IMAGE_URL, it.heroImageUrl)
                it.gameName
            }
            upsertItem(values)
        } else INVALID_ID.toLong()
    }

    private fun ContentValues.putStatusValue(statuses: List<String>, statusColumn: String) {
        put(statusColumn, if (statuses.contains(statusColumn)) 1 else 0)
    }

    private suspend fun upsertItem(
        values: ContentValues,
        candidate: SyncCandidate = SyncCandidate()
    ): Long =
        withContext(Dispatchers.IO) {
            if (candidate.internalId != INVALID_ID.toLong()) {
                removeDirtyValues(values, candidate)
                val uri = Collection.buildUri(candidate.internalId)
                maybeDeleteThumbnail(values, uri)
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
        values.getAsString(Collection.Columns.COLLECTION_THUMBNAIL_URL)?.let { newThumbnailUrl ->
            val oldThumbnailUrl = resolver.queryString(uri, Collection.Columns.COLLECTION_THUMBNAIL_URL).orEmpty()
            if (newThumbnailUrl != oldThumbnailUrl) {
                val thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl)
                if (thumbnailFileName.isNotBlank()) {
                    resolver.delete(Thumbnails.buildUri(thumbnailFileName), null, null)
                }
            }
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

    companion object {
        private const val NOT_DIRTY = 0L
    }
}
