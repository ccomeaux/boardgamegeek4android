package com.boardgamegeek.db

import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.entities.CollectionViewFilterEntity
import com.boardgamegeek.entities.CollectionViewShortcutEntity
import com.boardgamegeek.extensions.getBoolean
import com.boardgamegeek.extensions.load
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.sorter.CollectionSorterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class CollectionViewDao(private val context: BggApplication) {
    /**
     * Load all collection views from the database.
     */
    suspend fun load(includeFilters: Boolean = false): List<CollectionViewEntity> = withContext(Dispatchers.IO) {
        val uri = CollectionViews.CONTENT_URI
        val projection = arrayOf(
            CollectionViews._ID,
            CollectionViews.NAME,
            CollectionViews.SORT_TYPE,
            CollectionViews.STARRED,
        )
        val list = mutableListOf<CollectionViewEntity>()
        context.contentResolver.load(uri, projection)?.use {
            if (it.moveToFirst()) {
                do {
                    val entity = CollectionViewEntity(
                        id = it.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong(),
                        name = it.getStringOrNull(1).orEmpty(),
                        sortType = it.getIntOrNull(2) ?: CollectionSorterFactory.TYPE_UNKNOWN,
                        starred = it.getBoolean(3),
                    )
                    list += if (includeFilters) {
                        load(entity.id) ?: entity
                    } else entity
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
            CollectionViewFilters.STARRED,
            CollectionViewFilters.TYPE,
            CollectionViewFilters.DATA,
        )
        context.contentResolver.load(
            uri,
            projection,
        )?.use {
            if (it.moveToFirst()) {
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
            } else null
        }
    }

    suspend fun loadShortcuts(): MutableList<CollectionViewShortcutEntity> = withContext(Dispatchers.IO) {
        val shortcuts = mutableListOf<CollectionViewShortcutEntity>()
        context.contentResolver.load(
            CollectionViews.CONTENT_URI,
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
        context.contentResolver.load(uri, arrayOf(CollectionViews.SELECTED_COUNT))?.use {
            if (it.moveToFirst()) {
                val currentCount = it.getIntOrNull(0) ?: 0
                val values = contentValuesOf(
                    CollectionViews.SELECTED_COUNT to currentCount + 1,
                    CollectionViews.SELECTED_TIMESTAMP to System.currentTimeMillis(),
                )
                context.contentResolver.update(uri, values, null, null)
            }
        }
    }

    suspend fun insert(view: CollectionViewEntity): Long = withContext(Dispatchers.IO) {
        val values = contentValuesOf(
            CollectionViews.NAME to view.name,
            CollectionViews.STARRED to false,
            CollectionViews.SORT_TYPE to view.sortType,
        )
        val filterUri = context.contentResolver.insert(CollectionViews.CONTENT_URI, values)

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
        context.contentResolver.update(uri, values, null, null)

        val viewFiltersUri = CollectionViews.buildViewFilterUri(view.id)
        context.contentResolver.delete(viewFiltersUri, null, null)
        insertDetails(viewFiltersUri, view.filters)
    }

    suspend fun delete(viewId: Long): Boolean = withContext(Dispatchers.IO) {
        context.contentResolver.delete(CollectionViews.buildViewUri(viewId), null, null) > 0
    }

    private suspend fun insertDetails(viewFiltersUri: Uri, filters: List<CollectionViewFilterEntity>?) = withContext(Dispatchers.IO) {
        filters?.let {
            val values = it.map { filter ->
                contentValuesOf(
                    CollectionViewFilters.TYPE to filter.type,
                    CollectionViewFilters.DATA to filter.data,
                )
            }
            if (values.isNotEmpty()) {
                context.contentResolver.bulkInsert(viewFiltersUri, values.toTypedArray())
            }
        }
    }
}
