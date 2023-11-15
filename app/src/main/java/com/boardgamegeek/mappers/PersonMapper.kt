package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.*
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.model.Person
import com.boardgamegeek.io.model.PersonResponseV1
import com.boardgamegeek.io.model.PersonItem
import com.boardgamegeek.provider.BggContract
import java.util.Date

fun PersonResponseV1.mapToModel(id: Int, timestamp: Date): Person? {
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

fun PersonItem.mapToModel(person: Person, timestamp: Date): Person? {
    return if (id != person.id.toString()) null
    else
        person.copy(
            imageUrl = image,
            thumbnailUrl = thumbnail,
            imagesUpdatedTimestamp = timestamp,
        )
}

fun ArtistEntity.mapToModel() = Person(
    internalId = internalId,
    id = artistId,
    name = artistName,
    description = artistDescription.orEmpty(),
    imageUrl = artistImageUrl.orEmpty(),
    thumbnailUrl = artistThumbnailUrl.orEmpty(),
    heroImageUrl = artistHeroImageUrl.orEmpty(),
    updatedTimestamp = updatedTimestamp,
    imagesUpdatedTimestamp = imagesUpdatedTimestamp,
    whitmoreScore = whitmoreScore ?: 0,
    statsUpdatedTimestamp = statsUpdatedTimestamp,
)

fun DesignerEntity.mapToGameDetail() = GameDetail(
    id = designerId,
    name = designerName,
    thumbnailUrl = designerThumbnailUrl.orEmpty(),
)

fun ArtistEntity.mapToGameDetail() = GameDetail(
    id = artistId,
    name = artistName,
    thumbnailUrl = artistThumbnailUrl.orEmpty(),
)

fun Person.mapArtistForUpsert(internalId: Int = BggContract.INVALID_ID) = ArtistForUpsert(
    internalId = internalId,
    artistId = id,
    artistName = name,
    artistDescription = description,
    updatedTimestamp = updatedTimestamp,
)

fun DesignerEntity.mapToModel() = Person(
    internalId = internalId,
    id = designerId,
    name = designerName,
    description = designerDescription.orEmpty(),
    imageUrl = designerImageUrl.orEmpty(),
    thumbnailUrl = designerThumbnailUrl.orEmpty(),
    heroImageUrl = designerHeroImageUrl.orEmpty(),
    updatedTimestamp = updatedTimestamp,
    imagesUpdatedTimestamp = imagesUpdatedTimestamp,
    whitmoreScore = whitmoreScore ?: 0,
    statsUpdatedTimestamp = statsUpdatedTimestamp,
)

fun Person.mapDesignerForUpsert(internalId: Int = BggContract.INVALID_ID) = DesignerForUpsert(
    internalId = internalId,
    designerId = id,
    designerName = name,
    designerDescription = description,
    updatedTimestamp = updatedTimestamp,
)
