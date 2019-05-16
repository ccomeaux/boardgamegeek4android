package com.boardgamegeek.db

import androidx.lifecycle.LiveData
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.BriefGameEntity
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.entities.YEAR_UNKNOWN
import com.boardgamegeek.extensions.*
import com.boardgamegeek.livedata.RegisteredLiveData
import com.boardgamegeek.provider.BggContract

class CategoryDao(private val context: BggApplication) {
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

    fun loadCollectionAsLiveData(categoryId: Int): LiveData<List<BriefGameEntity>>? {
        return RegisteredLiveData(context, BggContract.Categories.buildCollectionUri(categoryId), true) {
            return@RegisteredLiveData loadCollection(categoryId)
        }
    }

    private fun loadCollection(categoryId: Int): List<BriefGameEntity> {
        val list = arrayListOf<BriefGameEntity>()
        context.contentResolver.load(
                BggContract.Categories.buildCollectionUri(categoryId),
                arrayOf(
                        "games." + BggContract.Collection.GAME_ID,
                        BggContract.Collection.GAME_NAME,
                        BggContract.Collection.COLLECTION_NAME,
                        BggContract.Collection.COLLECTION_YEAR_PUBLISHED,
                        BggContract.Collection.COLLECTION_THUMBNAIL_URL,
                        BggContract.Collection.THUMBNAIL_URL,
                        BggContract.Collection.HERO_IMAGE_URL
                ),
                sortOrder = BggContract.Collection.GAME_SORT_NAME.collateNoCase().ascending()
        )?.use {
            if (it.moveToFirst()) {
                do {
                    list += BriefGameEntity(
                            it.getInt(BggContract.Collection.GAME_ID),
                            it.getStringOrEmpty(BggContract.Collection.GAME_NAME),
                            it.getStringOrEmpty(BggContract.Collection.COLLECTION_NAME),
                            it.getIntOrNull(BggContract.Collection.COLLECTION_YEAR_PUBLISHED) ?: YEAR_UNKNOWN,
                            it.getStringOrEmpty(BggContract.Collection.COLLECTION_THUMBNAIL_URL),
                            it.getStringOrEmpty(BggContract.Collection.THUMBNAIL_URL),
                            it.getStringOrEmpty(BggContract.Collection.HERO_IMAGE_URL)
                    )
                } while (it.moveToNext())
            }
        }
        return list
    }
}

