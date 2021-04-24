package com.boardgamegeek.mappers

import com.boardgamegeek.entities.CompanyEntity
import com.boardgamegeek.io.model.CompanyItem
import com.boardgamegeek.provider.BggContract

fun CompanyItem.mapToEntity(): CompanyEntity {
    return CompanyEntity(
            id = this.id.toIntOrNull() ?: BggContract.INVALID_ID,
            name = this.name,
            description = this.description,
            imageUrl = this.image,
            thumbnailUrl = this.thumbnail,
    )
}
