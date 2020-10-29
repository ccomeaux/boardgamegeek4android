package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Collection

class PricePaidSorter(context: Context) : MoneySorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_price_paid

    @StringRes
    override val typeResId = R.string.collection_sort_type_price_paid

    override val amountColumnName = Collection.PRIVATE_INFO_PRICE_PAID
    override val currencyColumnName = Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY

    override fun amount(item: CollectionItemEntity) = item.pricePaid
    override fun currency(item: CollectionItemEntity) = item.pricePaidCurrency
}
