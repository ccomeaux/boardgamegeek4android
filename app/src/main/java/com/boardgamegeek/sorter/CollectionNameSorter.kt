package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.asRating
import com.boardgamegeek.extensions.getDouble
import com.boardgamegeek.extensions.getFirstChar
import com.boardgamegeek.provider.BggContract.Collection.COLLECTION_SORT_NAME
import com.boardgamegeek.provider.BggContract.Collection.STATS_AVERAGE

class CollectionNameSorter(context: Context) : CollectionSorter(context) {
    @StringRes
    override val descriptionResId = R.string.collection_sort_collection_name

    @StringRes
    public override val typeResId = R.string.collection_sort_type_collection_name

    override val columns = arrayOf(COLLECTION_SORT_NAME, STATS_AVERAGE)

    public override fun getHeaderText(cursor: Cursor) = cursor.getFirstChar(COLLECTION_SORT_NAME)

    override fun getRating(cursor: Cursor) = cursor.getDouble(STATS_AVERAGE)

    override fun getRatingText(cursor: Cursor) = getRating(cursor).asRating(context, R.string.unrated_abbr)
}
