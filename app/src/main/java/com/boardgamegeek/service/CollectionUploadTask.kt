package com.boardgamegeek.service

import com.boardgamegeek.entities.CollectionItemForUploadEntity
import okhttp3.FormBody
import okhttp3.OkHttpClient

abstract class CollectionUploadTask(client: OkHttpClient) : CollectionTask(client, CollectionItemForUploadEntity()) {
    abstract val timestampColumn: String

    abstract val isDirty: Boolean

    open fun addCollectionItem(collectionItem: CollectionItemForUploadEntity) {
        this.collectionItem = collectionItem
    }

    @Suppress("SpellCheckingInspection")
    override fun createFormBuilder(): FormBody.Builder {
        return super.createFormBuilder()
                .add("action", "savedata")
                .add("collid", collectionItem.collectionId.toString())
    }
}