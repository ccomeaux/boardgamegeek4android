package com.boardgamegeek.entities

import com.boardgamegeek.model.Constants.YEAR_UNKNOWN
import com.boardgamegeek.provider.BggContract

class CollectionItemEntity {
    var gameId = BggContract.INVALID_ID
    var gameName = ""
    var collectionId = BggContract.INVALID_ID
    var collectionName = ""
    var sortName = ""
    var yearPublished = YEAR_UNKNOWN
    var imageUrl: String = ""
    var thumbnailUrl: String = ""

    //stats
    var hasStatistics = false
    var minNumberOfPlayers = 0
    var maxNumberOfPlayers = 0
    var minPlayingTime = 0
    var maxPlayingTime = 0
    var playingTime = 0
    var numberOwned = 0
    var rating = 0.0
    var numberOfUsersRated = 0 // non-brief
    var average = 0.0
    var bayesAverage = 0.0
    var standardDeviation = 0.0 // non-brief
    var median = 0.0 // non-brief
    // ranks - non-brief

    var own = false
    var previouslyOwned = false
    var forTrade = false
    var want = false
    var wantToPlay = false
    var wantToBuy = false
    var wishList = false
    var wishListPriority = 3 // like to have. should this be 0?
    var preOrdered = false
    var lastModifiedDate = 0L

    var numberOfPlays = 0

    // private info
    var pricePaidCurrency = ""
    var pricePaid = 0.0
    var currentValueCurrency = ""
    var currentValue = 0.0
    var quantity = 1
    var acquisitionDate = "" // Long?
    var acquiredFrom = ""
    var privateComment = ""

    var comment = ""
    var conditionText = ""
    var wantPartsList = ""
    var hasPartsList = ""
    var wishListComment = ""
}