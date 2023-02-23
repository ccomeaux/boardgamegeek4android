@file:Suppress("DEPRECATION")

package com.boardgamegeek.io

import android.content.Context
import com.boardgamegeek.util.HttpUtils.getHttpClient
import com.boardgamegeek.util.HttpUtils.getHttpClientWithAuth
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

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

    fun createForXmlWithAuth(context: Context?): BggService = Retrofit.Builder()
        .baseUrl("https://boardgamegeek.com/")
        .addConverterFactory(EnumConverterFactory())
        .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
        .client(
            if (context == null) {
                getHttpClient(true)
            } else {
                getHttpClientWithAuth(context)
            }
        )
        .build()
        .create(BggService::class.java)
}
