package com.boardgamegeek.entities

data class BriefBuddyEntity(
    val id: Int,
    val userName: String,
    val updatedTimestamp: Long = 0L,
)
