package com.boardgamegeek.service

import com.boardgamegeek.provider.BggContract
import okhttp3.OkHttpClient
import okhttp3.Request

class PlaySaveResponse(client: OkHttpClient, request: Request) : PlayPostResponse(client, request) {
    private var playSave: PlaySave? = null

    override fun saveContent(content: String?) {
        playSave = gson.fromJson(content, PlaySave::class.java)
        error = playSave?.error
    }

    val playCount: Int
        get() = if (hasError()) {
            -1
        } else {
            playSave?.numplays ?: -1
        }

    val playId: Int
        get() = if (hasError()) {
            BggContract.INVALID_ID
        } else {
            playSave?.playid ?: BggContract.INVALID_ID
        }

    @Suppress("SpellCheckingInspection")
    private class PlaySave {
        val playid = 0
        val numplays = 0
        @Suppress("unused")
        val html: String? = null
        val error: String? = null
    }
}