package com.boardgamegeek.db

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.CollectionViewFilterLocal
import com.boardgamegeek.db.model.CollectionViewLocal
import com.boardgamegeek.extensions.getBooleanOrNull
import com.boardgamegeek.extensions.loadEntity
import com.boardgamegeek.extensions.loadList
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionViewDao(private val context: Context) {
    /**
     * Load all collection views from the database.
     */
    suspend fun load(includeFilters: Boolean = false): List<CollectionViewLocal> = withContext(Dispatchers.IO) {
        context.contentResolver.loadList(
            CollectionViews.CONTENT_URI,
            arrayOf(
                BaseColumns._ID,
                CollectionViews.Columns.NAME,
                CollectionViews.Columns.SORT_TYPE,
                CollectionViews.Columns.STARRED,
                CollectionViews.Columns.SELECTED_COUNT,
                CollectionViews.Columns.SELECTED_TIMESTAMP,
            ),
            sortOrder = "${CollectionViews.Columns.SELECTED_COUNT} DESC, ${CollectionViews.Columns.SELECTED_TIMESTAMP} DESC",
        ) {
            val entity = fromCursor(it)
            if (includeFilters) {
                load(entity.id, true) ?: entity
            } else entity
        }
    }

    /**
     * Load the specified collection view from the database. This includes the sort and filter details.
     */
    suspend fun load(viewId: Int, includeFilters: Boolean = true): CollectionViewLocal? =
        withContext(Dispatchers.IO) {
            if (viewId <= 0L) return@withContext null
            context.contentResolver.loadEntity(
                CollectionViewFilters.buildViewFilterUri(viewId),
                arrayOf(
                    CollectionViewFilters.Columns.VIEW_ID,
                    CollectionViews.Columns.NAME,
                    CollectionViews.Columns.SORT_TYPE,
                    CollectionViews.Columns.STARRED,
                    CollectionViews.Columns.SELECTED_COUNT,
                    CollectionViews.Columns.SELECTED_TIMESTAMP,
                    BaseColumns._ID,
                    CollectionViewFilters.Columns.TYPE,
                    CollectionViewFilters.Columns.DATA,
                ),
            ) {
                val entity = fromCursor(it)
                if (includeFilters && it.moveToFirst()) {
                    val filters = mutableListOf<CollectionViewFilterLocal>()
                    do {
                        filters += CollectionViewFilterLocal(
                            id = it.getInt(0),
                            viewId = it.getInt(6),
                            type = it.getIntOrNull(7),
                            data = it.getStringOrNull(8),
                        )
                    } while (it.moveToNext())
                    entity.copy(filters = filters)
                } else entity
            }
        }

    private fun fromCursor(it: Cursor) = CollectionViewLocal(
        id = it.getIntOrNull(0) ?: BggContract.INVALID_ID,
        name = it.getStringOrNull(1),
        sortType = it.getIntOrNull(2),
        starred = it.getBooleanOrNull(3),
        selectedCount = it.getIntOrNull(4),
        selectedTimestamp = it.getLongOrNull(5),
    )

    suspend fun updateShortcutCount(viewId: Int) = withContext(Dispatchers.IO) {
        load(viewId, false)?.let {
            context.contentResolver.update(
                CollectionViews.buildViewUri(viewId),
                contentValuesOf(
                    CollectionViews.Columns.SELECTED_COUNT to (it.selectedCount ?: 0) + 1,
                    CollectionViews.Columns.SELECTED_TIMESTAMP to System.currentTimeMillis(),
                ),
                null,
                null,
            )
        }
    }

    suspend fun insert(view: CollectionViewLocal): Int = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            CollectionViews.Columns.NAME to view.name,
            CollectionViews.Columns.STARRED to view.starred,
            CollectionViews.Columns.SORT_TYPE to view.sortType,
        )
        val filterUri = context.contentResolver.insert(CollectionViews.CONTENT_URI, values)

        filterUri?.let {
            val id = CollectionViews.getViewId(it)
            val uri = CollectionViewFilters.buildViewFilterUri(id)
            insertDetails(uri, view.filters)
        } ?: BggContract.INVALID_ID

        filterUri?.lastPathSegment?.toIntOrNull() ?: BggContract.INVALID_ID
    }

    suspend fun update(view: CollectionViewLocal) = withContext(Dispatchers.IO) {
        if (view.id == BggContract.INVALID_ID) return@withContext

        val uri = CollectionViews.buildViewUri(view.id)
        val values = contentValuesOf(
            CollectionViews.Columns.NAME to view.name,
            CollectionViews.Columns.STARRED to view.starred,
            CollectionViews.Columns.SORT_TYPE to view.sortType,
        )
        context.contentResolver.update(uri, values, null, null)

        val viewFiltersUri = CollectionViewFilters.buildViewFilterUri(view.id)
        context.contentResolver.delete(viewFiltersUri, null, null)
        insertDetails(viewFiltersUri, view.filters)
    }

    suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
        context.contentResolver.delete(CollectionViews.CONTENT_URI, null, null) > 0
    }

    suspend fun delete(viewId: Int): Boolean = withContext(Dispatchers.IO) {
        if (viewId == BggContract.INVALID_ID) return@withContext false
        context.contentResolver.delete(CollectionViews.buildViewUri(viewId), null, null) > 0
    }

    private suspend fun insertDetails(viewFiltersUri: Uri, filters: List<CollectionViewFilterLocal>?) = withContext(Dispatchers.IO) {
        filters?.let {
            val values = it.map { filter ->
                contentValuesOf(
                    CollectionViewFilters.Columns.TYPE to filter.type,
                    CollectionViewFilters.Columns.DATA to filter.data,
                )
            }
            if (values.isNotEmpty()) {
                context.contentResolver.bulkInsert(viewFiltersUri, values.toTypedArray())
            }
        }
    }
}
