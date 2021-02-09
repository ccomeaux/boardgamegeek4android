package com.boardgamegeek.service

import android.content.ContentValues
import android.text.Html
import com.boardgamegeek.service.model.CollectionItem
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

abstract class CollectionTask(protected val client: OkHttpClient, var collectionItem: CollectionItem) {
    protected var error: String? = null
    protected var exception: Exception? = null

    fun post() {
        val request: Request = Request.Builder()
                .url(GEEK_COLLECTION_URL)
                .post(createForm())
                .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val content = response.body?.string().orEmpty()
                if (content.startsWith(ERROR_DIV)) {
                    @Suppress("DEPRECATION")
                    error = Html.fromHtml(content).toString().trim()
                }
                saveContent(content)
            } else {
                error = "Unsuccessful post: " + response.code
            }
        } catch (e: IOException) {
            exception = e
        }
    }

    protected abstract fun createForm(): FormBody

    protected open fun createFormBuilder(): FormBody.Builder {
        @Suppress("SpellCheckingInspection")
        return FormBody.Builder()
                .add("ajax", "1")
                .add("objecttype", "thing")
                .add("objectid", collectionItem.gameId.toString())
    }

    protected open fun saveContent(content: String) {}

    abstract fun appendContentValues(contentValues: ContentValues)

    fun hasAuthError(): Boolean {
        return error?.contains(AUTH_ERROR_TEXT) ?: false
    }

    val errorMessage = exception?.message?.ifEmpty { exception.toString() } ?: error

    companion object {
        private const val GEEK_COLLECTION_URL = "https://www.boardgamegeek.com/geekcollection.php"

        @Suppress("SpellCheckingInspection")
        private const val ERROR_DIV = "<div class='messagebox error'>"
        private const val AUTH_ERROR_TEXT = "login"
    }
}