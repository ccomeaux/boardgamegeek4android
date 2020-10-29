package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.provider.BggContract.Collection

class AcquisitionDateSorter(context: Context) : CollectionDateSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_acquisition_date

    @StringRes
    override val descriptionResId = R.string.collection_sort_acquisition_date

    override val sortColumn = Collection.PRIVATE_INFO_ACQUISITION_DATE

    override fun getTimestamp(item: CollectionItemEntity) = item.acquisitionDate
}
