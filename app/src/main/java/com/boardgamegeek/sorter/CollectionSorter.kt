package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.provider.BggContract.Collection

abstract class CollectionSorter(protected val context: Context) {
    @get:StringRes
    protected abstract val descriptionResId: Int

    /**
     * Gets the description to display in the UI when this sort is applied. Subclasses should set descriptionId
     * to control this value.
     */
    val description: String
        get() {
            var description = context.getString(descriptionResId)
            if (subDescriptionResId != 0) {
                description += " - " + context.getString(subDescriptionResId)
            }
            return description
        }

    @StringRes
    protected open val subDescriptionResId = 0

    /**
     * The unique type.
     */
    val type: Int
        get() = context.getString(typeResId).toIntOrNull() ?: CollectionSorterFactory.TYPE_DEFAULT

    @get:StringRes
    protected abstract val typeResId: Int

    /**
     * Gets the sort order clause to use in the query.
     */
    val orderByClause: String
        get() = if (sortColumn.isEmpty()) {
            Collection.DEFAULT_SORT
        } else {
            val sortOrder = if (isSortDescending) "DESC" else "ASC"
            val collateNoCase = if (shouldCollate) BggContract.COLLATE_NOCASE else ""
            "$sortColumn $collateNoCase $sortOrder, ${Collection.DEFAULT_SORT}"
        }

    protected open val sortColumn = ""

    protected open val isSortDescending = false

    protected open val shouldCollate = false

    open fun getHeaderText(item: CollectionItemEntity): String = ""

    /**
     * Gets the detail text to display on each row.
     */
    open fun getDisplayInfo(item: CollectionItemEntity) = getHeaderText(item)

    open fun getRating(item: CollectionItemEntity): Double = 0.0

    open fun getRatingText(item: CollectionItemEntity) = ""

    open fun getTimestamp(item: CollectionItemEntity) = 0L
}
