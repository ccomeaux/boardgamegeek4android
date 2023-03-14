package com.boardgamegeek.util

import android.content.Context
import android.net.Uri
import com.boardgamegeek.BuildConfig
import com.boardgamegeek.io.AuthInterceptor
import com.boardgamegeek.io.RetryInterceptor
import com.boardgamegeek.io.UserAgentInterceptor
import com.facebook.stetho.okhttp3.StethoInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object HttpUtils {
    private const val HTTP_REQUEST_TIMEOUT_SEC = 15

    /**
     * Encodes `s` using UTF-8 using the format required by `application/x-www-form-urlencoded` MIME content type.
     */
    fun String?.encodeForUrl(): String? = Uri.encode(this, "UTF-8")

    fun getHttpClient(retry202Response: Boolean) = createBuilder()
        .addInterceptor(UserAgentInterceptor(null))
        .addInterceptor(RetryInterceptor(retry202Response))
        .addLoggingInterceptor()
        .build()

    fun getHttpClientWithAuth(context: Context?) = createBuilder()
        .addInterceptor(UserAgentInterceptor(context))
        .addInterceptor(AuthInterceptor(context))
        .addInterceptor(RetryInterceptor())
        .addLoggingInterceptor()
        .build()

    private fun createBuilder() = OkHttpClient.Builder()
        .connectTimeout(HTTP_REQUEST_TIMEOUT_SEC.toLong(), TimeUnit.SECONDS)
        .readTimeout(HTTP_REQUEST_TIMEOUT_SEC.toLong(), TimeUnit.SECONDS)
        .writeTimeout(HTTP_REQUEST_TIMEOUT_SEC.toLong(), TimeUnit.SECONDS)

    private fun OkHttpClient.Builder.addLoggingInterceptor() = apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            addNetworkInterceptor(StethoInterceptor())
        }
    }
}
