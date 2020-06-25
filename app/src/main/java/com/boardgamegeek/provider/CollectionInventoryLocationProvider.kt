package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Collection.SORT_INVENTORY_LOCATION
import com.boardgamegeek.provider.BggContract.CollectionColumns.PRIVATE_INFO_INVENTORY_LOCATION
import com.boardgamegeek.provider.BggContract.PATH_COLLECTION
import com.boardgamegeek.provider.BggContract.PATH_INVENTORY_LOCATION
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CollectionInventoryLocationProvider : BaseProvider() {
    override val path = "$PATH_COLLECTION/$PATH_INVENTORY_LOCATION"

    override val defaultSortOrder = SORT_INVENTORY_LOCATION

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
                .table(Tables.COLLECTION)
                .groupBy(PRIVATE_INFO_INVENTORY_LOCATION)
    }
}
