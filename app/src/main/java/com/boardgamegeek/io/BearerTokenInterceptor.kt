package com.boardgamegeek.io

import com.boardgamegeek.util.RemoteConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class BearerTokenInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = RemoteConfig.getString(RemoteConfig.KEY_BGG_BEARER_TOKEN)
        val originalRequest = chain.request()
        val request = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}