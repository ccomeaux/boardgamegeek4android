package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getInt
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.PresentationUtils

abstract class TotalTimePlayedSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.resources.getString(R.string.text_unknown)

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_total_time_played

    override val sortColumn: String
        get() = Collection.PLAYED_TIME

    public override fun getHeaderText(cursor: Cursor): String {
        val minutes = cursor.getInt(Collection.PLAYED_TIME)
        if (minutes == 0) return defaultValue

        return if (minutes >= 120) {
            val hours = minutes / 60
            "$hours ${context.getString(R.string.hours_abbr)}"
        } else {
            "$minutes ${context.getString(R.string.minutes_abbr)}"
        }
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        val minutes = cursor.getInt(Collection.PLAYED_TIME)
        return PresentationUtils.describeMinutes(context, minutes).toString()
    }
}
