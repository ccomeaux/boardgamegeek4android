package com.boardgamegeek.model

data class AuthToken(
    val username: String? = null,
    val token: String? = "password",
    val expiry: Long = Long.MAX_VALUE,
)
