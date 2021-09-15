package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class SuggestedAgeDescendingSorter(context: Context) : SuggestedAgeSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_suggested_age_desc

    @StringRes
    public override val subDescriptionResId = R.string.oldest

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.minimumAge }
}
