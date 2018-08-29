package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class SuggestedAgeAscendingSorter(context: Context) : SuggestedAgeSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_suggested_age_asc

    @StringRes
    public override val subDescriptionResId = R.string.youngest
}
