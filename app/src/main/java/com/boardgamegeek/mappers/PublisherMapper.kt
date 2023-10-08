package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.PublisherBasic
import com.boardgamegeek.db.model.PublisherBrief
import com.boardgamegeek.db.model.PublisherLocal
import com.boardgamegeek.model.Company
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.extensions.sortName
import com.boardgamegeek.io.model.CompanyItem
import com.boardgamegeek.provider.BggContract

fun CompanyItem.mapToModel(timestamp: Long) = Company(
    id = this.id.toIntOrNull() ?: BggContract.INVALID_ID,
    name = name,
    sortName = if (nameType == "primary") name.sortName(sortindex) else name,
    description = description.orEmpty(),
    imageUrl = image,
    thumbnailUrl = thumbnail,
    updatedTimestamp = timestamp,
)

fun PublisherLocal.mapToModel() = Company(
    id = publisherId,
    name = publisherName,
    sortName = "",
    description = publisherDescription.orEmpty(),
    imageUrl = publisherImageUrl.orEmpty(),
    thumbnailUrl = publisherThumbnailUrl.orEmpty(),
    heroImageUrl = "",
    updatedTimestamp = updatedTimestamp,
    whitmoreScore = whitmoreScore ?: 0,
    statsUpdatedTimestamp = statsUpdatedTimestamp ?: 0L,
    itemCount = itemCount ?: 0,
)

fun PublisherBrief.mapToGameDetail() = GameDetail(
    id = publisherId,
    name = publisherName,
    thumbnailUrl = publisherThumbnailUrl.orEmpty(),
)

fun Company.mapToPublisherBasic() = PublisherBasic(
    publisherId = id,
    publisherName = name,
    publisherDescription = description,
    sortName = sortName,
    publisherImageUrl = imageUrl,
    publisherThumbnailUrl = thumbnailUrl,
)
