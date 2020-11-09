package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class LastPlayDateDescendingSorter(context: Context) : LastPlayDateSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_play_date_max

    @StringRes
    override val descriptionResId = R.string.collection_sort_play_date_max

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedByDescending { it.lastPlayDate }
}