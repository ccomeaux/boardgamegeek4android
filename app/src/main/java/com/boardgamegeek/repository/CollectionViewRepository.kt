package com.boardgamegeek.repository

import android.content.pm.ShortcutManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionViewDao
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.extensions.CollectionView
import com.boardgamegeek.mappers.createShortcutName
import com.boardgamegeek.mappers.map
import com.boardgamegeek.sorter.CollectionSorterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionViewRepository(val application: BggApplication) {
    private val dao = CollectionViewDao(application)
    private val shortcutManager: ShortcutManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            application.getSystemService()
        } else {
            null
        }
    }

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

    suspend fun deleteView(viewId: Long) {
        dao.delete(viewId)
    }

    suspend fun updateShortcuts(viewId: Long) = withContext(Dispatchers.Default) {
        if (viewId > 0) {
            dao.updateShortcutCount(viewId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                shortcutManager?.reportShortcutUsed(createShortcutName(viewId))
                setShortcuts()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private suspend fun setShortcuts() {
        shortcutManager?.dynamicShortcuts = dao.loadShortcuts()
            .filter { it.name.isNotBlank() }
            .take(SHORTCUT_COUNT)
            .map { it.map(application) }
    }

    companion object {
        private const val SHORTCUT_COUNT = 3
    }
}
