package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor

import com.boardgamegeek.R
import com.boardgamegeek.getInt
import com.boardgamegeek.provider.BggContract.Collection
import java.text.NumberFormat

abstract class PlayCountSorter(context: Context) : CollectionSorter(context) {

    override val sortColumn: String
        get() = COLUMN_NAME

    public override fun getHeaderText(cursor: Cursor): String {
        return NUMBER_FORMAT.format(cursor.getInt(COLUMN_NAME))
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        return "${getHeaderText(cursor)} ${context.getString(R.string.plays)}"
    }

    companion object {
        private val NUMBER_FORMAT = NumberFormat.getIntegerInstance()
        private val COLUMN_NAME = Collection.NUM_PLAYS
    }
}
