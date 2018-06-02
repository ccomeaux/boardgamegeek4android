package com.boardgamegeek.entities

import com.boardgamegeek.model.Constants.YEAR_UNKNOWN
import com.boardgamegeek.provider.BggContract

class CollectionItemEntity(
        val gameId: Int = BggContract.INVALID_ID,
        val gameName: String = "",
        val collectionId: Int = BggContract.INVALID_ID,
        val collectionName: String = "",
        val sortName: String = "",
        val yearPublished: Int = YEAR_UNKNOWN,
        val imageUrl: String = "",
        val thumbnailUrl: String = "",
        val minNumberOfPlayers: Int = 0,
        val maxNumberOfPlayers: Int = 0,
        val minPlayingTime: Int = 0,
        val maxPlayingTime: Int = 0,
        val playingTime: Int = 0,
        val numberOwned: Int = 0,
        val rating: Double = 0.0,
        val numberOfUsersRated: Int = 0,
        val average: Double = 0.0,
        val bayesAverage: Double = 0.0,
        val standardDeviation: Double = 0.0,
        val median: Double = 0.0,
        val own: Boolean = false,
        val previouslyOwned: Boolean = false,
        val forTrade: Boolean = false,
        val want: Boolean = false,
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
        val wishListComment: String = ""
)