package com.boardgamegeek.db.model

data class UserForUpsert(
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
    val syncHashCode: Int? = null,
    val updatedDetailTimestamp: Long?,
) {
    fun generateSyncHashCode(): Int {
        return ("${username}\n${lastName}\n${avatarUrl}\n").hashCode()
    }
}
