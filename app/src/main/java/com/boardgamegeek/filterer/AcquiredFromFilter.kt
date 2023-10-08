package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItem

class AcquiredFromFilter(context: Context) : CollectionTextFilter(context) {
    override val typeResourceId = R.string.collection_filter_type_acquired_from

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_shopping_cart_24

    override fun chipText() = chipText(context.getString(R.string.from))

    override fun description() = description(context.getString(R.string.acquired_from))

    override fun filter(item: CollectionItem) = filterByText(item.acquiredFrom)
}
