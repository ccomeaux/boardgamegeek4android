package com.boardgamegeek.service

import okhttp3.FormBody
import okhttp3.OkHttpClient

abstract class CollectionUploadTask(client: OkHttpClient) : CollectionTask(client) {
    abstract val timestampColumn: String

    abstract val isDirty: Boolean

    @Suppress("SpellCheckingInspection")
    override fun createFormBuilder(): FormBody.Builder {
        return super.createFormBuilder()
                .add("action", "savedata")
                .add("collid", collectionItem.collectionId.toString())
    }
}