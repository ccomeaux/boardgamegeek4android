package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "publishers",
    indices = [Index("publisher_id", name = "index_publishers_publisher_id", unique = true)]
)
data class PublisherEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
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
    @ColumnInfo(name = "publisher_hero_image_url")
    val publisherHeroImageUrl: String?,
    @ColumnInfo(name = "whitmore_score")
    val whitmoreScore: Int?,
    @ColumnInfo(name = "publisher_stats_updated_timestamp")
    val statsUpdatedTimestamp: Date?,
    @ColumnInfo(name = "updated")
    val updatedTimestamp: Date?,
)
