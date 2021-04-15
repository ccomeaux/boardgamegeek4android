package com.boardgamegeek.db

import android.content.ContentValues
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.entities.CollectionViewFilterEntity
import com.boardgamegeek.entities.CollectionViewShortcutEntity
import com.boardgamegeek.extensions.load
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class CollectionViewDao(private val context: BggApplication) {
    private val resolver = context.contentResolver

    fun loadAsLiveData(): LiveData<List<CollectionViewEntity>> {
        return RegisteredLiveData(context, CollectionViews.CONTENT_URI, true) {
            return@RegisteredLiveData load()
        }
    }

    /**
     * Load all collection views from the database. This only includes ID and name.
     */
    fun load(): List<CollectionViewEntity> {
        val uri = CollectionViews.CONTENT_URI
        val projection = arrayOf(
                CollectionViews._ID,
                CollectionViews.NAME
        )
        val list = arrayListOf<CollectionViewEntity>()
        resolver.load(uri, projection)?.use {
            if (it.moveToFirst()) {
                do {
                    val cv = CollectionViewEntity(
                            it.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong(),
                            it.getStringOrNull(1).orEmpty()
                    )
                    list.add(cv)
                } while (it.moveToNext())
            }
        }
        return list
    }

    /**
     * Load the specified collection view from the database. This includes the sort and filter details.
     */
    fun loadAsLiveData(viewId: Long): LiveData<CollectionViewEntity> {
        return RegisteredLiveData(context, CollectionViews.buildViewFilterUri(viewId), true) {
            return@RegisteredLiveData load(viewId)
        }
    }

    /**
     * Load the specified collection view from the database. This includes the sort and filter details.
     */
    private fun load(viewId: Long): CollectionViewEntity? {
        if (viewId <= 0L) return null
        val uri = CollectionViews.buildViewFilterUri(viewId)
        val projection = arrayOf(
                CollectionViewFilters.NAME,
                CollectionViewFilters.SORT_TYPE,
                CollectionViewFilters.TYPE,
                CollectionViewFilters.DATA
        )
        return context.contentResolver.load(
                uri,
                projection
        )?.use {
            if (it.moveToFirst()) {
                val viewName = it.getStringOrNull(0).orEmpty()
                val sortType = it.getIntOrNull(1) ?: 0
                val filters = mutableListOf<CollectionViewFilterEntity>()
                do {
                    filters += CollectionViewFilterEntity(
                            it.getIntOrNull(2) ?: BggContract.INVALID_ID,
                            it.getStringOrNull(3).orEmpty()
                    )
                } while (it.moveToNext())
                CollectionViewEntity(
                        viewId,
                        viewName,
                        sortType,
                        filters
                )
            } else null
        }
    }

    suspend fun loadShortcuts(): MutableList<CollectionViewShortcutEntity> {
        val shortcuts = mutableListOf<CollectionViewShortcutEntity>()
        withContext(Dispatchers.IO) {
            resolver.load(CollectionViews.CONTENT_URI,
                    arrayOf(CollectionViews._ID, CollectionViews.NAME),
                    sortOrder = "${CollectionViews.SELECTED_COUNT} DESC, ${CollectionViews.SELECTED_TIMESTAMP} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val viewId = cursor.getLongOrNull(0) ?: BggContract.INVALID_ID.toLong()
                    val name = cursor.getStringOrNull(1).orEmpty()
                    shortcuts += CollectionViewShortcutEntity(viewId, name)
                }
            }
        }
        return shortcuts
    }

    suspend fun updateShortcutCount(viewId: Long) {
        if (viewId <= 0) return
        withContext(Dispatchers.IO) {
            val uri = CollectionViews.buildViewUri(viewId)
            resolver.load(uri, arrayOf(CollectionViews.SELECTED_COUNT))?.use {
                if (it.moveToFirst()) {
                    val currentCount = it.getIntOrNull(0) ?: 0
                    val values = contentValuesOf(
                            CollectionViews.SELECTED_COUNT to currentCount + 1,
                            CollectionViews.SELECTED_TIMESTAMP to System.currentTimeMillis()
                    )
                    resolver.update(uri, values, null, null)
                }
            }
        }
    }

    fun insert(view: CollectionViewEntity): Long {
        val values = contentValuesOf(
                CollectionViews.NAME to view.name,
                CollectionViews.STARRED to false,
                CollectionViews.SORT_TYPE to view.sortType
        )
        val filterUri = resolver.insert(CollectionViews.CONTENT_URI, values)

        val filterId = CollectionViews.getViewId(filterUri)
        val uri = CollectionViews.buildViewFilterUri(filterId.toLong())
        insertDetails(uri, view.filters)
        return filterUri?.lastPathSegment?.toLongOrNull() ?: BggContract.INVALID_ID.toLong()
    }

    fun update(view: CollectionViewEntity) {
        val uri = CollectionViews.buildViewUri(view.id)
        val values = contentValuesOf(
                CollectionViews.NAME to view.name,
                CollectionViews.SORT_TYPE to view.sortType
        )
        resolver.update(uri, values, null, null)

        val viewFiltersUri = CollectionViews.buildViewFilterUri(view.id)
        resolver.delete(viewFiltersUri, null, null)
        insertDetails(viewFiltersUri, view.filters)
    }

    fun delete(viewId: Long): Boolean {
        return resolver.delete(CollectionViews.buildViewUri(viewId), null, null) > 0
    }

    private fun insertDetails(viewFiltersUri: Uri, filters: List<CollectionViewFilterEntity>?) {
        if (filters == null) return
        val values = ArrayList<ContentValues>(filters.size)
        for (filter in filters) {
            values.add(contentValuesOf(
                    CollectionViewFilters.TYPE to filter.type,
                    CollectionViewFilters.DATA to filter.data
            ))
        }
        if (values.size > 0) {
            resolver.bulkInsert(viewFiltersUri, values.toTypedArray())
        }
    }
}
