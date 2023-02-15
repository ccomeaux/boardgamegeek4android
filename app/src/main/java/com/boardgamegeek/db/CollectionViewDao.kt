package com.boardgamegeek.db

import android.content.Context
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.entities.CollectionViewFilterEntity
import com.boardgamegeek.entities.CollectionViewShortcutEntity
import com.boardgamegeek.extensions.getBoolean
import com.boardgamegeek.extensions.loadEntity
import com.boardgamegeek.extensions.loadList
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.sorter.CollectionSorterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionViewDao(private val context: Context) {
    /**
     * Load all collection views from the database.
     */
    suspend fun load(includeFilters: Boolean = false): List<CollectionViewEntity> = withContext(Dispatchers.IO) {
        context.contentResolver.loadList(
            CollectionViews.CONTENT_URI,
            arrayOf(
                BaseColumns._ID,
                CollectionViews.Columns.NAME,
                CollectionViews.Columns.SORT_TYPE,
                CollectionViews.Columns.STARRED,
            )
        ) {
            val entity = CollectionViewEntity(
                id = it.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong(),
                name = it.getStringOrNull(1).orEmpty(),
                sortType = it.getIntOrNull(2) ?: CollectionSorterFactory.TYPE_UNKNOWN,
                starred = it.getBoolean(3),
            )
            if (includeFilters) {
                load(entity.id) ?: entity
            } else entity
        }
    }

    /**
     * Load the specified collection view from the database. This includes the sort and filter details.
     */
    suspend fun load(viewId: Long): CollectionViewEntity? = withContext(Dispatchers.IO) {
        if (viewId <= 0L) return@withContext null
        context.contentResolver.loadEntity(
            CollectionViewFilters.buildViewFilterUri(viewId),
            arrayOf(
                CollectionViews.Columns.NAME,
                CollectionViews.Columns.SORT_TYPE,
                CollectionViews.Columns.STARRED,
                CollectionViewFilters.Columns.TYPE,
                CollectionViewFilters.Columns.DATA,
            ),
        ) {
            val viewName = it.getStringOrNull(0).orEmpty()
            val viewSortType = it.getIntOrNull(1) ?: CollectionSorterFactory.TYPE_UNKNOWN
            val starred = it.getBoolean(2)
            val filters = mutableListOf<CollectionViewFilterEntity>()
            do {
                filters += CollectionViewFilterEntity(
                    type = it.getIntOrNull(3) ?: BggContract.INVALID_ID,
                    data = it.getStringOrNull(4).orEmpty(),
                )
            } while (it.moveToNext())
            CollectionViewEntity(
                id = viewId,
                name = viewName,
                sortType = viewSortType,
                starred = starred,
                filters = filters,
            )
        }
    }

    suspend fun loadShortcuts(): List<CollectionViewShortcutEntity> = withContext(Dispatchers.IO) {
        context.contentResolver.loadList(
            CollectionViews.CONTENT_URI,
            arrayOf(
                BaseColumns._ID,
                CollectionViews.Columns.NAME,
                CollectionViews.Columns.SELECTED_COUNT,
                CollectionViews.Columns.SELECTED_TIMESTAMP,
            ),
            sortOrder = "${CollectionViews.Columns.SELECTED_COUNT} DESC, ${CollectionViews.Columns.SELECTED_TIMESTAMP} DESC"
        ) {
            CollectionViewShortcutEntity(
                viewId = it.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong(),
                name = it.getStringOrNull(1).orEmpty(),
                count = it.getIntOrNull(2) ?: 0,
                timestamp = it.getLongOrNull(3) ?: 0L,
            )
        }
    }

    suspend fun updateShortcutCount(viewId: Long) = withContext(Dispatchers.IO) {
        if (viewId <= 0) return@withContext
        val uri = CollectionViews.buildViewUri(viewId)
        val shortcut = context.contentResolver.loadEntity(
            CollectionViews.buildViewUri(viewId),
            arrayOf(
                BaseColumns._ID,
                CollectionViews.Columns.NAME,
                CollectionViews.Columns.SELECTED_COUNT,
                CollectionViews.Columns.SELECTED_TIMESTAMP,
            ),
            sortOrder = "${CollectionViews.Columns.SELECTED_COUNT} DESC, ${CollectionViews.Columns.SELECTED_TIMESTAMP} DESC"
        ) {
            CollectionViewShortcutEntity(
                viewId = it.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong(),
                name = it.getStringOrNull(1).orEmpty(),
                count = it.getIntOrNull(2) ?: 0,
                timestamp = it.getLongOrNull(3) ?: 0L,
            )
        }
        shortcut?.let {
            context.contentResolver.update(
                uri,
                contentValuesOf(
                    CollectionViews.Columns.SELECTED_COUNT to it.count + 1,
                    CollectionViews.Columns.SELECTED_TIMESTAMP to System.currentTimeMillis(),
                ),
                null,
                null,
            )
        }
    }

    suspend fun insert(view: CollectionViewEntity): Long = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            CollectionViews.Columns.NAME to view.name,
            CollectionViews.Columns.STARRED to view.starred,
            CollectionViews.Columns.SORT_TYPE to view.sortType,
        )
        val filterUri = context.contentResolver.insert(CollectionViews.CONTENT_URI, values)

        filterUri?.let {
            val id = CollectionViews.getViewId(it)
            val uri = CollectionViewFilters.buildViewFilterUri(id.toLong())
            insertDetails(uri, view.filters)
        } ?: BggContract.INVALID_ID

        filterUri?.lastPathSegment?.toLongOrNull() ?: BggContract.INVALID_ID.toLong()
    }

    suspend fun update(view: CollectionViewEntity) = withContext(Dispatchers.IO) {
        if (view.id == BggContract.INVALID_ID.toLong()) return@withContext

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

    suspend fun delete(viewId: Long): Boolean = withContext(Dispatchers.IO) {
        if (viewId == BggContract.INVALID_ID.toLong()) return@withContext false
        context.contentResolver.delete(CollectionViews.buildViewUri(viewId), null, null) > 0
    }

    private suspend fun insertDetails(viewFiltersUri: Uri, filters: List<CollectionViewFilterEntity>?) = withContext(Dispatchers.IO) {
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
