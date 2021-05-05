package com.boardgamegeek.mappers

import com.boardgamegeek.entities.UserEntity
import com.boardgamegeek.io.model.User
import com.boardgamegeek.provider.BggContract

fun User.mapToEntity(): UserEntity = UserEntity(
        internalId = BggContract.INVALID_ID.toLong(),
        id = id,
        userName = name,
        firstName = firstName,
        lastName = lastName,
        avatarUrlRaw = avatarUrl,
)
