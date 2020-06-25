package com.boardgamegeek.entities

import com.boardgamegeek.provider.BggContract.INVALID_URL

data class UserEntity(
        val internalId: Long,
        val id: Int,
        val userName: String,
        val firstName: String,
        val lastName: String,
        val avatarUrlRaw: String,
        val playNickname: String,
        val updatedTimestamp: Long
) {
    val fullName = "$firstName $lastName".trim()

    val description = if (userName.isBlank()) fullName else "$fullName ($userName)"

    val avatarUrl: String = avatarUrlRaw
        get() = if (field == INVALID_URL) "" else field
}