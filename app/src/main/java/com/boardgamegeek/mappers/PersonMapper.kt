package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.*
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.entities.PersonEntity
import com.boardgamegeek.io.model.Person
import com.boardgamegeek.io.model.PersonItem
import com.boardgamegeek.provider.BggContract

fun Person.mapToEntity(id: Int, timestamp: Long = System.currentTimeMillis()): PersonEntity? {
    return if (name.isNullOrBlank()) null
    else {
        val missingDescriptionMessage = "This page does not exist. You can edit this page to create it."
        PersonEntity(
            internalId = BggContract.INVALID_ID,
            id = id,
            name = name,
            description = if (description == missingDescriptionMessage) "" else description,
            updatedTimestamp = timestamp,
        )
    }
}

fun PersonItem.mapToEntity(entity: PersonEntity, timestamp: Long = System.currentTimeMillis()): PersonEntity? {
    return if (id != entity.id.toString()) null
    else
        entity.copy(
            imageUrl = image,
            thumbnailUrl = thumbnail,
            imagesUpdatedTimestamp = timestamp,
        )
}

fun ArtistLocal.mapToArtistEntity() = PersonEntity(
    internalId = internalId,
    id = artistId,
    name = artistName,
    description = artistDescription.orEmpty(),
    imageUrl = artistImageUrl.orEmpty(),
    thumbnailUrl = artistThumbnailUrl.orEmpty(),
    heroImageUrl = artistHeroImageUrl.orEmpty(),
    updatedTimestamp = updatedTimestamp ?: 0L,
    imagesUpdatedTimestamp = imagesUpdatedTimestamp ?: 0L,
    whitmoreScore = whitmoreScore ?: 0,
    statsUpdatedTimestamp = statsUpdatedTimestamp ?: 0L,
    itemCount = itemCount ?: 0,
)

fun DesignerBrief.mapToGameDetail() = GameDetailEntity(
    id = designerId,
    name = designerName,
    thumbnailUrl = designerThumbnailUrl.orEmpty(),
)

fun ArtistBrief.mapToGameDetail() = GameDetailEntity(
    id = artistId,
    name = artistName,
    thumbnailUrl = artistThumbnailUrl.orEmpty(),
)

fun PersonEntity.mapToArtistBasic() = ArtistBasic(
    artistId = id,
    artistName = name,
    artistDescription = description,
    updatedTimestamp = updatedTimestamp,
)

fun PersonEntity.mapToArtistImages() = ArtistImages(
    artistId = id,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    updatedTimestamp = imagesUpdatedTimestamp,
)

fun DesignerLocal.mapToDesignerEntity() = PersonEntity(
    internalId = internalId,
    id = designerId,
    name = designerName,
    description = designerDescription.orEmpty(),
    imageUrl = designerImageUrl.orEmpty(),
    thumbnailUrl = designerThumbnailUrl.orEmpty(),
    heroImageUrl = designerHeroImageUrl.orEmpty(),
    updatedTimestamp = updatedTimestamp,
    imagesUpdatedTimestamp = imagesUpdatedTimestamp ?: 0L,
    whitmoreScore = whitmoreScore ?: 0,
    statsUpdatedTimestamp = statsUpdatedTimestamp ?: 0L,
    itemCount = itemCount ?: 0,
)

fun PersonEntity.mapToDesignerBasic() = DesignerBasic(
    designerId = id,
    designerName = name,
    designerDescription = description,
    updatedTimestamp = updatedTimestamp,
)

fun PersonEntity.mapToDesignerImages() = DesignerImages(
    designerId = id,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    updatedTimestamp = imagesUpdatedTimestamp,
)
