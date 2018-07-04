package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getInt
import com.boardgamegeek.provider.BggContract.Collection

class WishlistPrioritySorter(context: Context) : CollectionSorter(context) {
    private val columnName = Collection.STATUS_WISHLIST_PRIORITY
    private val priorityDescriptions = context.resources.getStringArray(R.array.wishlist_priority)

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_wishlist_priority

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_wishlist_priority

    override val sortColumn: String
        get() = columnName

    public override fun getHeaderText(cursor: Cursor): String {
        var level = cursor.getInt(columnName)
        if (level >= priorityDescriptions.size) level = 0
        return priorityDescriptions[level]
    }
}
