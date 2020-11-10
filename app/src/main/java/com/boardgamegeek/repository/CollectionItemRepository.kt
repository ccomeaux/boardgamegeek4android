package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.entities.CollectionItemEntity

class CollectionItemRepository(val application: BggApplication) {
    private val dao = CollectionDao(application)

    fun load(): LiveData<List<CollectionItemEntity>> {
        return dao.loadAsLiveData()
    }
}