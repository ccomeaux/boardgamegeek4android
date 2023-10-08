package com.boardgamegeek.repository

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionViewDao
import com.boardgamegeek.entities.CollectionView
import com.boardgamegeek.extensions.CollectionViewPrefs
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.mappers.mapToLocal
import com.boardgamegeek.ui.CollectionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionViewRepository(val context: Context) {
    private val dao = CollectionViewDao(context)

    private val defaultView = CollectionView(
        id = CollectionViewPrefs.DEFAULT_DEFAULT_ID,
        name = context.getString(R.string.title_collection),
    )

    suspend fun load(includeDefault: Boolean = true, includeFilters: Boolean = false): List<CollectionView> {
        val list = dao.load(includeFilters).map { it.mapToModel() }
        return if (includeDefault) listOf(defaultView) + list else list
    }

    suspend fun load(viewId: Int): CollectionView {
        return dao.load(viewId)?.mapToModel() ?: defaultView
    }

    suspend fun insertView(view: CollectionView): Int {
        return dao.insert(view.mapToLocal())
    }

    suspend fun updateView(view: CollectionView) {
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
                .map { view -> CollectionActivity.createShortcutInfo(context, view.id, view.name.orEmpty()) }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }

    companion object {
        private const val SHORTCUT_COUNT = 3
    }
}
