package com.boardgamegeek.db

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract.Mechanics

class MechanicDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT
    }

    fun loadMechanicsAsLiveData(sortBy: SortType = SortType.NAME): LiveData<List<MechanicEntity>> {
        return RegisteredLiveData(context, Mechanics.CONTENT_URI, true) {
            return@RegisteredLiveData loadMechanics(sortBy)
        }
    }

    private fun loadMechanics(sortBy: SortType): List<MechanicEntity> {
        val results = arrayListOf<MechanicEntity>()
        val sortByName = Mechanics.MECHANIC_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Mechanics.ITEM_COUNT.descending().plus(", $sortByName")
        }
        context.contentResolver.load(
                Mechanics.CONTENT_URI,
                arrayOf(
                        Mechanics.MECHANIC_ID,
                        Mechanics.MECHANIC_NAME,
                        Mechanics.ITEM_COUNT
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += MechanicEntity(
                            it.getInt(Mechanics.MECHANIC_ID),
                            it.getStringOrEmpty(Mechanics.MECHANIC_NAME),
                            it.getIntOrZero(Mechanics.ITEM_COUNT)
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadCollectionAsLiveData(mechanicId: Int, sortBy: CollectionDao.SortType): LiveData<List<BriefGameEntity>>? {
        val uri = Mechanics.buildCollectionUri(mechanicId)
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData collectionDao.loadLinkedCollection(uri, sortBy)
        }

    }
}
