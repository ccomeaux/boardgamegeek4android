package com.boardgamegeek.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MechanicRepository(
    val context: Context,
    private val mechanicDao: MechanicDao,
    private val collectionDao: CollectionDao,
) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    enum class CollectionSortType {
        NAME, RATING
    }

    suspend fun loadMechanicsAsLiveData(sortBy: SortType = SortType.NAME): LiveData<List<Mechanic>> = withContext(Dispatchers.Default) {
        mechanicDao.loadMechanicsAsLiveData().map {
            it.map { entity -> entity.mapToModel() }
        }.map { list ->
            list.sortedWith(
                when (sortBy) {
                    SortType.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    SortType.ITEM_COUNT -> compareByDescending<Mechanic> { it.itemCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                }
            )
        }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): List<CollectionItem> {
        if (id == BggContract.INVALID_ID) return emptyList()
        return withContext(Dispatchers.IO) { collectionDao.loadForMechanic(id) }
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }
            .sortedWith(
                if (sortBy == CollectionSortType.RATING)
                    compareByDescending<CollectionItem> { it.rating }
                        .thenByDescending { it.isFavorite }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                else
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
            )
    }

    suspend fun deleteAll() = mechanicDao.deleteAll()
}
