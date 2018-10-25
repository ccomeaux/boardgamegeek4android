package com.boardgamegeek.entities

data class UserEntity(
        val internalId: Long,
        val id: Int,
        val userName: String,
        val firstName: String,
        val lastName: String,
        val avatarUrl: String,
        val playNickname: String,
        val updatedTimestamp: Long
) {
    val fullName = "$firstName $lastName".trim()
}