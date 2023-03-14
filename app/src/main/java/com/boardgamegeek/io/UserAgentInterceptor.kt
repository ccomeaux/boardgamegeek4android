package com.boardgamegeek.io

import android.content.Context
import com.boardgamegeek.extensions.versionName
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class UserAgentInterceptor(private val context: Context? = null) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val request = originalRequest.newBuilder()
                .header("User-Agent", constructUserAgent())
                .build()
        return chain.proceed(request)
    }

    private fun constructUserAgent(): String {
        val userAgent = "BGG4Android"
        return if (context == null) {
            userAgent
        } else {
            "$userAgent/${context.versionName()}"
        }
    }
}
