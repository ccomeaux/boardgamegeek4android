package com.boardgamegeek.export

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import androidx.core.content.contentValuesOf
import com.boardgamegeek.export.model.CollectionView
import com.boardgamegeek.extensions.applyBatch
import com.boardgamegeek.provider.BggContract.CollectionViewFilters
import com.boardgamegeek.provider.BggContract.CollectionViews
import com.google.gson.Gson
import com.google.gson.stream.JsonReader

class CollectionViewImportTask(context: Context, uri: Uri) : JsonImportTask<CollectionView>(context, Constants.TYPE_COLLECTION_VIEWS, uri) {
    override fun initializeImport() {
        context.contentResolver.delete(CollectionViews.CONTENT_URI, null, null)
    }

    override fun parseItem(gson: Gson, reader: JsonReader): CollectionView {
        return gson.fromJson(reader, CollectionView::class.java)
    }

    override fun importRecord(item: CollectionView, version: Int) {
        val values = contentValuesOf(
                CollectionViews.NAME to item.name,
                CollectionViews.STARRED to item.starred,
                CollectionViews.SORT_TYPE to item.sortType,
        )
        val uri = context.contentResolver.insert(CollectionViews.CONTENT_URI, values)
        val viewId = CollectionViews.getViewId(uri)
        val filterUri = CollectionViews.buildViewFilterUri(viewId.toLong())
        val batch = arrayListOf<ContentProviderOperation>()
        for (filter in item.filters) {
            val builder = ContentProviderOperation.newInsert(filterUri)
                    .withValue(CollectionViewFilters.TYPE, filter.type)
                    .withValue(CollectionViewFilters.DATA, filter.data)
            batch.add(builder.build())
        }
        context.contentResolver.applyBatch(batch)
    }
}