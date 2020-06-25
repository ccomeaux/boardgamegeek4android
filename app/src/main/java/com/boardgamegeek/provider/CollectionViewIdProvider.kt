package com.boardgamegeek.provider

import android.net.Uri
import android.provider.BaseColumns._ID
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.provider.BggContract.PATH_COLLECTION_VIEWS
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CollectionViewIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = CollectionViews.CONTENT_ITEM_TYPE

    override val path = "$PATH_COLLECTION_VIEWS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        val filterId = CollectionViews.getViewId(uri).toLong()
        return SelectionBuilder()
                .table(Tables.COLLECTION_VIEWS)
                .where("$_ID=?", filterId.toString())
    }
}