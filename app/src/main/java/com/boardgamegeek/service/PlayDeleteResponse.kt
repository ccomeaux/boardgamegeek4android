package com.boardgamegeek.service

import okhttp3.OkHttpClient
import okhttp3.Request

class PlayDeleteResponse(client: OkHttpClient, request: Request) : PlayPostResponse(client, request) {
    private var playDelete: PlayDelete? = null

    override fun saveContent(content: String?) {
        playDelete = gson.fromJson(content, PlayDelete::class.java)
    }

    val isSuccessful: Boolean
        get() = !(hasError()) && playDelete?.isSuccessful == true

    private class PlayDelete {
        val isSuccessful = false
    }
}