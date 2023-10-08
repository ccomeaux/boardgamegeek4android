package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItem

class PricePaidSorter(context: Context) : MoneySorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_price_paid_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_price_paid

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_price_paid

    override fun amount(item: CollectionItem) = item.pricePaid
    override fun currency(item: CollectionItem) = item.pricePaidCurrency
}
