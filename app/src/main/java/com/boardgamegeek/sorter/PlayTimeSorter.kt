package com.boardgamegeek.sorter

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.asMinutes

abstract class PlayTimeSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.resources.getString(R.string.text_unknown)

    @StringRes
    override val descriptionResId = R.string.collection_sort_play_time

    override fun getHeaderText(item: CollectionItemEntity): String {
        val minutes = item.playingTime
        return when {
            minutes == 0 -> defaultValue
            minutes >= 120 -> "${minutes / 60} ${context.getString(R.string.hours_abbr)}"
            else -> "$minutes ${context.getString(R.string.minutes_abbr)}"
        }
    }

    override fun getDisplayInfo(item: CollectionItemEntity) = item.playingTime.asMinutes(context)
}
