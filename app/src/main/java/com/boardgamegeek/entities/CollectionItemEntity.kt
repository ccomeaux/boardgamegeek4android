package com.boardgamegeek.entities

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.extensions.appendBold
import com.boardgamegeek.extensions.asMoney
import com.boardgamegeek.provider.BggContract

data class CollectionItemEntity(
        val internalId: Long = BggContract.INVALID_ID.toLong(),
        val gameId: Int = BggContract.INVALID_ID,
        val gameName: String = "",
        val collectionId: Int = BggContract.INVALID_ID,
        val collectionName: String = "",
        val sortName: String = "",
        val gameYearPublished: Int = YEAR_UNKNOWN,
        val collectionYearPublished: Int = YEAR_UNKNOWN,
        override val imageUrl: String = "",
        override val thumbnailUrl: String = "",
        override val heroImageUrl: String = "",
        val averageRating: Double = 0.0,
        val rating: Double = 0.0,
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
        val wishListDirtyTimestamp: Long = 0L,
        val tradeConditionDirtyTimestamp: Long = 0L,
        val hasPartsDirtyTimestamp: Long = 0L,
        val wantPartsDirtyTimestamp: Long = 0L,
        val winsColor: Int = Color.TRANSPARENT,
        val winnablePlaysColor: Int = Color.TRANSPARENT,
        val allPlaysColor: Int = Color.TRANSPARENT,
        val playingTime: Int = 0,
        val minimumAge: Int = 0,
        val rank: Int = RANK_UNKNOWN,
        val geekRating: Double = 0.0,
        val averageWeight: Double = 0.0,
        val isFavorite: Boolean = false,
        val lastPlayDate: Long = 0L,
        val arePlayersCustomSorted: Boolean = false,
) : ImagesEntity {
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

    fun hasPrivateInfo(): Boolean {
        return quantity > 1 ||
                acquisitionDate > 0L ||
                acquiredFrom.isNotBlank() ||
                pricePaid > 0.0 ||
                currentValue > 0.0 ||
                inventoryLocation.isNotBlank()
    }

    fun getPrivateInfo(context: Context): CharSequence {
        val initialText = context.resources.getString(R.string.acquired)
        val sb = SpannableStringBuilder()
        sb.append(initialText)
        if (quantity > 1) {
            sb.append(" ").appendBold(quantity.toString())
        }
        if (acquisitionDate > 0L) {
            val date = DateUtils.formatDateTime(context, acquisitionDate, DateUtils.FORMAT_SHOW_DATE)
            if (!date.isNullOrEmpty()) {
                sb.append(" ").append(context.getString(R.string.on)).append(" ").appendBold(date)
            }
        }
        if (acquiredFrom.isNotBlank()) {
            sb.append(" ").append(context.getString(R.string.from)).append(" ").appendBold(acquiredFrom)
        }
        if (pricePaid > 0.0) {
            sb.append(" ").append(context.getString(R.string.for_)).append(" ").appendBold(pricePaid.asMoney(pricePaidCurrency))
        }
        if (currentValue > 0.0) {
            sb.append(" (").append(context.getString(R.string.currently_worth)).append(" ").appendBold(currentValue.asMoney(currentValueCurrency)).append(")")
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

    override val imagesEntityDescription: String
        get() = "$collectionName ($collectionId)"

}
