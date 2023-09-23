package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.UserForUpsert
import com.boardgamegeek.db.model.UserLocal
import com.boardgamegeek.db.model.UserAsBuddyForUpsert
import com.boardgamegeek.entities.User
import com.boardgamegeek.io.model.Buddy
import com.boardgamegeek.io.model.UserRemote
import com.boardgamegeek.provider.BggContract

fun UserLocal.mapToModel() = User(
    internalId = internalId,
    id = buddyId,
    userName = buddyName,
    firstName = buddyFirstName.orEmpty(),
    lastName = buddyLastName.orEmpty(),
    avatarUrl = if (avatarUrl == BggContract.INVALID_URL) "" else avatarUrl.orEmpty(),
    playNickname = playNickname.orEmpty(),
    updatedTimestamp = updatedTimestamp ?: 0L,
)

fun UserRemote.mapForUpsert(timestamp: Long) = UserForUpsert(
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
