package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

class AcquisitionDateSorter(context: Context) : CollectionDateSorter(context) {

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_acquisition_date

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_acquisition_date

    override val sortColumn: String
        get() = Collection.PRIVATE_INFO_ACQUISITION_DATE
}
