package com.boardgamegeek.provider

import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CollectionViewIdFiltersIdProvider : BaseProvider() {
    override fun getType(uri: Uri) = CollectionViewFilters.CONTENT_ITEM_TYPE

    override val path = "$PATH_COLLECTION_VIEWS/#/$PATH_FILTERS/#"

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS_JOIN_COLLECTION_VIEWS)
    }

    private fun buildSelection(uri: Uri, table: String): SelectionBuilder {
        val filterId = CollectionViews.getViewId(uri).toLong()
        val type = CollectionViewFilters.getFilterType(uri)
        return SelectionBuilder().table(table)
                .where("${CollectionViewFilters.VIEW_ID}=?", filterId.toString())
                .where("${CollectionViewFilters.TYPE}=?", type.toString())
    }
}
