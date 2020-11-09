package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.entities.CollectionItemEntity

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

    open fun sort(items: Iterable<CollectionItemEntity>): List<CollectionItemEntity> = items.toList()

    open fun getHeaderText(item: CollectionItemEntity): String = ""

    /**
     * Gets the detail text to display on each row.
     */
    open fun getDisplayInfo(item: CollectionItemEntity) = getHeaderText(item)

    open fun getRating(item: CollectionItemEntity): Double = 0.0

    open fun getRatingText(item: CollectionItemEntity) = ""

    open fun getTimestamp(item: CollectionItemEntity) = 0L
}
