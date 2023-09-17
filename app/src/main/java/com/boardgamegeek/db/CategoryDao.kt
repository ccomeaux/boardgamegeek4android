package com.boardgamegeek.db

import android.content.Context
import android.provider.BaseColumns
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.boardgamegeek.db.model.CategoryLocal
import com.boardgamegeek.extensions.ascending
import com.boardgamegeek.extensions.collateNoCase
import com.boardgamegeek.extensions.descending
import com.boardgamegeek.extensions.loadList
import com.boardgamegeek.provider.BggContract.Categories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryDao(private val context: Context) {
    enum class SortType {
        NAME, ITEM_COUNT
    }

    suspend fun loadCategories(sortBy: SortType): List<CategoryLocal> = withContext(Dispatchers.IO) {
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
                Categories.Columns.ITEM_COUNT,
                BaseColumns._ID,
            ),
            sortOrder = sortOrder
        ) {
            CategoryLocal(
                it.getInt(3),
                it.getInt(0),
                it.getStringOrNull(1).orEmpty(),
                it.getIntOrNull(2) ?: 0,
            )
        }
    }

    suspend fun delete(): Int = withContext(Dispatchers.IO) {
        context.contentResolver.delete(Categories.CONTENT_URI, null, null)
    }
}
