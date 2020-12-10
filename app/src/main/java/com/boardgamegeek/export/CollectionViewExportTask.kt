package com.boardgamegeek.export

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.boardgamegeek.export.model.CollectionView
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.google.gson.Gson
import com.google.gson.stream.JsonWriter

class CollectionViewExportTask(context: Context, uri: Uri) : JsonExportTask<CollectionView>(context, Constants.TYPE_COLLECTION_VIEWS, uri) {
    override val version: Int
        get() = 1

    override fun getCursor(context: Context): Cursor? {
        return context.contentResolver.query(
                CollectionViews.CONTENT_URI,
                CollectionView.PROJECTION,
                null, null, null)
    }

    override fun writeJsonRecord(context: Context, cursor: Cursor, gson: Gson, writer: JsonWriter) {
        val cv = CollectionView.fromCursor(cursor)
        cv.addFilters(context)
        gson.toJson(cv, CollectionView::class.java, writer)
    }
}