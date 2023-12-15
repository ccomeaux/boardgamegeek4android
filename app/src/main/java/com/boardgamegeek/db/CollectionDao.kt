package com.boardgamegeek.db

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import com.boardgamegeek.db.model.GameEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.provider.BggContract.Companion.INVALID_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionDao(context: Context) {
    private val resolver = context.contentResolver

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
                //removeDirtyValues(values, candidate)
                val uri = Collection.buildUri(candidate.internalId)
                //maybeDeleteThumbnail(values, uri)
                resolver.update(uri, values, null, null)
                candidate.internalId
            } else {
                val url = resolver.insert(Collection.CONTENT_URI, values)
                url?.lastPathSegment?.toLongOrNull() ?: INVALID_ID.toLong()
            }
        }

    class SyncCandidate(
        val internalId: Long = INVALID_ID.toLong(),
        val dirtyTimestamp: Long = 0,
    ) {
        companion object {
            private val projection = arrayOf(
                BaseColumns._ID,
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
                )
            }
        }
    }
}
