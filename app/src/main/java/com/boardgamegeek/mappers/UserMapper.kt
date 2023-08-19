package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.UserForUpsert
import com.boardgamegeek.db.model.UserLocal
import com.boardgamegeek.db.model.UserAsBuddyForUpsert
import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.io.model.Buddy
import com.boardgamegeek.io.model.User
import com.boardgamegeek.provider.BggContract

fun UserLocal.mapToEntity() = UserEntity(
    internalId = internalId,
    id = buddyId,
    userName = buddyName,
    firstName = buddyFirstName.orEmpty(),
    lastName = buddyLastName.orEmpty(),
    avatarUrl = if (avatarUrl == BggContract.INVALID_URL) "" else avatarUrl.orEmpty(), // TODO do we store "N/A" in the database?
    playNickname = playNickname.orEmpty(),
    updatedTimestamp = updatedTimestamp ?: 0L,
)

fun User.mapForUpsert(timestamp: Long) = UserForUpsert(
    buddyId = id.toIntOrNull() ?: BggContract.INVALID_ID,
    buddyName = name,
    buddyFirstName = firstName,
    buddyLastName = lastName,
    avatarUrl = if (avatarUrl == BggContract.INVALID_URL) "" else avatarUrl.orEmpty(),
    updatedTimestamp = timestamp,
)

fun Buddy.mapForBuddyUpsert(timestamp: Long) = UserAsBuddyForUpsert(
    buddyId = id.toIntOrNull() ?: BggContract.INVALID_ID,
    userName = name,
    updatedTimestamp = timestamp,
)
