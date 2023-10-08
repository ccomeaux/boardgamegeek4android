package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.mappers.mapToModel

class MechanicRepository(val context: Context) {
    private val mechanicDao = MechanicDao(context)
    private val collectionDao = CollectionDao(context)

    suspend fun loadMechanics(sortBy: MechanicDao.SortType = MechanicDao.SortType.NAME): List<Mechanic> {
        return mechanicDao.loadMechanics(sortBy).map { it.mapToModel() }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForMechanic(id, sortBy)
            .map { it.mapToModel() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun delete() = mechanicDao.delete()
}
