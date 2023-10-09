package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artists",
    indices = [Index("artist_id", name = "index_artists_artist_id", unique = true)]
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Int,
    @ColumnInfo(name = "artist_id")
    val artistId: Int, // unique
    @ColumnInfo(name = "artist_name")
    val artistName: String,
    @ColumnInfo(name = "artist_description")
    val artistDescription: String?,
    @ColumnInfo(name = "artist_image_url")
    val artistImageUrl: String?,
    @ColumnInfo(name = "artist_thumbnail_url")
    val artistThumbnailUrl: String?,
    @ColumnInfo(name = "artist_hero_image_url")
    val artistHeroImageUrl: String?,
    @ColumnInfo(name = "artist_images_updated_timestamp")
    val imagesUpdatedTimestamp: Long?,
    @ColumnInfo(name = "whitmore_score")
    val whitmoreScore: Int?,
    @ColumnInfo(name = "artist_stats_updated_timestamp")
    val statsUpdatedTimestamp: Long?,
    @ColumnInfo(name = "updated")
    val updatedTimestamp: Long?,
)