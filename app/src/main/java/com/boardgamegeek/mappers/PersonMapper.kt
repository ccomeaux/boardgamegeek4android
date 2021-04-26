package com.boardgamegeek.mappers

import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.io.model.Person

fun Person.mapToEntity(id: Int): PersonEntity {
    return PersonEntity(
            id = id,
            name = this.name,
            description = this.description,
            updatedTimestamp = 0L,
    )
}
