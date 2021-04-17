package com.boardgamegeek.db

import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.extensions.ascending
import com.boardgamegeek.extensions.collateNoCase
import com.boardgamegeek.extensions.descending
import com.boardgamegeek.extensions.load
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.livedata.RegisteredLiveDataCoroutine
import com.boardgamegeek.provider.BggContract
import kotlinx.coroutines.*

class CategoryDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT
    }

    suspend fun loadCategoriesAsLiveData(scope: CoroutineScope, sortBy: SortType = SortType.NAME): LiveData<List<CategoryEntity>> {
        return RegisteredLiveDataCoroutine(context, BggContract.Categories.CONTENT_URI, true, scope) {
            return@RegisteredLiveDataCoroutine loadCategories(sortBy)
        }
    }

    private suspend fun loadCategories(sortBy: SortType, ioDispatcher: CoroutineDispatcher = Dispatchers.IO): List<CategoryEntity> = withContext(ioDispatcher) {
        val results = mutableListOf<CategoryEntity>()
        val sortByName = BggContract.Categories.CATEGORY_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> BggContract.Categories.ITEM_COUNT.descending().plus(", $sortByName")
        }
        context.contentResolver.load(
                BggContract.Categories.CONTENT_URI,
                arrayOf(
                        BggContract.Categories.CATEGORY_ID,
                        BggContract.Categories.CATEGORY_NAME,
                        BggContract.Categories.ITEM_COUNT
                ),
                sortOrder = sortOrder
        )?.use {
            if (it.moveToFirst()) {
                do {
                    results += CategoryEntity(
                            it.getInt(0),
                            it.getStringOrNull(1).orEmpty(),
                            it.getIntOrNull(2) ?: 0,
                    )
                } while (it.moveToNext())
            }
        }
        return@withContext results
    }

    fun loadCollectionAsLiveData(categoryId: Int, sortBy: CollectionDao.SortType): LiveData<List<BriefGameEntity>> {
        val uri = BggContract.Categories.buildCollectionUri(categoryId)
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData collectionDao.loadLinkedCollection(uri, sortBy)
        }
    }
}

