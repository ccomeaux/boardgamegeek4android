package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class AcquisitionDateSorter(context: Context) : CollectionDateSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_acquisition_date

    @StringRes
    override val descriptionResId = R.string.collection_sort_acquisition_date

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.acquisitionDate }

    override fun getTimestamp(item: CollectionItemEntity) = item.acquisitionDate
}
