package com.boardgamegeek.io

import com.boardgamegeek.io.model.Image
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GeekdoApi {
    @GET("/api/images/{id}")
    fun image(@Path("id") imageId: Int): Call<Image>

    @GET("/api/images/{id}")
    suspend fun image2(@Path("id") imageId: Int): Image
}
