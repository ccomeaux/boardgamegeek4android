package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.asMinutes

class PlayTimeSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.resources.getString(R.string.text_unknown)

    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_play_time_asc

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_play_time_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_play_time

    override fun sortAscending(items: Iterable<CollectionItem>) = items.sortedBy { it.playingTime }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.playingTime }

    override fun getHeaderText(item: CollectionItem): String {
        val minutes = item.playingTime
        return when {
            minutes == 0 -> defaultValue
            minutes >= 120 -> "${minutes / 60} ${context.getString(R.string.hours_abbr)}"
            else -> "$minutes ${context.getString(R.string.minutes_abbr)}"
        }
    }

    override fun getDisplayInfo(item: CollectionItem) = item.playingTime.asMinutes(context)
}
