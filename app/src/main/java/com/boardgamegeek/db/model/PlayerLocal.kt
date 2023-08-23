package com.boardgamegeek.db.model

data class PlayerLocal(
    val name: String,
    val username: String,
    val playCount: Int?,
    val winCount: Int?,
    val avatar: String?,
)
