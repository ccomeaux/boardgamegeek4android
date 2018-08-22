package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPastDaySpan
import com.boardgamegeek.extensions.getApiTime
import java.text.SimpleDateFormat
import java.util.*

abstract class CollectionDateSorter(context: Context) : CollectionSorter(context) {
    override val isSortDescending = true

    public override fun getHeaderText(cursor: Cursor): String {
        val time = cursor.getApiTime(sortColumn)
        return if (time == 0L) context.getString(defaultValueResId) else DISPLAY_FORMAT.format(time)
    }

    override fun getDisplayInfo(cursor: Cursor) =
            cursor.getApiTime(sortColumn).asPastDaySpan(context, defaultValueResId).toString()

    @StringRes
    protected open val defaultValueResId = R.string.text_unknown

    companion object {
        private val DISPLAY_FORMAT = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    }
}
