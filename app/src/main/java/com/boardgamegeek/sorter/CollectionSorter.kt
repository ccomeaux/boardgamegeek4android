package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.entities.CollectionItemEntity

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

    open fun sortAscending(items: Iterable<CollectionItemEntity>): List<CollectionItemEntity> = items.toList()

    open fun sortDescending(items: Iterable<CollectionItemEntity>): List<CollectionItemEntity> = sortAscending(items).reversed()

    fun sort(items: Iterable<CollectionItemEntity>, direction: Boolean) = if (direction) sortDescending(items) else sortAscending(items)

    open fun getHeaderText(item: CollectionItemEntity): String = ""

    /**
     * Gets the detail text to display on each row.
     */
    open fun getDisplayInfo(item: CollectionItemEntity) = getHeaderText(item)

    open fun getRating(item: CollectionItemEntity): Double = 0.0

    open fun getRatingText(item: CollectionItemEntity) = ""

    open fun getTimestamp(item: CollectionItemEntity) = 0L
}
