package com.boardgamegeek.service

import android.content.ContentValues
import okhttp3.FormBody
import okhttp3.OkHttpClient

abstract class CollectionTextUploadTask(client: OkHttpClient) : CollectionUploadTask(client) {
    protected var text: String? = null

    protected abstract val fieldName: String

    protected abstract fun getValue(): String

    override fun createForm(): FormBody {
        @Suppress("SpellCheckingInspection")
        return createFormBuilder()
                .add("fieldname", fieldName)
                .add("value", getValue())
                .build()
    }

    override fun saveContent(content: String) {
        text = content
    }

    override fun appendContentValues(contentValues: ContentValues) {
        // Don't save text. The response to the POST translates markdown into HTML
        contentValues.put(timestampColumn, 0)
    }
}