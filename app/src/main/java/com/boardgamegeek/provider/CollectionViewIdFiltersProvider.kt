package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.boardgamegeek.provider.BggContract.Companion.PATH_COLLECTION_VIEWS
import com.boardgamegeek.provider.BggContract.Companion.PATH_FILTERS
import com.boardgamegeek.provider.BggDatabase.Tables

class CollectionViewIdFiltersProvider : BaseProvider() {
    override fun getType(uri: Uri) = CollectionViewFilters.CONTENT_TYPE

    override val path = "$PATH_COLLECTION_VIEWS/#/$PATH_FILTERS"

    override val defaultSortOrder = CollectionViewFilters.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS_JOIN_COLLECTION_VIEWS)
    }

    @Suppress("RedundantNullableReturnType")
    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val viewId = CollectionViews.getViewId(uri)
        values.put(CollectionViewFilters.Columns.VIEW_ID, viewId)
        val rowId = db.insertOrThrow(Tables.COLLECTION_VIEW_FILTERS, null, values)
        return CollectionViewFilters.buildViewFilterUri(viewId, rowId)
    }

    private fun buildSelection(uri: Uri, table: String): SelectionBuilder {
        val filterId = CollectionViews.getViewId(uri).toLong()
        return SelectionBuilder().table(table)
            .mapToTable(BaseColumns._ID, Tables.COLLECTION_VIEW_FILTERS)
            .where("${CollectionViewFilters.Columns.VIEW_ID}=?", filterId.toString())
    }
}
