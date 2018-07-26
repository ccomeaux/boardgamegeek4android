package com.boardgamegeek.entities

import com.boardgamegeek.provider.BggContract

data class CollectionItemEntity(
        val internalId: Long = BggContract.INVALID_ID.toLong(),
        val gameId: Int = BggContract.INVALID_ID,
        val gameName: String = "",
        val collectionId: Int = BggContract.INVALID_ID,
        val collectionName: String = "",
        val sortName: String = "",
        val yearPublished: Int = YEAR_UNKNOWN,
        val imageUrl: String = "",
        val thumbnailUrl: String = "",
        val rating: Double = 0.0,
        val own: Boolean = false,
        val previouslyOwned: Boolean = false,
        val forTrade: Boolean = false,
        val wantInTrade: Boolean = false,
        val wantToPlay: Boolean = false,
        val wantToBuy: Boolean = false,
        val wishList: Boolean = false,
        val wishListPriority: Int = 3, // like to have. should this be 0?
        val preOrdered: Boolean = false,
        val lastModifiedDate: Long = 0L,
        val numberOfPlays: Int = 0,
        val pricePaidCurrency: String = "",
        val pricePaid: Double = 0.0,
        val currentValueCurrency: String = "",
        val currentValue: Double = 0.0,
        val quantity: Int = 1,
        val acquisitionDate: String = "", // Long?
        val acquiredFrom: String = "",
        val privateComment: String = "",
        val comment: String = "",
        val conditionText: String = "",
        val wantPartsList: String = "",
        val hasPartsList: String = "",
        val wishListComment: String = "",
        val syncTimestamp: Long = 0L,
        val deleteTimestamp: Long = 0L,
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
