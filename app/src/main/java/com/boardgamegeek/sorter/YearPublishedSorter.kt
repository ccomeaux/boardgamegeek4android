package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asYear
import com.boardgamegeek.extensions.getInt
import com.boardgamegeek.provider.BggContract.Collection

abstract class YearPublishedSorter(context: Context) : CollectionSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_year_published

    override val sortColumn = Collection.YEAR_PUBLISHED

    public override fun getHeaderText(cursor: Cursor) = cursor.getInt(sortColumn).asYear(context)
}
