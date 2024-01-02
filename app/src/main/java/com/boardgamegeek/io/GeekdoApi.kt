package com.boardgamegeek.io

import com.boardgamegeek.io.model.Image
import retrofit2.http.GET
import retrofit2.http.Path

@Suppress("SpellCheckingInspection")
interface GeekdoApi {
    @GET("/api/images/{id}")
    suspend fun image(@Path("id") imageId: Int): Image
}
