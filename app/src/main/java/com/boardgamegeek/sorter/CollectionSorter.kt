package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.entities.CollectionItem

abstract class CollectionSorter(protected val context: Context) {
    @get:StringRes
    protected abstract val descriptionResId: Int

    val description: String
        get() = context.getString(descriptionResId)

    @get:StringRes
    protected abstract val ascendingSortTypeResId: Int

    @get:StringRes
    protected abstract val descendingSortTypeResId: Int

    val ascendingSortType: Int
        get() = context.getString(ascendingSortTypeResId).toIntOrNull() ?: CollectionSorterFactory.TYPE_DEFAULT

    val descendingSortType: Int
        get() = context.getString(descendingSortTypeResId).toIntOrNull() ?: CollectionSorterFactory.TYPE_DEFAULT

    fun getType(direction: Boolean) = if (direction) descendingSortType else ascendingSortType

    open fun sortAscending(items: Iterable<CollectionItem>): List<CollectionItem> = items.toList()

    open fun sortDescending(items: Iterable<CollectionItem>): List<CollectionItem> = sortAscending(items).reversed()

    fun sort(items: Iterable<CollectionItem>, direction: Boolean) = if (direction) sortDescending(items) else sortAscending(items)

    open fun getHeaderText(item: CollectionItem): String = ""

    /**
     * Gets the detail text to display on each row.
     */
    open fun getDisplayInfo(item: CollectionItem) = getHeaderText(item)

    open fun getRating(item: CollectionItem): Double = 0.0

    open fun getRatingText(item: CollectionItem) = ""

    open fun getTimestamp(item: CollectionItem) = 0L
}
