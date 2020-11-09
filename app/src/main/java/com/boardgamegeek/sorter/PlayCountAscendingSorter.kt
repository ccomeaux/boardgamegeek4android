package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity

class PlayCountAscendingSorter(context: Context) : PlayCountSorter(context) {
    @StringRes
    public override val typeResId = R.string.collection_sort_type_play_count_asc

    @StringRes
    override val descriptionResId = R.string.collection_sort_play_count_asc

    override fun sort(items: Iterable<CollectionItemEntity>) = items.sortedBy { it.numberOfPlays }
}
