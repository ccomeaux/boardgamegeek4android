package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.mappers.mapToEntity

class MechanicRepository(val context: Context) {
    private val mechanicDao = MechanicDao(context)

    suspend fun loadMechanics(sortBy: MechanicDao.SortType = MechanicDao.SortType.NAME): List<MechanicEntity> {
        return mechanicDao.loadMechanics(sortBy).map { it.mapToEntity() }
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType): List<BriefGameEntity> {
        return mechanicDao.loadCollection(id, sortBy)
    }

    suspend fun delete() = mechanicDao.delete()
}
