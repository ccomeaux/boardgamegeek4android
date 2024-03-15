package com.boardgamegeek.model

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract

data class CollectionItem(
    val internalId: Long = BggContract.INVALID_ID.toLong(),
    val gameId: Int = BggContract.INVALID_ID,
    val gameName: String = "",
    val collectionId: Int = BggContract.INVALID_ID,
    val collectionName: String = "",
    val sortName: String = "",
    val gameYearPublished: Int = YEAR_UNKNOWN,
    val collectionYearPublished: Int = YEAR_UNKNOWN,
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val heroImageUrl: String = "",
    val gameImageUrl: String = "",
    val gameThumbnailUrl: String = "",
    val gameHeroImageUrl: String = "",
    val averageRating: Double = UNRATED,
    val rating: Double = UNRATED,
    val own: Boolean = false,
    val previouslyOwned: Boolean = false,
    val forTrade: Boolean = false,
    val wantInTrade: Boolean = false,
    val wantToPlay: Boolean = false,
    val wantToBuy: Boolean = false,
    val wishList: Boolean = false,
    val wishListPriority: Int = WISHLIST_PRIORITY_UNKNOWN,
    val preOrdered: Boolean = false,
    val lastModifiedDate: Long = 0L,
    val lastViewedDate: Long = 0L,
    val numberOfPlays: Int = 0,
    val pricePaidCurrency: String = "",
    val pricePaid: Double = 0.0,
    val currentValueCurrency: String = "",
    val currentValue: Double = 0.0,
    val quantity: Int = 1,
    val acquisitionDate: Long = 0L,
    val acquiredFrom: String = "",
    val privateComment: String = "",
    val inventoryLocation: String = "",
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
    val wishListCommentDirtyTimestamp: Long = 0L,
    val tradeConditionDirtyTimestamp: Long = 0L,
    val hasPartsDirtyTimestamp: Long = 0L,
    val wantPartsDirtyTimestamp: Long = 0L,
    val winsColor: Int = Color.TRANSPARENT,
    val winnablePlaysColor: Int = Color.TRANSPARENT,
    val allPlaysColor: Int = Color.TRANSPARENT,
    val playingTime: Int = 0,
    val minimumAge: Int = 0,
    val rank: Int = RANK_UNKNOWN,
    val geekRating: Double = UNRATED,
    val averageWeight: Double = UNWEIGHTED,
    val isFavorite: Boolean = false,
    val lastPlayDate: Long? = null,
    val arePlayersCustomSorted: Boolean = false,
    val minPlayerCount: Int = 0,
    val maxPlayerCount: Int = 0,
    val subtype: Game.Subtype? = null,
    val bestPlayerCounts: Set<Int>? = null,
    val recommendedPlayerCounts: Set<Int>? = null,
    val numberOfUsersOwned: Int = 0,
    val numberOfUsersWanting: Int = 0,
    val numberOfUsersRating: Int = 0,
    val standardDeviation: Double = 0.0,
) {
    val isDirty: Boolean by lazy {
        when {
            deleteTimestamp > 0L -> true
            dirtyTimestamp > 0L -> true
            statusDirtyTimestamp > 0L -> true
            ratingDirtyTimestamp > 0L -> true
            commentDirtyTimestamp > 0L -> true
            privateInfoDirtyTimestamp > 0L -> true
            wishListCommentDirtyTimestamp > 0L -> true
            tradeConditionDirtyTimestamp > 0L -> true
            hasPartsDirtyTimestamp > 0L -> true
            wantPartsDirtyTimestamp > 0L -> true
            else -> false
        }
    }

    fun hasPrivateInfo(): Boolean {
        return quantity > 1 ||
                acquisitionDate > 0L ||
                acquiredFrom.isNotBlank() ||
                pricePaid > 0.0 ||
                currentValue > 0.0 ||
                inventoryLocation.isNotBlank()
    }

    val yearPublished: Int
        get() = if (collectionYearPublished == YEAR_UNKNOWN) gameYearPublished else collectionYearPublished

    val robustName:String
        get() = collectionName.ifBlank { gameName }

    val robustThumbnailUrl: String
        get() = thumbnailUrl.ifBlank { gameThumbnailUrl }

    val robustHeroImageUrl: String
        get() = heroImageUrl.ifBlank { thumbnailUrl }.ifBlank { imageUrl }

    fun doesHeroImageNeedUpdating(): Boolean {
        return heroImageUrl.getImageId() != thumbnailUrl.getImageId()
    }

    fun getPrivateInfo(context: Context): CharSequence {
        val initialText = context.resources.getString(R.string.acquired)
        val sb = SpannableStringBuilder()
        sb.append(initialText)
        if (quantity > 1) {
            sb.append(" ").appendBold(quantity.toString())
        }
        if (acquisitionDate > 0L) {
            val date = acquisitionDate.formatDateTime(context, flags = DateUtils.FORMAT_SHOW_DATE)
            if (date.isNotEmpty()) {
                sb.append(" ").append(context.getString(R.string.on)).append(" ").appendBold(date.toString())
            }
        }
        if (acquiredFrom.isNotBlank()) {
            sb.append(" ").append(context.getString(R.string.from)).append(" ").appendBold(acquiredFrom)
        }
        if (pricePaid > 0.0) {
            sb.append(" ").append(context.getString(R.string.for_)).append(" ")
                .appendBold(pricePaid.asMoney(pricePaidCurrency))
        }
        if (currentValue > 0.0) {
            sb.append(" (").append(context.getString(R.string.currently_worth)).append(" ")
                .appendBold(currentValue.asMoney(currentValueCurrency)).append(")")
        }
        if (inventoryLocation.isNotBlank()) {
            if (sb.toString() == initialText) {
                sb.clear()
            } else {
                sb.append(". ")
            }
            sb.append(context.getString(R.string.located_in)).append(" ").appendBold(inventoryLocation)
        }

        return if (sb.toString() == initialText) {
            // shouldn't happen
            ""
        } else sb.append(".")
    }

    enum class SortType {
        NAME, RATING
    }

    companion object {
        const val WISHLIST_PRIORITY_UNKNOWN = 0
        const val RANK_UNKNOWN = GameSubtype.RANK_UNKNOWN
        const val YEAR_UNKNOWN = Game.YEAR_UNKNOWN
        const val UNRATED = Game.UNRATED
        const val UNWEIGHTED = Game.UNWEIGHTED

        fun List<CollectionItem>.applySort(sortBy: SortType): List<CollectionItem> {
            return sortedWith(
                if (sortBy == SortType.RATING)
                    compareByDescending<CollectionItem> { it.rating }
                        .thenByDescending { it.isFavorite }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
                else
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.sortName }
            )
        }

        fun CollectionItem.filterBySyncedStatues(context: Context): Boolean {
            val syncedStatuses = context.preferences().getSyncStatusesOrDefault()
            return (syncedStatuses.contains(COLLECTION_STATUS_OWN) && own) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_PREVIOUSLY_OWNED) && previouslyOwned) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_PREORDERED) && preOrdered) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_FOR_TRADE) && forTrade) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_WANT_IN_TRADE) && wantInTrade) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_WANT_TO_BUY) && wantToBuy) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_WANT_TO_PLAY) && wantToPlay) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_WISHLIST) && wishList) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_RATED) && rating != UNRATED) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_PLAYED) && numberOfPlays > 0) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_COMMENTED) && comment.isNotBlank()) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_HAS_PARTS) && hasPartsList.isNotBlank()) ||
                    (syncedStatuses.contains(COLLECTION_STATUS_WANT_PARTS) && wantPartsList.isNotBlank())
        }
    }
}
