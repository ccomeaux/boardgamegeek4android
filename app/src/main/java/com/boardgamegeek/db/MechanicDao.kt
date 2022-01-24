package com.boardgamegeek.db

import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.MechanicEntity
import com.boardgamegeek.extensions.ascending
import com.boardgamegeek.extensions.collateNoCase
import com.boardgamegeek.extensions.descending
import com.boardgamegeek.extensions.load
import com.boardgamegeek.provider.BggContract.Mechanics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MechanicDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT
    }

    suspend fun loadMechanics(sortBy: SortType): List<MechanicEntity> = withContext(Dispatchers.IO) {
        val results = arrayListOf<MechanicEntity>()
        val sortByName = Mechanics.Columns.MECHANIC_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Mechanics.Columns.ITEM_COUNT.descending().plus(", $sortByName")
        }
        context.contentResolver.load(
            Mechanics.CONTENT_URI,
            arrayOf(
                Mechanics.Columns.MECHANIC_ID,
                Mechanics.Columns.MECHANIC_NAME,
                Mechanics.Columns.ITEM_COUNT
            ),
            sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += MechanicEntity(
                        it.getInt(0),
                        it.getStringOrNull(1).orEmpty(),
                        it.getIntOrNull(2) ?: 0
                    )
                } while (it.moveToNext())
            }
        }
        results
    }

    suspend fun loadCollection(mechanicId: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadLinkedCollection(Mechanics.buildCollectionUri(mechanicId), sortBy)

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Mechanics.CONTENT_URI, null, null)
    }
}
