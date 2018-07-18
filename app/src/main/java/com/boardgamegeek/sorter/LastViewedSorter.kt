package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import android.text.format.DateUtils

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getLong
import com.boardgamegeek.provider.BggContract.Games

class LastViewedSorter(context: Context) : CollectionSorter(context) {
    private val never: String = context.getString(R.string.never)

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_last_viewed

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_last_viewed

    override val sortColumn: String
        get() = COLUMN_NAME

    override val isSortDescending: Boolean
        get() = true

    public override fun getHeaderText(cursor: Cursor): String {
        val time = cursor.getLong(COLUMN_NAME)
        return if (time == 0L) {
            never
        } else DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        return ""
    }

    override fun getTimestamp(cursor: Cursor): Long {
        return cursor.getLong(COLUMN_NAME)
    }

    companion object {
        private const val COLUMN_NAME = Games.LAST_VIEWED
    }
}
