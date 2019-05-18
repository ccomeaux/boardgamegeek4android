package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor

import com.boardgamegeek.R
import com.boardgamegeek.extensions.asRating
import com.boardgamegeek.extensions.getDouble
import com.boardgamegeek.extensions.getDoubleAsString

import java.text.DecimalFormat

abstract class RatingSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue: String = context.getString(R.string.text_unknown)

    override val isSortDescending = true

    protected abstract val displayFormat: DecimalFormat

    public override fun getHeaderText(cursor: Cursor) = cursor.getDoubleAsString(sortColumn, defaultValue)

    override fun getRating(cursor: Cursor) = cursor.getDouble(sortColumn)

    override fun getRatingText(cursor: Cursor) = getRating(cursor).asRating(context, R.string.unrated_abbr)
}
