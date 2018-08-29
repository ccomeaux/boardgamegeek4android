package com.boardgamegeek.sorter

import android.content.Context
import android.support.annotation.StringRes

import com.boardgamegeek.R

class SuggestedAgeDescendingSorter(context: Context) : SuggestedAgeSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_suggested_age_desc

    @StringRes
    public override val subDescriptionResId = R.string.oldest

    override val isSortDescending = true
}
