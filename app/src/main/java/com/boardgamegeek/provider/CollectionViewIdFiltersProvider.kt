package com.boardgamegeek.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.boardgamegeek.provider.BggContract.*
import com.boardgamegeek.provider.BggDatabase.Tables
import com.boardgamegeek.util.SelectionBuilder

class CollectionViewIdFiltersProvider : BaseProvider() {
    override fun getType(uri: Uri) = CollectionViewFilters.CONTENT_TYPE

    override val path = "$PATH_COLLECTION_VIEWS/#/$PATH_FILTERS"

    override val defaultSortOrder = CollectionViewFilters.DEFAULT_SORT

    override fun buildSimpleSelection(uri: Uri): SelectionBuilder {
        return buildSelection(uri, Tables.COLLECTION_VIEW_FILTERS, CollectionViewFilters.VIEW_ID)
    }

    override fun buildExpandedSelection(uri: Uri): SelectionBuilder {
        return buildSelection(uri,
                Tables.COLLECTION_VIEW_FILTERS_JOIN_COLLECTION_VIEWS,
                "${Tables.COLLECTION_VIEWS}.${CollectionViews._ID}")
    }

    override fun insert(context: Context, db: SQLiteDatabase, uri: Uri, values: ContentValues): Uri? {
        val filterId = CollectionViews.getViewId(uri).toLong()
        values.put(CollectionViewFilters.VIEW_ID, filterId)
        val rowId = db.insertOrThrow(Tables.COLLECTION_VIEW_FILTERS, null, values)
        return CollectionViews.buildViewFilterUri(filterId, rowId)
    }

    private fun buildSelection(uri: Uri, table: String, idColumnName: String): SelectionBuilder {
        val filterId = CollectionViews.getViewId(uri).toLong()
        return SelectionBuilder().table(table)
                .mapIfNullToTable(CollectionViewFilters._ID, Tables.COLLECTION_VIEW_FILTERS, "0")
                .where("$idColumnName=?", filterId.toString())
    }
}
