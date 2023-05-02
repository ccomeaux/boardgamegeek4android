package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class InventoryLocationFilter(context: Context) : CollectionTextFilter(context) {
    override val typeResourceId = R.string.collection_filter_type_inventory_location

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_location_on_24

    override fun chipText() = chipText(context.getString(R.string.at))

    override fun description() = description(context.getString(R.string.inventory_location))

    override fun filter(item: CollectionItemEntity) = filterByText(item.inventoryLocation)
}
