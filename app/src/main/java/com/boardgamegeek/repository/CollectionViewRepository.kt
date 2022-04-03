package com.boardgamegeek.repository

import androidx.core.content.pm.ShortcutManagerCompat
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionViewDao
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.extensions.CollectionView
import com.boardgamegeek.sorter.CollectionSorterFactory
import com.boardgamegeek.ui.CollectionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionViewRepository(val application: BggApplication) {
    private val dao = CollectionViewDao(application)

    private val defaultView = CollectionViewEntity(
        id = CollectionView.DEFAULT_DEFAULT_ID,
        name = application.getString(R.string.title_collection),
        sortType = CollectionSorterFactory.TYPE_DEFAULT
    )

    suspend fun load(includeDefault: Boolean = true, includeFilters: Boolean = false): List<CollectionViewEntity> {
        val list = dao.load(includeFilters)
        return if (includeDefault) listOf(defaultView) + list else list
    }

    suspend fun load(viewId: Long): CollectionViewEntity {
        return dao.load(viewId) ?: defaultView
    }

    suspend fun insertView(view: CollectionViewEntity): Long {
        return dao.insert(view)
    }

    suspend fun updateView(view: CollectionViewEntity) {
        dao.update(view)
    }

    suspend fun delete() = dao.delete()

    suspend fun deleteView(viewId: Long) = dao.delete(viewId)

    suspend fun updateShortcuts(viewId: Long) = withContext(Dispatchers.Default) {
        if (viewId > 0) {
            dao.updateShortcutCount(viewId)
            ShortcutManagerCompat.reportShortcutUsed(application, CollectionActivity.createShortcutName(viewId))
            val shortcuts = dao.loadShortcuts()
                .filter { it.name.isNotBlank() }
                .take(SHORTCUT_COUNT)
                .map { entity -> CollectionActivity.createShortcutInfo(application, entity.viewId, entity.name) }
            ShortcutManagerCompat.setDynamicShortcuts(application, shortcuts)
        }
    }

    companion object {
        private const val SHORTCUT_COUNT = 3
    }
}
