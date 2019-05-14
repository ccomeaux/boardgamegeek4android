package com.boardgamegeek.db

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract

class MechanicDao(private val context: BggApplication) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    fun loadMechanicsAsLiveData(sortBy: SortType = SortType.NAME): LiveData<List<MechanicEntity>> {
        return RegisteredLiveData(context, BggContract.Mechanics.CONTENT_URI, true) {
            return@RegisteredLiveData loadMechanics(sortBy)
        }
    }

    private fun loadMechanics(sortBy: SortType): List<MechanicEntity> {
        val results = arrayListOf<MechanicEntity>()
        val sortByName = BggContract.Mechanics.MECHANIC_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> BggContract.Mechanics.ITEM_COUNT.descending().plus(", $sortByName")
        }
        context.contentResolver.load(
                BggContract.Mechanics.CONTENT_URI,
                arrayOf(
                        BggContract.Mechanics.MECHANIC_ID,
                        BggContract.Mechanics.MECHANIC_NAME,
                        BggContract.Mechanics.ITEM_COUNT
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += MechanicEntity(
                            it.getInt(BggContract.Mechanics.MECHANIC_ID),
                            it.getStringOrEmpty(BggContract.Mechanics.MECHANIC_NAME),
                            it.getIntOrZero(BggContract.Mechanics.ITEM_COUNT)
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }
}