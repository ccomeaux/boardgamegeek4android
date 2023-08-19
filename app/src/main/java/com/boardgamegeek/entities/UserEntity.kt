package com.boardgamegeek.entities

data class UserEntity(
    val internalId: Int,
    val id: Int,
    val userName: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String,
    val playNickname: String = "",
    val updatedTimestamp: Long = 0L,
    val isBuddy: Boolean = false,
) {
    val fullName = "$firstName $lastName".trim()

    val description = if (userName.isBlank()) fullName else "$fullName ($userName)"
}
