package com.boardgamegeek.db.model

data class UserAsBuddyForUpsert(
    val username: String,
    val updatedTimestamp: Long,
)
