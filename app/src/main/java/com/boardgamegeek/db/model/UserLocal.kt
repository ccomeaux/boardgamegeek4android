package com.boardgamegeek.db.model

data class UserLocal(
    val internalId: Int,
    val buddyId: Int, // unique
    val buddyName: String,
    val buddyFirstName: String?,
    val buddyLastName: String?,
    val avatarUrl: String?,
    val playNickname: String?,
    val buddyFlag: Boolean?,
    val syncHashCode: Int? = null,
    val updatedListTimestamp: Long,
    val updatedTimestamp: Long?,
)
