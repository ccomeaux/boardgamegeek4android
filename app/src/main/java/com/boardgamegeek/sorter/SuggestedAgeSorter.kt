package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getIntAsString
import com.boardgamegeek.provider.BggContract.Collection

abstract class SuggestedAgeSorter(context: Context) : CollectionSorter(context) {
    private val columnName = Collection.MINIMUM_AGE
    private val defaultValue = context.getString(R.string.text_unknown)

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_suggested_age

    override val sortColumn: String
        get() = columnName

    public override fun getHeaderText(cursor: Cursor): String {
        return cursor.getIntAsString(columnName, defaultValue, true)
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        var info = getHeaderText(cursor)
        if (defaultValue != info) info += "+"
        return "${context.getString(R.string.ages)} $info"
    }
}
