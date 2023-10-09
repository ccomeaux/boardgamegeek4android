package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "designers")
data class DesignerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Int,
    @ColumnInfo(name = "designer_id")
    val designerId: Int, // unique
    @ColumnInfo(name = "designer_name")
    val designerName: String,
    @ColumnInfo(name = "designer_description")
    val designerDescription: String?,
    @ColumnInfo(name = "designer_image_url")
    val designerImageUrl: String?,
    @ColumnInfo(name = "designer_thumbnail_url")
    val designerThumbnailUrl: String?,
    @ColumnInfo(name = "designer_hero_image_url")
    val designerHeroImageUrl: String?,
    @ColumnInfo(name = "designer_images_updated_timestamp")
    val imagesUpdatedTimestamp: Date?,
    @ColumnInfo(name = "whitmore_score")
    val whitmoreScore: Int?,
    @ColumnInfo(name = "designer_stats_updated_timestamp")
    val statsUpdatedTimestamp: Date?,
    @ColumnInfo(name = "updated")
    val updatedTimestamp: Date?,
)