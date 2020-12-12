package com.boardgamegeek.export

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.export.model.CollectionView
import com.boardgamegeek.export.model.Filter
import com.boardgamegeek.extensions.getIntOrNull
import com.boardgamegeek.extensions.load
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter

class CollectionViewExportTask(context: Context, uri: Uri) : JsonExportTask<CollectionView>(context, Constants.TYPE_COLLECTION_VIEWS, uri) {
    override val version: Int
        get() = 1

    override fun getCursor(context: Context): Cursor? {
        return context.contentResolver.query(
                CollectionViews.CONTENT_URI,
                arrayOf(
                        CollectionViews._ID,
                        CollectionViews.NAME,
                        CollectionViews.SORT_TYPE,
                        CollectionViews.STARRED
                ),
                null, null, null)
    }

    override fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter) {
        val filters = mutableListOf<Filter>()
        context.contentResolver.load(
                CollectionViews.buildViewFilterUri(cursor.getLong(0)),
                arrayOf(
                        CollectionViewFilters._ID,
                        CollectionViewFilters.TYPE,
                        CollectionViewFilters.DATA
                )
        )?.use {
            while (it.moveToNext()) {
                (it.getIntOrNull(1) ?: 0).also { type ->
                    if (type > 0) filters.add(Filter(type, it.getString(2).orEmpty()))
                }
            }
        }

        gson.toJson(CollectionView(
                cursor.getString(1),
                cursor.getInt(2),
                cursor.getInt(3) == 1,
                filters,
        ), CollectionView::class.java, writer)
    }
}