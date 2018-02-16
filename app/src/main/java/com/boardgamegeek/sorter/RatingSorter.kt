package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor

import com.boardgamegeek.R
import com.boardgamegeek.getDoubleAsString

import java.text.DecimalFormat

abstract class RatingSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue: String = context.getString(R.string.text_unknown)

    override val isSortDescending: Boolean
        get() = true

    protected abstract val displayFormat: DecimalFormat

    public override fun getHeaderText(cursor: Cursor): String {
        return cursor.getDoubleAsString(sortColumn, defaultValue)
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        return cursor.getDoubleAsString(sortColumn, defaultValue, format = displayFormat)
    }
}
