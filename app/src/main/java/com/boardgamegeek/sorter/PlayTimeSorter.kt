package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asMinutes
import com.boardgamegeek.extensions.getInt
import com.boardgamegeek.provider.BggContract.Collection

abstract class PlayTimeSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.resources.getString(R.string.text_unknown)

    @StringRes
    override val descriptionResId = R.string.collection_sort_play_time

    override val sortColumn = Collection.PLAYING_TIME

    public override fun getHeaderText(cursor: Cursor): String {
        val minutes = cursor.getInt(sortColumn)
        if (minutes == 0) return defaultValue

        return if (minutes >= 120) {
            val hours = minutes / 60
            "$hours ${context.getString(R.string.hours_abbr)}"
        } else {
            "$minutes ${context.getString(R.string.minutes_abbr)}"
        }
    }

    override fun getDisplayInfo(cursor: Cursor) = cursor.getInt(sortColumn).asMinutes(context)
}
