package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

class CurrentValueSorter(context: Context) : MoneySorter(context) {

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_current_value

    override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_current_value

    override val amountColumnName: String
        get() = Collection.PRIVATE_INFO_CURRENT_VALUE

    override val currencyColumnName: String
        get() = Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY
}
