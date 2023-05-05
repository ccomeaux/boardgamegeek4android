@file:Suppress("SpellCheckingInspection", "unused")

package com.boardgamegeek.io

import com.boardgamegeek.io.model.GeekListsResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface BggAjaxApi {
    @GET("/geeklist/module?ajax=1&domain=boardgame&nosession=1&tradelists=0&version=v5")
    suspend fun geekLists(@Query("sort") sort: GeekListSort?, @Query("showcount") pageSize: Int, @Query("pageid") page: Int): GeekListsResponse

    enum class GeekListSort {
        @SerializedName("hot")
        HOT,

        @SerializedName("recent")
        RECENT,

        @SerializedName("active")
        ACTIVE,
    }
}
