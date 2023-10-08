package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.UserForUpsert
import com.boardgamegeek.db.model.UserLocal
import com.boardgamegeek.db.model.UserAsBuddyForUpsert
import com.boardgamegeek.model.User
import com.boardgamegeek.io.model.Buddy
import com.boardgamegeek.io.model.UserRemote
import com.boardgamegeek.provider.BggContract

fun UserLocal.mapToModel() = User(
    username = username,
    firstName = firstName.orEmpty(),
    lastName = lastName.orEmpty(),
    avatarUrl = if (avatarUrl == BggContract.INVALID_URL) "" else avatarUrl.orEmpty(),
    playNickname = playNickname.orEmpty(),
    updatedTimestamp = updatedDetailTimestamp ?: 0L,
)

fun UserRemote.mapForUpsert(timestamp: Long) = UserForUpsert(
    username = name,
    firstName = firstName,
    lastName = lastName,
    avatarUrl = if (avatarUrl == BggContract.INVALID_URL) "" else avatarUrl.orEmpty(),
    updatedDetailTimestamp = timestamp,
)

fun Buddy.mapForBuddyUpsert(timestamp: Long) = UserAsBuddyForUpsert(
    username = name,
    updatedTimestamp = timestamp,
)
