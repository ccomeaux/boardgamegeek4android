package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getApiTime
import com.boardgamegeek.provider.BggContract.Plays
import com.boardgamegeek.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*

class PlaysDateSorter(context: Context) : PlaysSorter(context) {
    private val defaultValue = context.getString(R.string.text_unknown)

    override val descriptionId: Int
        @StringRes
        get() = R.string.menu_plays_sort_date

    override val type: Int
        get() = PlaysSorterFactory.TYPE_PLAY_DATE

    override val columns: Array<String>
        get() = arrayOf(Plays.DATE)

    public override fun getHeaderText(cursor: Cursor): String {
        val time = cursor.getApiTime(Plays.DATE)
        return if (time == DateTimeUtils.UNKNOWN_DATE) defaultValue else DATE_FORMAT.format(time)
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    }
}
