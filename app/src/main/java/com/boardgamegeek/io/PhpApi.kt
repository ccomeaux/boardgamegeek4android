@file:Suppress("SpellCheckingInspection")

package com.boardgamegeek.io

import com.boardgamegeek.io.model.PlayPostResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface PhpApi {
    @POST("geekplay.php")
    suspend fun play(@Body play: RequestBody): PlayPostResponse
}
