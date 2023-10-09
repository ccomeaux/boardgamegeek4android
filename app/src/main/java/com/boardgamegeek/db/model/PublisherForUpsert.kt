package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import java.util.Date

data class PublisherForUpsert(
    @ColumnInfo(name = "_id")
    val internalId: Int,
    @ColumnInfo(name = "publisher_id")
    val publisherId: Int,
    @ColumnInfo(name = "publisher_name")
    val publisherName: String,
    @ColumnInfo(name = "publisher_sort_name")
    val publisherSortName: String?,
    @ColumnInfo(name = "publisher_description")
    val publisherDescription: String?,
    @ColumnInfo(name = "publisher_image_url")
    val publisherImageUrl: String?,
    @ColumnInfo(name = "publisher_thumbnail_url")
    val publisherThumbnailUrl: String?,
    @ColumnInfo(name = "updated")
    val updatedTimestamp: Date?,
)
