@file:Suppress("DEPRECATION")

package com.boardgamegeek.io

import com.boardgamegeek.util.HttpUtils.getHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Adapter {
    @Suppress("SpellCheckingInspection")
    fun createGeekdoApi(): GeekdoApi {
        return Retrofit.Builder()
            .client(getHttpClient(true))
            .baseUrl("https://api.geekdo.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeekdoApi::class.java)
    }
}
