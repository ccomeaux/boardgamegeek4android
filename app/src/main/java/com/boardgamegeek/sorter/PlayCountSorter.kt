package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getInt
import com.boardgamegeek.provider.BggContract.Collection
import java.text.NumberFormat

abstract class PlayCountSorter(context: Context) : CollectionSorter(context) {

    override val sortColumn = Collection.NUM_PLAYS

    public override fun getHeaderText(cursor: Cursor) = NUMBER_FORMAT.format(cursor.getInt(sortColumn))!!

    override fun getDisplayInfo(cursor: Cursor) = "${getHeaderText(cursor)} ${context.getString(R.string.plays)}"

    companion object {
        private val NUMBER_FORMAT = NumberFormat.getIntegerInstance()
    }
}
