package com.boardgamegeek.db.model

import androidx.room.ColumnInfo

data class CollectionStatusEntity(
    @ColumnInfo(name = "_id")
    val internalId: Long,
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
    @ColumnInfo("status_dirty_timestamp")
    val statusDirtyTimestamp: Long?,
)
