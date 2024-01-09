package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.UserAsBuddyForUpsert
import com.boardgamegeek.db.model.UserEntity
import com.boardgamegeek.db.model.UserForUpsert
import com.boardgamegeek.io.model.Buddy
import com.boardgamegeek.io.model.UserRemote
import com.boardgamegeek.model.User
import com.boardgamegeek.provider.BggContract
import java.util.Date

fun UserEntity.mapToModel() = User(
    username = username,
    firstName = firstName.orEmpty(),
    lastName = lastName.orEmpty(),
    avatarUrl = if (avatarUrl == BggContract.INVALID_URL) "" else avatarUrl.orEmpty(),
    playNickname = playNickname.orEmpty(),
    updatedTimestamp = updatedDetailDate?.time ?: 0L,
    isBuddy = buddyFlag == true,
)

fun UserRemote.mapForUpsert(timestamp: Long) = UserForUpsert(
    username = name,
    firstName = firstName,
    lastName = lastName,
    avatarUrl = if (avatarUrl == BggContract.INVALID_URL) "" else avatarUrl,
    syncHashCode = ("${name}\n${lastName}\n${avatarUrl}\n").hashCode(),
    updatedDetailTimestamp = Date(timestamp),
)

fun Buddy.mapForBuddyUpsert(timestamp: Long) = UserAsBuddyForUpsert(
    username = name,
    buddyFlag = true,
    updatedListDate = Date(timestamp),
)
