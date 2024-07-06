package com.boardgamegeek.repository

import android.content.Context
import com.boardgamegeek.db.CollectionDao
import com.boardgamegeek.db.MechanicDao
import com.boardgamegeek.model.CollectionItem.Companion.filterBySyncedStatues
import com.boardgamegeek.model.Mechanic
import com.boardgamegeek.mappers.mapToModel
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.CollectionItem.Companion.applySort
import com.boardgamegeek.model.Mechanic.Companion.applySort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MechanicRepository(
    val context: Context,
    private val mechanicDao: MechanicDao,
    private val collectionDao: CollectionDao,
) {
    fun loadMechanicsFlow(sortBy: Mechanic.SortType): Flow<List<Mechanic>> {
        return mechanicDao.loadMechanicsFLow()
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { it.applySort(sortBy) }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    fun loadCollection(id: Int, sortBy: CollectionItem.SortType): Flow<List<CollectionItem>> {
        return collectionDao.loadForMechanicFlow(id)
            .map { it.map { entity -> entity.mapToModel() } }
            .flowOn(Dispatchers.Default)
            .map { list ->
                list.filter { it.deleteTimestamp == 0L }
                    .filter { it.filterBySyncedStatues(context) }
                    .applySort(sortBy)
            }
            .flowOn(Dispatchers.Default)
            .conflate()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) { mechanicDao.deleteAll() }
}
