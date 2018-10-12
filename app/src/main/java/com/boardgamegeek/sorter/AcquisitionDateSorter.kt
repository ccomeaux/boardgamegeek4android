package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Collection

class AcquisitionDateSorter(context: Context) : CollectionDateSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_acquisition_date

    @StringRes
    public override val typeResId = R.string.collection_sort_type_acquisition_date

    override val sortColumn = Collection.PRIVATE_INFO_ACQUISITION_DATE
}
