package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.getDoubleAsString
import com.boardgamegeek.getFirstChar
import com.boardgamegeek.provider.BggContract.Collection.COLLECTION_SORT_NAME
import com.boardgamegeek.provider.BggContract.Collection.STATS_AVERAGE
import java.text.DecimalFormat

class CollectionNameSorter(context: Context) : CollectionSorter(context) {
    private val displayFormat = DecimalFormat("0.00")
    private val defaultValue = context.getString(R.string.text_unknown)

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_collection_name

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_collection_name

    override val columns: Array<String>
        get() = arrayOf(COLLECTION_SORT_NAME, STATS_AVERAGE)

    public override fun getHeaderText(cursor: Cursor): String {
        return cursor.getFirstChar(COLLECTION_SORT_NAME)
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        return cursor.getDoubleAsString(STATS_AVERAGE, defaultValue, format = displayFormat)
    }
}
