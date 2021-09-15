package com.boardgamegeek.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.R
import com.boardgamegeek.db.CollectionViewDao
import com.boardgamegeek.entities.CollectionViewEntity
import com.boardgamegeek.extensions.CollectionView
import com.boardgamegeek.sorter.CollectionSorterFactory

class CollectionViewRepository(val context: BggApplication) {
    private val dao = CollectionViewDao(context)
    private val defaultView = CollectionViewEntity(
            CollectionView.DEFAULT_DEFAULT_ID,
            context.getString(R.string.title_collection),
            CollectionSorterFactory.TYPE_DEFAULT
    )

    fun load(): LiveData<List<CollectionViewEntity>> {
        val mediatorLiveData = MediatorLiveData<List<CollectionViewEntity>>()
        mediatorLiveData.addSource(dao.loadAsLiveData()) {
            mediatorLiveData.value = arrayListOf<CollectionViewEntity>().apply {
                add(defaultView)
                addAll(it)
            }
        }
        return mediatorLiveData
    }

    fun load(viewId: Long): LiveData<CollectionViewEntity> {
        val mediatorLiveData = MediatorLiveData<CollectionViewEntity>()
        mediatorLiveData.addSource(dao.loadAsLiveData(viewId)) {
            mediatorLiveData.value = it ?: defaultView
        }
        return mediatorLiveData
    }

    fun insertView(view: CollectionViewEntity): Long {
        // TODO get this off the main thread
        // context.appExecutors.diskIO.execute {
        return dao.insert(view)
        //}
    }

    fun updateView(view: CollectionViewEntity) {
        context.appExecutors.diskIO.execute {
            dao.update(view)
        }
    }

    fun deleteView(viewId: Long) {
        context.appExecutors.diskIO.execute {
            dao.delete(viewId)
        }
    }
}
