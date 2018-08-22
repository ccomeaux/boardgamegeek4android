package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.getString
import com.boardgamegeek.provider.BggContract.Collection

class InventoryLocationSorter(context: Context) : CollectionSorter(context) {
    private val nowhere = context.getString(R.string.nowhere_in_angle_brackets)

    @StringRes
    override val descriptionResId = R.string.collection_sort_inventory_location

    @StringRes
    public override val typeResId = R.string.collection_sort_type_inventory_location

    override val sortColumn = Collection.PRIVATE_INFO_INVENTORY_LOCATION

    override val shouldCollate = true

    public override fun getHeaderText(cursor: Cursor) = cursor.getString(sortColumn, nowhere)
}
