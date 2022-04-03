package com.boardgamegeek.mappers

import com.boardgamegeek.entities.BriefBuddyEntity
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.io.model.Buddy
import com.boardgamegeek.io.model.User
import com.boardgamegeek.provider.BggContract

fun User.mapToEntity() = UserEntity(
    internalId = BggContract.INVALID_ID.toLong(),
    id = id.toIntOrNull() ?: BggContract.INVALID_ID,
    userName = name,
    firstName = firstName,
    lastName = lastName,
    avatarUrlRaw = avatarUrl,
)

fun Buddy.mapToEntity(timestamp: Long) = BriefBuddyEntity(
    id = id.toIntOrNull() ?: BggContract.INVALID_ID,
    userName = name,
    updatedTimestamp = timestamp,
)
