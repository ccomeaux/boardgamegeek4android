package com.boardgamegeek.mappers

import com.boardgamegeek.db.model.PublisherEntity
import com.boardgamegeek.db.model.PublisherForUpsert
import com.boardgamegeek.db.model.PublisherWithItemCount
import com.boardgamegeek.model.Company
import com.boardgamegeek.model.GameDetail
import com.boardgamegeek.extensions.sortName
import com.boardgamegeek.io.model.CompanyItem
import com.boardgamegeek.provider.BggContract
import java.util.Date

fun CompanyItem.mapToModel(timestamp: Date) = Company(
    id = this.id.toIntOrNull() ?: BggContract.INVALID_ID,
    name = name,
    sortName = if (nameType == "primary") name.sortName(sortindex) else name,
    description = description.orEmpty(),
    imageUrl = image,
    thumbnailUrl = thumbnail,
    updatedTimestamp = timestamp,
)

fun PublisherEntity.mapToModel() = Company(
    id = publisherId,
    name = publisherName,
    sortName = "",
    description = publisherDescription.orEmpty(),
    imageUrl = publisherImageUrl.orEmpty(),
    thumbnailUrl = publisherThumbnailUrl.orEmpty(),
    heroImageUrl = "",
    updatedTimestamp = updatedTimestamp,
    whitmoreScore = whitmoreScore ?: 0,
    statsUpdatedTimestamp = statsUpdatedTimestamp,
)

fun PublisherWithItemCount.mapToModel() = publisher.mapToModel().copy(itemCount = itemCount)

fun PublisherEntity.mapToGameDetail() = GameDetail(
    id = publisherId,
    name = publisherName,
    thumbnailUrl = publisherThumbnailUrl.orEmpty(),
)

fun Company.mapForUpsert(internalId: Long = 0) = PublisherForUpsert(
    internalId = internalId,
    publisherId = id,
    publisherName = name,
    publisherDescription = description,
    publisherSortName = sortName,
    publisherImageUrl = imageUrl,
    publisherThumbnailUrl = thumbnailUrl,
    updatedTimestamp = updatedTimestamp,
)
