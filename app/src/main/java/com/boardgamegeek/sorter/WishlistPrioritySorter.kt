package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class WishlistPrioritySorter(context: Context) : CollectionSorter(context) {
    private val priorityDescriptions = context.resources.getStringArray(R.array.wishlist_priority)

    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_wishlist_priority

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_wishlist_priority_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_wishlist_priority

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.wishListPriority }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.wishListPriority }

    override fun getHeaderText(item: CollectionItemEntity): String {
        return priorityDescriptions.getOrNull(item.wishListPriority) ?: priorityDescriptions[0]
    }
}
