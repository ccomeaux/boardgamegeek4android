package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.mappers.mapToModel

class MechanicRepository(
    val context: Context,
    private val mechanicDao: MechanicDao,
) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT
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

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForMechanic(id, sortBy)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun deleteAll() = mechanicDao.deleteAll()
}
