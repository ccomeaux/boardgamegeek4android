package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class WishlistPrioritySorter(context: Context) : CollectionSorter(context) {
    private val priorityDescriptions = context.resources.getStringArray(R.array.wishlist_priority)

    @StringRes
    public override val typeResId = R.string.collection_sort_type_wishlist_priority

    @StringRes
    override val descriptionResId = R.string.collection_sort_wishlist_priority

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.wishListPriority }

    override fun getHeaderText(item: CollectionItemEntity): String {
        return priorityDescriptions.getOrNull(item.wishListPriority) ?: priorityDescriptions[0]
    }
}
