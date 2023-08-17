package com.boardgamegeek.repository

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionViewDao
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.extensions.CollectionView
import com.boardgamegeek.mappers.mapToEntity
import com.boardgamegeek.mappers.mapToLocal
import com.boardgamegeek.ui.CollectionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionViewRepository(val context: Context) {
    private val dao = CollectionViewDao(context)

    private val defaultView = CollectionViewEntity(
        id = CollectionView.DEFAULT_DEFAULT_ID,
        name = context.getString(R.string.title_collection),
    )

    suspend fun load(includeDefault: Boolean = true, includeFilters: Boolean = false): List<CollectionViewEntity> {
        val list = dao.load(includeFilters).map { it.mapToEntity() }
        return if (includeDefault) listOf(defaultView) + list else list
    }

    suspend fun load(viewId: Int): CollectionViewEntity {
        return dao.load(viewId)?.mapToEntity() ?: defaultView
    }

    suspend fun insertView(view: CollectionViewEntity): Int {
        return dao.insert(view.mapToLocal())
    }

    suspend fun updateView(view: CollectionViewEntity) {
        dao.update(view.mapToLocal())
    }

    suspend fun delete() = dao.delete()

    suspend fun deleteView(viewId: Int) = dao.delete(viewId)

    suspend fun updateShortcuts(viewId: Int) = withContext(Dispatchers.Default) {
        if (viewId > 0) {
            dao.updateShortcutCount(viewId)
            ShortcutManagerCompat.reportShortcutUsed(context, CollectionActivity.createShortcutName(viewId))
            val shortcuts = dao.load(false)
                .filterNot { it.name.isNullOrBlank() }
                .take(SHORTCUT_COUNT)
                .map { entity -> CollectionActivity.createShortcutInfo(context, entity.id, entity.name.orEmpty()) }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }

    companion object {
        private const val SHORTCUT_COUNT = 3
    }
}
