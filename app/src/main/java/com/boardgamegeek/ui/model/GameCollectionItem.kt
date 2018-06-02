package com.boardgamegeek.ui.model

data class GameCollectionItem(
        val internalId: Long,
        val collectionId: Int,
        val collectionName: String,
        val gameName: String,
        val collectionYearPublished: Int,
        val yearPublished: Int,
        val imageUrl: String,
        val thumbnailUrl: String,
        val comment: String,
        val numberOfPlays: Int,
        val rating: Double,
        val syncTimestamp: Long,
        val deleteTimestamp: Long,
        val own: Boolean = false,
        val previouslyOwned: Boolean = false,
        val forTrade: Boolean = false,
        val wantInTrade: Boolean = false,
        val wantToPlay: Boolean = false,
        val wantToBuy: Boolean = false,
        val preOrdered: Boolean = false,
        val wishList: Boolean = false,
        val wishListPriority: Int = 3,
        val dirtyTimestamp: Long = 0L,
        val statusDirtyTimestamp: Long = 0L,
        val ratingDirtyTimestamp: Long = 0L,
        val commentDirtyTimestamp: Long = 0L,
        val privateInfoDirtyTimestamp: Long = 0L,
        val wishListDirtyTimestamp: Long = 0L,
        val tradeConditionDirtyTimestamp: Long = 0L,
        val hasPartsDirtyTimestamp: Long = 0L,
        val wantPartsDirtyTimestamp: Long = 0L
) {
    val isDirty: Boolean by lazy {
        when {
            deleteTimestamp > 0L -> true
            dirtyTimestamp > 0L -> true
            statusDirtyTimestamp > 0L -> true
            ratingDirtyTimestamp > 0L -> true
            commentDirtyTimestamp > 0L -> true
            privateInfoDirtyTimestamp > 0L -> true
            wishListDirtyTimestamp > 0L -> true
            tradeConditionDirtyTimestamp > 0L -> true
            hasPartsDirtyTimestamp > 0L -> true
            wantPartsDirtyTimestamp > 0L -> true
            else -> false
        }
    }
}
