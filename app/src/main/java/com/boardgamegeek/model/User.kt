package com.boardgamegeek.model

data class User(
    val username: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String,
    val playNickname: String = "",
    val updatedTimestamp: Long = 0L,
    val isBuddy: Boolean = false,
) {
    val fullName = "$firstName $lastName".trim()

    val description = if (username.isBlank()) fullName else "$fullName ($username)"
}
