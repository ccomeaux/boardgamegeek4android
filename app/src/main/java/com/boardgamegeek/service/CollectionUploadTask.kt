package com.boardgamegeek.service

import com.boardgamegeek.service.model.CollectionItem
import okhttp3.FormBody
import okhttp3.OkHttpClient

abstract class CollectionUploadTask(client: OkHttpClient) : CollectionTask(client, CollectionItem()) {
    abstract val timestampColumn: String

    abstract val isDirty: Boolean

    open fun addCollectionItem(collectionItem: CollectionItem) {
        this.collectionItem = collectionItem
    }

    @Suppress("SpellCheckingInspection")
    override fun createFormBuilder(): FormBody.Builder {
        return super.createFormBuilder()
                .add("action", "savedata")
                .add("collid", collectionItem.collectionId.toString())
    }
}