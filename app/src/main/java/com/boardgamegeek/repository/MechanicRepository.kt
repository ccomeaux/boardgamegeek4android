package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.provider.BggContract

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

    suspend fun loadMechanics(sortBy: SortType = SortType.NAME): List<Mechanic> {
        return mechanicDao.loadMechanics()
            .sortedBy {
                when (sortBy) {
                    SortType.NAME -> it.mechanicName
                    SortType.ITEM_COUNT -> 0 // TODO
                }.toString()
            }
            .map { it.mapToModel() }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionSortType): List<CollectionItem> {
        if (id == BggContract.INVALID_ID) return emptyList()
        return collectionDao.loadForMechanic(id)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }
            .sortedWith(
                if (sortBy == CollectionSortType.RATING)
                    compareByDescending<CollectionItem> { it.rating }.thenByDescending { it.isFavorite }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                else
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
            )
    }

    suspend fun deleteAll() = mechanicDao.deleteAll()
}
