package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class InventoryLocationSorter(context: Context) : CollectionSorter(context) {
    private val nowhere = context.getString(R.string.nowhere_in_angle_brackets)

    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_inventory_location

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_inventory_location_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_inventory_location

    override fun sortAscending(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.inventoryLocation }

    override fun sortDescending(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.inventoryLocation }

    override fun getHeaderText(item: CollectionItemEntity) = item.inventoryLocation.ifBlank { nowhere }
}
