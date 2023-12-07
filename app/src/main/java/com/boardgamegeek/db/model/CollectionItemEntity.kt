package com.boardgamegeek.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "collection",
    foreignKeys = [
        ForeignKey(entity = GameEntity::class, parentColumns = ["game_id"], childColumns = ["game_id"], onDelete = ForeignKey.CASCADE)
    ],
)
data class CollectionItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val internalId: Long,
    @ColumnInfo(name = "updated")
    val updatedTimestamp: Long?,
    @ColumnInfo(name = "updated_list")
    val updatedListTimestamp: Long?,
    @ColumnInfo(name = "game_id") // TODO add index
    val gameId: Int,
    @ColumnInfo(name = "collection_id")
    val collectionId: Int,
    @ColumnInfo(name = "collection_name")
    val collectionName: String,
    @ColumnInfo(name = "collection_sort_name")
    val collectionSortName: String,
    @ColumnInfo(name = "own")
    val statusOwn: Boolean,
    @ColumnInfo(name = "previously_owned")
    val statusPreviouslyOwned: Boolean,
    @ColumnInfo(name = "for_trade")
    val statusForTrade: Boolean,
    @ColumnInfo(name = "want")
    val statusWant: Boolean,
    @ColumnInfo(name = "want_to_play")
    val statusWantToPlay: Boolean,
    @ColumnInfo(name = "want_to_buy")
    val statusWantToBuy: Boolean,
    @ColumnInfo(name = "wishlist")
    val statusWishlist: Boolean,
    @ColumnInfo(name = "wishlist_priority")
    val statusWishlistPriority: Int?,
    @ColumnInfo(name = "preordered")
    val statusPreordered: Boolean,
    @ColumnInfo
    val comment: String?,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long?,
    @ColumnInfo(name = "price_paid_currency")
    val privateInfoPricePaidCurrency: String?,
    @ColumnInfo(name = "price_paid")
    val privateInfoPricePaid: Double?,
    @ColumnInfo(name = "current_value_currency")
    val privateInfoCurrentValueCurrency: String?,
    @ColumnInfo(name = "current_value")
    val privateInfoCurrentValue: Double?,
    @ColumnInfo(name = "quantity")
    val privateInfoQuantity: Int?,
    @ColumnInfo(name = "acquisition_date")
    val privateInfoAcquisitionDate: String?,
    @ColumnInfo(name = "acquired_from")
    val privateInfoAcquiredFrom: String?,
    @ColumnInfo(name = "private_comment")
    val privateInfoComment: String?,
    @ColumnInfo("conditiontext")
    val condition: String?,
    @ColumnInfo("wantpartslist")
    val wantpartsList: String?,
    @ColumnInfo("haspartslist")
    val haspartsList: String?,
    @ColumnInfo("wishlistcomment")
    val wishlistComment: String?,
    @ColumnInfo("collection_year_published")
    val collectionYearPublished: Int?,
    @ColumnInfo
    val rating: Double?,
    @ColumnInfo("collection_thumbnail_url")
    val collectionThumbnailUrl: String?,
    @ColumnInfo("collection_image_url")
    val collectionImageUrl: String?,
    @ColumnInfo("status_dirty_timestamp")
    val statusDirtyTimestamp: Long?,
    @ColumnInfo("rating_dirty_timestamp")
    val ratingDirtyTimestamp: Long?,
    @ColumnInfo("comment_dirty_timestamp")
    val commentDirtyTimestamp: Long?,
    @ColumnInfo("private_info_dirty_timestamp")
    val privateInfoDirtyTimestamp: Long?,
    @ColumnInfo("collection_dirty_timestamp")
    val collectionDirtyTimestamp: Long?,
    @ColumnInfo("collection_delete_timestamp")
    val collectionDeleteTimestamp: Long?,
    @ColumnInfo("wishlist_comment_dirty_timestamp")
    val wishlistCommentDirtyTimestamp: Long?,
    @ColumnInfo("trade_condition_dirty_timestamp")
    val tradeConditionDirtyTimestamp: Long?,
    @ColumnInfo("want_parts_dirty_timestamp")
    val wantPartsDirtyTimestamp: Long?,
    @ColumnInfo("has_parts_dirty_timestamp")
    val hasPartsDirtyTimestamp: Long?,
    @ColumnInfo("collection_hero_image_url")
    val collectionHeroImageUrl: String?,
    @ColumnInfo("inventory_location")
    val privateInfoInventoryLocation: String?,
)
