package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.*
import com.boardgamegeek.entities.GameDetailEntity
import com.boardgamegeek.entities.Person
import com.boardgamegeek.io.model.PersonResponseV1
import com.boardgamegeek.io.model.PersonItem
import com.boardgamegeek.provider.BggContract

fun PersonResponseV1.mapToModel(id: Int, timestamp: Long = System.currentTimeMillis()): Person? {
    return if (name.isNullOrBlank()) null
    else {
        val missingDescriptionMessage = "This page does not exist. You can edit this page to create it."
        Person(
            internalId = BggContract.INVALID_ID,
            id = id,
            name = name,
            description = if (description == missingDescriptionMessage) "" else description,
            updatedTimestamp = timestamp,
        )
    }
}

fun PersonItem.mapToModel(person: Person, timestamp: Long = System.currentTimeMillis()): Person? {
    return if (id != person.id.toString()) null
    else
        person.copy(
            imageUrl = image,
            thumbnailUrl = thumbnail,
            imagesUpdatedTimestamp = timestamp,
        )
}

fun ArtistLocal.mapToModel() = Person(
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

fun Person.mapToArtistBasic() = ArtistBasic(
    artistId = id,
    artistName = name,
    artistDescription = description,
    updatedTimestamp = updatedTimestamp,
)

fun Person.mapToArtistImages() = ArtistImages(
    artistId = id,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    updatedTimestamp = imagesUpdatedTimestamp,
)

fun DesignerLocal.mapToModel() = Person(
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

fun Person.mapToDesignerBasic() = DesignerBasic(
    designerId = id,
    designerName = name,
    designerDescription = description,
    updatedTimestamp = updatedTimestamp,
)

fun Person.mapToDesignerImages() = DesignerImages(
    designerId = id,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    updatedTimestamp = imagesUpdatedTimestamp,
)
