package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.MechanicEntity

class MechanicRepository(val application: BggApplication) {
    private val mechanicDao = MechanicDao(application)

    fun loadMechanics(sortBy: MechanicDao.SortType = MechanicDao.SortType.NAME): LiveData<List<MechanicEntity>> {
        return mechanicDao.loadMechanicsAsLiveData(sortBy)
    }

    fun loadCollection(id: Int, sortBy: CollectionDao.SortType): LiveData<List<BriefGameEntity>>? {
        return mechanicDao.loadCollectionAsLiveData(id, sortBy)
    }
}
