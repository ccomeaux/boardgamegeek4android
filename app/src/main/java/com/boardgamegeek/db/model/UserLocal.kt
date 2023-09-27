package com.boardgamegeek.db.model

data class UserLocal(
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
    val playNickname: String?,
    val buddyFlag: Boolean?,
    val syncHashCode: Int?,
    val updatedListTimestamp: Long?,
    val updatedDetailTimestamp: Long?,
)
