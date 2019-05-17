package com.boardgamegeek.db

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract

class CategoryDao(private val context: BggApplication) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT
    }

    fun loadCategoriesAsLiveData(sortBy: SortType = SortType.NAME): LiveData<List<CategoryEntity>> {
        return RegisteredLiveData(context, BggContract.Categories.CONTENT_URI, true) {
            return@RegisteredLiveData loadCategories(sortBy)
        }
    }

    private fun loadCategories(sortBy: SortType): List<CategoryEntity> {
        val results = arrayListOf<CategoryEntity>()
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
                            it.getInt(BggContract.Categories.CATEGORY_ID),
                            it.getStringOrEmpty(BggContract.Categories.CATEGORY_NAME),
                            it.getIntOrZero(BggContract.Categories.ITEM_COUNT)
                    )
                } while (it.moveToNext())
            }
        }
        return results
    }

    fun loadCollectionAsLiveData(categoryId: Int, sortBy: CollectionDao.SortType): LiveData<List<BriefGameEntity>>? {
        val uri = BggContract.Categories.buildCollectionUri(categoryId)
        return RegisteredLiveData(context, uri, true) {
            return@RegisteredLiveData collectionDao.loadLinkedCollection(uri, sortBy)
        }
    }
}

