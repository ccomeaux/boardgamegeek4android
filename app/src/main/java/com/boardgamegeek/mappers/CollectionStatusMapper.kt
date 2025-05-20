package com.boardgamegeek.mappers

import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionStatus

fun CollectionStatus.mapToPreference() = when (this) {
    CollectionStatus.Own -> COLLECTION_STATUS_OWN
    CollectionStatus.PreviouslyOwned -> COLLECTION_STATUS_PREVIOUSLY_OWNED
    CollectionStatus.Preordered -> COLLECTION_STATUS_PREORDERED
    CollectionStatus.Played -> COLLECTION_STATUS_PLAYED
    CollectionStatus.ForTrade -> COLLECTION_STATUS_FOR_TRADE
    CollectionStatus.WantInTrade -> COLLECTION_STATUS_WANT_IN_TRADE
    CollectionStatus.WantToBuy -> COLLECTION_STATUS_WANT_TO_BUY
    CollectionStatus.WantToPlay -> COLLECTION_STATUS_WANT_TO_PLAY
    CollectionStatus.Wishlist -> COLLECTION_STATUS_WISHLIST
    CollectionStatus.Rated -> COLLECTION_STATUS_RATED
    CollectionStatus.Commented -> COLLECTION_STATUS_COMMENTED
    CollectionStatus.HasParts -> COLLECTION_STATUS_HAS_PARTS
    CollectionStatus.WantParts -> COLLECTION_STATUS_WANT_PARTS
    CollectionStatus.Unknown -> ""
}

fun String?.mapToEnum() = when (this) {
    COLLECTION_STATUS_OWN -> CollectionStatus.Own
    COLLECTION_STATUS_PREVIOUSLY_OWNED -> CollectionStatus.PreviouslyOwned
    COLLECTION_STATUS_PREORDERED -> CollectionStatus.Preordered
    COLLECTION_STATUS_PLAYED -> CollectionStatus.Played
    COLLECTION_STATUS_FOR_TRADE -> CollectionStatus.ForTrade
    COLLECTION_STATUS_WANT_IN_TRADE -> CollectionStatus.WantInTrade
    COLLECTION_STATUS_WANT_TO_BUY -> CollectionStatus.WantToBuy
    COLLECTION_STATUS_WANT_TO_PLAY -> CollectionStatus.WantToPlay
    COLLECTION_STATUS_WISHLIST -> CollectionStatus.Wishlist
    COLLECTION_STATUS_RATED -> CollectionStatus.Rated
    COLLECTION_STATUS_COMMENTED -> CollectionStatus.Commented
    COLLECTION_STATUS_HAS_PARTS -> CollectionStatus.HasParts
    COLLECTION_STATUS_WANT_PARTS -> CollectionStatus.WantParts
    else -> CollectionStatus.Unknown
}
