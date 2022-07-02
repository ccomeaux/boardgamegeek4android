package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class CurrentValueSorter(context: Context) : MoneySorter(context) {
    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_current_value_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_current_value

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_current_value

    override fun amount(item: CollectionItemEntity) = item.currentValue

    override fun currency(item: CollectionItemEntity) = item.currentValueCurrency
}
