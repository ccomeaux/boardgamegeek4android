@file:Suppress("LeakingThis")

package com.boardgamegeek.service

import android.text.Html
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// TODO replace this whole thing with coroutines
abstract class PlayPostResponse(client: OkHttpClient, request: Request) {
    protected val gson = Gson()
    protected var error: String? = null
    protected var exception: Exception? = null
    protected abstract fun saveContent(content: String?)

    init {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val content = response.body?.string()?.trim() ?: ""
                if (content.startsWith(ERROR_DIV)) {
                    @Suppress("DEPRECATION")
                    error = Html.fromHtml(content).toString().trim()
                } else {
                    saveContent(content)
                }
            } else {
                error = "Unsuccessful post: " + response.code
            }
        } catch (e: IOException) {
            exception = e
        } catch (e: IllegalStateException) {
            exception = e
        }
    }

    fun hasError() = errorMessage.isNotBlank()

    /**
     * Indicates the user attempted to modify a play without being authenticated.
     */
    fun hasAuthError(): Boolean {
        return "You must login to save plays".equals(error, ignoreCase = true) ||
                "You can't delete this play".equals(error, ignoreCase = true) ||
                "You are not permitted to edit this play.".equals(error, ignoreCase = true)
    }

    /**
     * Indicates the user attempted to modify a play that doesn't exist.
     */
    fun hasInvalidIdError(): Boolean {
        return "Play does not exist.".equals(error, ignoreCase = true) || "Invalid item. Play not saved." == error
    }

    val errorMessage: String
        get() {
            return if (exception != null) {
                if (error != null) {
                    "$error\n${exception.toString()}"
                } else exception.toString()
            } else error.orEmpty()
        }

    companion object {
        protected const val ERROR_DIV = "<div class='messagebox error'>"
    }
}
