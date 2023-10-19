package com.boardgamegeek.repository

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionViewDao
import com.boardgamegeek.model.CollectionView
import com.boardgamegeek.extensions.CollectionViewPrefs
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

    suspend fun loadViewsWithoutFilters(includeDefault: Boolean = true): List<CollectionView> {
        val list = collectionViewDao.loadViewsWithoutFilters().map { it.mapToModel() }
        return if (includeDefault) listOf(defaultView) + list else list
    }

    suspend fun loadViews(): List<CollectionView> {
        return collectionViewDao.loadViews().map { it.key.mapToModel(it.value) }
    }

    suspend fun loadView(viewId: Int): CollectionView {
        if (viewId == CollectionViewPrefs.DEFAULT_DEFAULT_ID){
            return defaultView
        }
        val multimap = collectionViewDao.loadView(viewId)
        return multimap?.firstNotNullOfOrNull { it.key.mapToModel(it.value) } ?: defaultView
    }

    suspend fun insertView(view: CollectionView): Int {
        return collectionViewDao.insert(view.mapToEntity(), view.filters?.map { it.mapToEntity(view.id) }.orEmpty()).toInt()
    }

    suspend fun updateView(view: CollectionView) {
        collectionViewDao.update(view.mapToEntity(), view.filters?.map { it.mapToEntity(view.id) }.orEmpty())
    }

    suspend fun deleteAll() = collectionViewDao.deleteAll()

    suspend fun deleteView(viewId: Int): Boolean {
        if (viewId == BggContract.INVALID_ID) return false
        return collectionViewDao.delete(viewId) > 0
    }

    suspend fun updateShortcuts(viewId: Int) = withContext(Dispatchers.Default) {
        if (viewId > 0) {
            collectionViewDao.loadView(viewId)?.let { view ->
                val count = view.firstNotNullOfOrNull { it.key.selectedCount } ?: 0
                collectionViewDao.updateShortcut(viewId, count, System.currentTimeMillis())
            }
            ShortcutManagerCompat.reportShortcutUsed(context, CollectionActivity.createShortcutName(viewId))
            val shortcuts = collectionViewDao.loadViewsWithoutFilters()
                .filterNot { it.name.isNullOrBlank() }
                .take(SHORTCUT_COUNT)
                .map { view -> CollectionActivity.createShortcutInfo(context, view.id, view.name.orEmpty()) }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }

    companion object {
        private const val SHORTCUT_COUNT = 3
    }
}
