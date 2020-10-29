package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Collection

class CurrentValueSorter(context: Context) : MoneySorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_current_value

    @StringRes
    override val typeResId = R.string.collection_sort_type_current_value

    override val amountColumnName = Collection.PRIVATE_INFO_CURRENT_VALUE
    override val currencyColumnName = Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY

    override fun amount(item: CollectionItemEntity) = item.currentValue
    override fun currency(item: CollectionItemEntity) = item.currentValueCurrency
}
