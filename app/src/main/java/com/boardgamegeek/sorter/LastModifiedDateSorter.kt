package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPastDaySpan
import com.boardgamegeek.extensions.getLongOrZero
import com.boardgamegeek.provider.BggContract

class LastModifiedDateSorter(context: Context) : CollectionDateSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_last_modified

    @StringRes
    public override val typeResId = R.string.collection_sort_type_last_modified

    override val sortColumn = BggContract.Collection.LAST_MODIFIED

    override val isSortDescending = true

    override fun getHeaderText(cursor: Cursor) =
            cursor.getLongOrZero(sortColumn).asPastDaySpan(context).toString()

    override fun getDisplayInfo(cursor: Cursor) = ""

    override fun getTimestamp(cursor: Cursor) = cursor.getLongOrZero(sortColumn)
}
