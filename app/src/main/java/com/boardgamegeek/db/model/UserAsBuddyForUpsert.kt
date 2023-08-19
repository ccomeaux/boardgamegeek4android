package com.boardgamegeek.db.model

data class UserAsBuddyForUpsert(
    val buddyId: Int,
    val userName: String,
    val updatedTimestamp: Long,
)
