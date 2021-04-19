package com.boardgamegeek.repository

import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.MechanicEntity

class MechanicRepository(val application: BggApplication) {
    private val mechanicDao = MechanicDao(application)

    suspend fun loadMechanics(sortBy: MechanicDao.SortType = MechanicDao.SortType.NAME): List<MechanicEntity> {
        return mechanicDao.loadMechanics(sortBy)
    }

    suspend fun loadCollection(id: Int, sortBy: CollectionDao.SortType): List<BriefGameEntity> {
        return mechanicDao.loadCollection(id, sortBy)
    }
}
