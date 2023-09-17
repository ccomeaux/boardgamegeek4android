package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.entities.CollectionItemEntity.Companion.filterBySyncedStatues
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.mappers.mapToEntity

class MechanicRepository(val context: Context) {
    private val mechanicDao = MechanicDao(context)
    private val collectionDao = CollectionDao(context)

    suspend fun loadMechanics(sortBy: MechanicDao.SortType = MechanicDao.SortType.NAME): List<MechanicEntity> {
        return mechanicDao.loadMechanics(sortBy).map { it.mapToEntity() }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadCollectionForMechanic(id, sortBy)
            .map { it.mapToEntity() }
            .filter { it.deleteTimestamp == 0L }
            .filter { it.filterBySyncedStatues(context) }

    suspend fun delete() = mechanicDao.delete()
}
