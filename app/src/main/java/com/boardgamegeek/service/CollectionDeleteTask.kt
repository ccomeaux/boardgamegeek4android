package com.boardgamegeek.service

import android.content.ContentValues
import com.boardgamegeek.provider.BggContract
import com.boardgamegeek.service.model.CollectionItem
import okhttp3.FormBody
import okhttp3.OkHttpClient

class CollectionDeleteTask(client: OkHttpClient, collectionItem: CollectionItem) : CollectionTask(client, collectionItem) {
    override fun createForm(): FormBody {
        @Suppress("SpellCheckingInspection")
        return super.createFormBuilder()
                .add("collid", collectionItem.collectionId.toString())
                .add("action", "delete")
                .build()
    }

    override fun appendContentValues(contentValues: ContentValues) {
        contentValues.put(BggContract.Collection.COLLECTION_DIRTY_TIMESTAMP, 0)
    }
}