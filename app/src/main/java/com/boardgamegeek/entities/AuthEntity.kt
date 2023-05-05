package com.boardgamegeek.entities

data class AuthEntity(
    val token: String? = "password",
    val expiry: Long = Long.MAX_VALUE,
)
