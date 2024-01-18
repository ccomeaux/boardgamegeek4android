package com.boardgamegeek.repository

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionViewDao
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.ui.CollectionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionViewRepository(
    val context: Context,
    private val collectionViewDao: CollectionViewDao,
) {
    private val defaultView = CollectionView(
        id = CollectionViewPrefs.DEFAULT_DEFAULT_ID,
        name = context.getString(R.string.title_collection),
    )

    fun loadViewsWithoutFiltersAsLiveData(): LiveData<List<CollectionView>> {
        return collectionViewDao.loadViewsWithoutFiltersAsLiveData().map { list ->
            listOf(defaultView) + list.map { it.mapToModel() }
        }
    }

    suspend fun loadViews(): List<CollectionView> = withContext(Dispatchers.IO) {
        collectionViewDao.loadViews().map { it.key.mapToModel(it.value) }
    }

    fun loadViewAsLiveData(viewId: Int): LiveData<CollectionView> {
        return if (viewId == CollectionViewPrefs.DEFAULT_DEFAULT_ID) MutableLiveData(defaultView)
        else collectionViewDao.loadViewAsLiveData(viewId).map { it.view.mapToModel(it.filters) }
    }

    suspend fun insertView(view: CollectionView): Int {
        val viewId = collectionViewDao.insert(view.mapToEntity(), view.filters?.map { it.mapToEntity(view.id) }.orEmpty()).toInt()
        if (view.starred) {
            context.preferences()[CollectionViewPrefs.PREFERENCES_KEY_DEFAULT_ID] = viewId
        }
        return viewId
    }

    suspend fun updateView(view: CollectionView) = withContext(Dispatchers.IO) {
        collectionViewDao.update(view.mapToEntity(), view.filters?.map { it.mapToEntity(view.id) }.orEmpty())
        val defaultViewId = context.preferences()[CollectionViewPrefs.PREFERENCES_KEY_DEFAULT_ID, CollectionViewPrefs.DEFAULT_DEFAULT_ID] ?: CollectionViewPrefs.DEFAULT_DEFAULT_ID
        if (view.starred) {
            context.preferences()[CollectionViewPrefs.PREFERENCES_KEY_DEFAULT_ID] = view.id
        } else if (view.id == defaultViewId) {
            context.preferences().remove(CollectionViewPrefs.PREFERENCES_KEY_DEFAULT_ID)
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { collectionViewDao.deleteAll() }

    suspend fun deleteView(viewId: Int): Boolean {
        if (viewId == BggContract.INVALID_ID) return false
        collectionViewDao.loadView(viewId)?.let { view ->
            if (view.starred == true) {
                context.preferences().remove(CollectionViewPrefs.PREFERENCES_KEY_DEFAULT_ID)
            }
        }
        return collectionViewDao.delete(viewId) > 0
    }

    suspend fun updateShortcuts(viewId: Int) {
        if (viewId > 0) {
            withContext(Dispatchers.IO) {
                collectionViewDao.loadView(viewId)?.let { view ->
                    val updatedCount = (view.selectedCount ?: 0) + 1
                    collectionViewDao.updateShortcut(viewId, updatedCount, System.currentTimeMillis())
                }
            }
            withContext(Dispatchers.Default) {
                ShortcutManagerCompat.reportShortcutUsed(context, CollectionActivity.createShortcutName(viewId))
                val shortcuts = withContext(Dispatchers.IO) { collectionViewDao.loadViewsWithoutFilters() }
                    .filterNot { it.name.isNullOrBlank() }
                    .take(SHORTCUT_COUNT)
                    .map { view -> CollectionActivity.createShortcutInfo(context, view.id, view.name.orEmpty()) }
                ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
            }
        }
    }

    companion object {
        private const val SHORTCUT_COUNT = 3
    }
}
