package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

class PricePaidSorter(context: Context) : MoneySorter(context) {

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_price_paid

    override val typeResource: Int
        get() = R.string.collection_sort_type_price_paid

    override val amountColumnName: String
        get() = Collection.PRIVATE_INFO_PRICE_PAID

    override val currencyColumnName: String
        get() = Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY
}
