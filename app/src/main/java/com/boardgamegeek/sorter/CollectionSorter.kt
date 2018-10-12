package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import androidx.annotation.StringRes
import com.boardgamegeek.provider.BggContract.Collection

abstract class CollectionSorter(context: Context) : Sorter(context) {

    /**
     * {@inheritDoc}
     */
    override val description: String
        get() {
            var description = super.description
            if (subDescriptionResId != 0) {
                description += " - " + context.getString(subDescriptionResId)
            }
            return description
        }

    @StringRes
    protected open val subDescriptionResId = 0

    override val type: Int
        get() = context.getString(typeResId).toIntOrNull() ?: CollectionSorterFactory.TYPE_DEFAULT

    @get:StringRes
    protected abstract val typeResId: Int

    override val defaultSort = Collection.DEFAULT_SORT

    /**
     * Gets the detail text to display on each row.
     */
    open fun getDisplayInfo(cursor: Cursor) = getHeaderText(cursor)

    open fun getTimestamp(cursor: Cursor) = 0L
}
