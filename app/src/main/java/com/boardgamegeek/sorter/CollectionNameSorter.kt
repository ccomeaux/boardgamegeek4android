package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getDoubleAsString
import com.boardgamegeek.extensions.getFirstChar
import com.boardgamegeek.provider.BggContract.Collection.COLLECTION_SORT_NAME
import com.boardgamegeek.provider.BggContract.Collection.STATS_AVERAGE
import java.text.DecimalFormat

class CollectionNameSorter(context: Context) : CollectionSorter(context) {
    private val displayFormat = DecimalFormat("0.00")
    private val defaultValue = context.getString(R.string.text_unknown)

    @StringRes
    override val descriptionResId = R.string.collection_sort_collection_name

    @StringRes
    public override val typeResId = R.string.collection_sort_type_collection_name

    override val columns = arrayOf(COLLECTION_SORT_NAME, STATS_AVERAGE)

    public override fun getHeaderText(cursor: Cursor) = cursor.getFirstChar(COLLECTION_SORT_NAME)

    override fun getDisplayInfo(cursor: Cursor) = cursor.getDoubleAsString(STATS_AVERAGE, defaultValue, format = displayFormat)
}
