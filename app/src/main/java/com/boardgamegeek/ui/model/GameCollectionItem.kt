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
        val wishListPriority: Int = 3
)