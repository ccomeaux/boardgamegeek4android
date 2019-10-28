package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R

class AverageWeightAscendingSorter(context: Context) : AverageWeightSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_average_weight_asc

    @StringRes
    public override val subDescriptionResId = R.string.lightest
}
