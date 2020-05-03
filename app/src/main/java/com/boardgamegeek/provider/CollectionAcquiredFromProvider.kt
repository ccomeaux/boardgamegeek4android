package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.Collection.SORT_ACQUIRED_FROM
import com.boardgamegeek.provider.BggContract.CollectionColumns.PRIVATE_INFO_ACQUIRED_FROM
import com.boardgamegeek.provider.BggContract.PATH_ACQUIRED_FROM
import com.boardgamegeek.provider.BggContract.PATH_COLLECTION
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CollectionAcquiredFromProvider : BaseProvider() {
    override val path = "$PATH_COLLECTION/$PATH_ACQUIRED_FROM"

    override val defaultSortOrder = SORT_ACQUIRED_FROM

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return SelectionBuilder()
                .table(Tables.COLLECTION)
                .groupBy(PRIVATE_INFO_ACQUIRED_FROM)
    }
}
