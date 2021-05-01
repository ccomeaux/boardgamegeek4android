package com.boardgamegeek.db

import android.content.ContentValues
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.entities.CollectionViewFilterEntity
import com.boardgamegeek.entities.CollectionViewShortcutEntity
import com.boardgamegeek.extensions.load
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.sorter.CollectionSorterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class CollectionViewDao(private val context: BggApplication) {
    private val resolver = context.contentResolver

    /**
     * Load all collection views from the database. This only includes ID and name.
     */
    suspend fun load(): List<CollectionViewEntity> = withContext(Dispatchers.IO) {
        val uri = CollectionViews.CONTENT_URI
        val projection = arrayOf(
                CollectionViews._ID,
                CollectionViews.NAME
        )
        val list = mutableListOf<CollectionViewEntity>()
        resolver.load(uri, projection)?.use {
            if (it.moveToFirst()) {
                do {
                    list += CollectionViewEntity(
                            id = it.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong(),
                            name = it.getStringOrNull(1).orEmpty(),
                    )
                } while (it.moveToNext())
            }
        }
        list
    }

    /**
     * Load the specified collection view from the database. This includes the sort and filter details.
     */
    suspend fun load(viewId: Long): CollectionViewEntity? = withContext(Dispatchers.IO) {
        if (viewId <= 0L) return@withContext null
        val uri = CollectionViews.buildViewFilterUri(viewId)
        val projection = arrayOf(
                CollectionViewFilters.NAME,
                CollectionViewFilters.SORT_TYPE,
                CollectionViewFilters.TYPE,
                CollectionViewFilters.DATA,
        )
        context.contentResolver.load(
                uri,
                projection
        )?.use {
            if (it.moveToFirst()) {
                val viewName = it.getStringOrNull(0).orEmpty()
                val viewSortType = it.getIntOrNull(1) ?: CollectionSorterFactory.TYPE_UNKNOWN
                val filters = mutableListOf<CollectionViewFilterEntity>()
                do {
                    filters += CollectionViewFilterEntity(
                            type = it.getIntOrNull(2) ?: BggContract.INVALID_ID,
                            data = it.getStringOrNull(3).orEmpty()
                    )
                } while (it.moveToNext())
                CollectionViewEntity(
                        id = viewId,
                        name = viewName,
                        sortType = viewSortType,
                        filters = filters,
                )
            } else null
        }
    }

    suspend fun loadShortcuts(): MutableList<CollectionViewShortcutEntity> = withContext(Dispatchers.IO) {
        val shortcuts = mutableListOf<CollectionViewShortcutEntity>()
        resolver.load(CollectionViews.CONTENT_URI,
                arrayOf(CollectionViews._ID, CollectionViews.NAME),
                sortOrder = "${CollectionViews.SELECTED_COUNT} DESC, ${CollectionViews.SELECTED_TIMESTAMP} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    shortcuts += CollectionViewShortcutEntity(
                            viewId = cursor.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong(),
                            name = cursor.getStringOrNull(1).orEmpty(),
                    )
                } while (cursor.moveToNext())
            }
        }
        return@withContext shortcuts
    }

    suspend fun updateShortcutCount(viewId: Long) = withContext(Dispatchers.IO) {
        if (viewId <= 0) return@withContext
        val uri = CollectionViews.buildViewUri(viewId)
        resolver.load(uri, arrayOf(CollectionViews.SELECTED_COUNT))?.use {
            if (it.moveToFirst()) {
                val currentCount = it.getIntOrNull(0) ?: 0
                val values = contentValuesOf(
                        CollectionViews.SELECTED_COUNT to currentCount + 1,
                        CollectionViews.SELECTED_TIMESTAMP to System.currentTimeMillis(),
                )
                resolver.update(uri, values, null, null)
            }
        }
    }

    suspend fun insert(view: CollectionViewEntity): Long = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
                CollectionViews.NAME to view.name,
                CollectionViews.STARRED to false,
                CollectionViews.SORT_TYPE to view.sortType,
        )
        val filterUri = resolver.insert(CollectionViews.CONTENT_URI, values)

        val filterId = CollectionViews.getViewId(filterUri)
        val uri = CollectionViews.buildViewFilterUri(filterId.toLong())
        insertDetails(uri, view.filters)
        filterUri?.lastPathSegment?.toLongOrNull() ?: BggContract.INVALID_ID.toLong()
    }

    suspend fun update(view: CollectionViewEntity) = withContext(Dispatchers.IO) {
        val uri = CollectionViews.buildViewUri(view.id)
        val values = contentValuesOf(
                CollectionViews.NAME to view.name,
                CollectionViews.SORT_TYPE to view.sortType,
        )
        resolver.update(uri, values, null, null)

        val viewFiltersUri = CollectionViews.buildViewFilterUri(view.id)
        resolver.delete(viewFiltersUri, null, null)
        insertDetails(viewFiltersUri, view.filters)
    }

    suspend fun delete(viewId: Long): Boolean = withContext(Dispatchers.IO) {
        resolver.delete(CollectionViews.buildViewUri(viewId), null, null) > 0
    }

    private suspend fun insertDetails(viewFiltersUri: Uri, filters: List<CollectionViewFilterEntity>?) = withContext(Dispatchers.IO) {
        filters?.let {
            val values = mutableListOf<ContentValues>()
            for (filter in it) {
                values += contentValuesOf(
                        CollectionViewFilters.TYPE to filter.type,
                        CollectionViewFilters.DATA to filter.data,
                )
            }
            if (values.isNotEmpty()) {
                resolver.bulkInsert(viewFiltersUri, values.toTypedArray())
            }
        }
    }
}
