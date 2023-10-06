package com.boardgamegeek.entities

data class AuthToken(
    val token: String? = "password",
    val expiry: Long = Long.MAX_VALUE,
)
