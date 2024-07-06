package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class CollectionItemForUpdate(
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "updated")
    val updatedTimestamp: Long?,
    @ColumnInfo(name = "updated_list")
    val updatedListTimestamp: Long?,
    @ColumnInfo(name = "game_id")
    val gameId: Int,
    @ColumnInfo(name = "collection_id")
    val collectionId: Int,
    @ColumnInfo(name = "collection_name")
    val collectionName: String,
    @ColumnInfo(name = "collection_sort_name")
    val collectionSortName: String,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long?,
    @ColumnInfo("collection_year_published")
    val collectionYearPublished: Int?,
    @ColumnInfo("collection_thumbnail_url")
    val collectionThumbnailUrl: String?,
    @ColumnInfo("collection_image_url")
    val collectionImageUrl: String?,
)
