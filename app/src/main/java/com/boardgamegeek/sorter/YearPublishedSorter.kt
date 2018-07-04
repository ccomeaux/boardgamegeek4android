package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getInt
import com.boardgamegeek.provider.BggContract.Collection
import com.boardgamegeek.util.PresentationUtils

abstract class YearPublishedSorter(context: Context) : CollectionSorter(context) {
    private val columnName = Collection.YEAR_PUBLISHED

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_year_published

    override val sortColumn: String
        get() = columnName

    public override fun getHeaderText(cursor: Cursor): String {
        return PresentationUtils.describeYear(context, cursor.getInt(columnName))
    }
}
