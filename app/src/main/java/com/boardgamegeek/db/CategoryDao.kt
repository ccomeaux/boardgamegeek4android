package com.boardgamegeek.db

import android.content.Context
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.BggApplication
import com.boardgamegeek.entities.CategoryEntity
import com.boardgamegeek.extensions.ascending
import com.boardgamegeek.extensions.collateNoCase
import com.boardgamegeek.extensions.descending
import com.boardgamegeek.extensions.loadList
import com.boardgamegeek.provider.BggContract.Categories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryDao(private val context: Context) {
    private val collectionDao = CollectionDao(context)

    enum class SortType {
        NAME, ITEM_COUNT
    }

    suspend fun loadCategories(sortBy: SortType): List<CategoryEntity> = withContext(Dispatchers.IO) {
        val sortByName = Categories.Columns.CATEGORY_NAME.collateNoCase().ascending()
        val sortOrder = when (sortBy) {
            SortType.NAME -> sortByName
            SortType.ITEM_COUNT -> Categories.Columns.ITEM_COUNT.descending().plus(", $sortByName")
        }
        context.contentResolver.loadList(
            Categories.CONTENT_URI,
            arrayOf(
                Categories.Columns.CATEGORY_ID,
                Categories.Columns.CATEGORY_NAME,
                Categories.Columns.ITEM_COUNT
            ),
            sortOrder = sortOrder
        ) {
            CategoryEntity(
                it.getInt(0),
                it.getStringOrNull(1).orEmpty(),
                it.getIntOrNull(2) ?: 0,
            )
        }
    }

    suspend fun loadCollection(categoryId: Int, sortBy: CollectionDao.SortType) =
        collectionDao.loadLinkedCollection(Categories.buildCollectionUri(categoryId), sortBy)

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Categories.CONTENT_URI, null, null)
    }
}
