package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.entities.BriefGameEntity

class MechanicRepository(val application: BggApplication) {
    private val mechanicDao = MechanicDao(application)

    fun loadMechanics(sortBy: MechanicDao.SortType = MechanicDao.SortType.NAME): LiveData<List<MechanicEntity>> {
        return mechanicDao.loadMechanicsAsLiveData(sortBy)
    }

    fun loadCollection(id: Int): LiveData<List<BriefGameEntity>>? {
        return mechanicDao.loadCollectionAsLiveData(id)
    }
}