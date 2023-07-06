@file:Suppress("SpellCheckingInspection")

package com.boardgamegeek.io

import com.boardgamegeek.io.model.CollectionPostResponse
import com.boardgamegeek.io.model.PlayPostResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface PhpApi {
    @POST("geekcollection.php")
    suspend fun collection(@Body play: RequestBody): CollectionPostResponse

    @POST("geekplay.php")
    suspend fun play(@Body play: RequestBody): PlayPostResponse
}
