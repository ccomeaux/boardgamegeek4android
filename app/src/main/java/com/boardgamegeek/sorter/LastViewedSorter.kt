package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asPastDaySpan
import com.boardgamegeek.extensions.getLongOrZero
import com.boardgamegeek.provider.BggContract.Games

class LastViewedSorter(context: Context) : CollectionSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_last_viewed

    @StringRes
    public override val typeResId = R.string.collection_sort_type_last_viewed

    override val sortColumn = Games.LAST_VIEWED

    override val isSortDescending = true

    public override fun getHeaderText(cursor: Cursor) =
            cursor.getLongOrZero(sortColumn).asPastDaySpan(context).toString()

    override fun getDisplayInfo(cursor: Cursor) = ""

    override fun getTimestamp(cursor: Cursor) = cursor.getLongOrZero(sortColumn)
}
