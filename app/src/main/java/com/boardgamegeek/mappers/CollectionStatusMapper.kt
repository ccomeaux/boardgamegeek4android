package com.boardgamegeek.mappers

import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.model.CollectionStatus
import com.boardgamegeek.provider.BggContract

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

fun CollectionStatus.mapToDatabase() = when (this) {
    CollectionStatus.Own -> BggContract.Collection.Columns.STATUS_OWN
    CollectionStatus.PreviouslyOwned -> BggContract.Collection.Columns.STATUS_PREVIOUSLY_OWNED
    CollectionStatus.ForTrade -> BggContract.Collection.Columns.STATUS_FOR_TRADE
    CollectionStatus.WantToPlay -> BggContract.Collection.Columns.STATUS_WANT_TO_PLAY
    CollectionStatus.WantInTrade -> BggContract.Collection.Columns.STATUS_WANT
    CollectionStatus.WantToBuy -> BggContract.Collection.Columns.STATUS_WANT_TO_BUY
    CollectionStatus.Preordered -> BggContract.Collection.Columns.STATUS_PREORDERED
    CollectionStatus.Wishlist -> BggContract.Collection.Columns.STATUS_WISHLIST
    else -> null
}

@StringRes
fun CollectionStatus.mapToResId(): Int = when (this) {
    CollectionStatus.Own -> R.string.collection_status_own
    CollectionStatus.PreviouslyOwned -> R.string.collection_status_prev_owned
    CollectionStatus.ForTrade -> R.string.collection_status_for_trade
    CollectionStatus.WantToPlay -> R.string.collection_status_want_to_play
    CollectionStatus.WantInTrade -> R.string.collection_status_want_in_trade
    CollectionStatus.WantToBuy -> R.string.collection_status_want_to_buy
    CollectionStatus.Preordered -> R.string.collection_status_preordered
    CollectionStatus.Wishlist -> R.string.collection_status_wishlist
    CollectionStatus.Played -> R.string.collection_status_played
    CollectionStatus.Rated -> R.string.collection_status_rated
    CollectionStatus.Commented -> R.string.collection_status_commented
    CollectionStatus.HasParts -> R.string.collection_status_has_parts
    CollectionStatus.WantParts -> R.string.collection_status_want_parts
    CollectionStatus.Unknown -> R.string.unknown
}
