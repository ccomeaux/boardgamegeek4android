package com.boardgamegeek.db.model

data class UserForUpsert(
    val buddyId: Int, // unique
    val buddyName: String,
    val buddyFirstName: String?,
    val buddyLastName: String?,
    val avatarUrl: String?,
    val syncHashCode: Int? = null,
    val updatedTimestamp: Long?,
) {
    fun generateSyncHashCode(): Int {
        return ("${buddyFirstName}\n${buddyLastName}\n${avatarUrl}\n").hashCode()
    }
}
