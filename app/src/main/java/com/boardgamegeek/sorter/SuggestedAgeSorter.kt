package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getIntAsString
import com.boardgamegeek.provider.BggContract.Collection

abstract class SuggestedAgeSorter(context: Context) : CollectionSorter(context) {
    private val defaultValue = context.getString(R.string.text_unknown)

    @StringRes
    override val descriptionResId = R.string.collection_sort_suggested_age

    override val sortColumn = Collection.MINIMUM_AGE

    public override fun getHeaderText(cursor: Cursor): String {
        return cursor.getIntAsString(sortColumn, defaultValue, true)
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        val info = getHeaderText(cursor)
        return when {
            defaultValue != info -> "${context.getString(R.string.ages)} $info+"
            else -> "${context.getString(R.string.ages)} $info"
        }
    }
}
