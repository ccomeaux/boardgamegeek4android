package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes

import com.boardgamegeek.R
import com.boardgamegeek.extensions.getString
import com.boardgamegeek.provider.BggContract.Collection

class AcquiredFromSorter(context: Context) : CollectionSorter(context) {
    private val nowhere = context.getString(R.string.nowhere_in_angle_brackets)

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_acquired_from

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_acquired_from

    override val sortColumn: String
        get() = COLUMN_NAME

    override val shouldCollate: Boolean
        get() = true

    public override fun getHeaderText(cursor: Cursor) = cursor.getString(COLUMN_NAME, nowhere)

    companion object {
        private const val COLUMN_NAME = Collection.PRIVATE_INFO_ACQUIRED_FROM
    }
}
