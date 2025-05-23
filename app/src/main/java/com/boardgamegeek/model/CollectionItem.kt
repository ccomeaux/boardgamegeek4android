package com.boardgamegeek.model

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import com.boardgamegeek.R
import com.boardgamegeek.extensions.*
import com.boardgamegeek.provider.BggContract
import java.util.EnumSet
import kotlin.math.pow
import kotlin.time.Duration.Companion.milliseconds

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
    val numberOfUsersWishing: Int = 0,
    val standardDeviation: Double = 0.0,
) {
    val statuses: Pair<EnumSet<CollectionStatus>, Int> by lazy {
        val set = EnumSet.noneOf(CollectionStatus::class.java)
        if (own) set.add(CollectionStatus.Own)
        if (preOrdered) set.add(CollectionStatus.Preordered)
        if (previouslyOwned) set.add(CollectionStatus.PreviouslyOwned)
        if (forTrade) set.add(CollectionStatus.ForTrade)
        if (wantInTrade) set.add(CollectionStatus.WantInTrade)
        if (wantToBuy) set.add(CollectionStatus.WantToBuy)
        if (wantToPlay) set.add(CollectionStatus.WantToPlay)
        if (wishList) set.add(CollectionStatus.Wishlist)
        set to wishListPriority
    }

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

    val robustName: String
        get() = collectionName.ifBlank { gameName }

    val robustThumbnailUrl: String
        get() = thumbnailUrl.ifBlank { gameThumbnailUrl }

    val robustHeroImageUrl: String
        get() = heroImageUrl.ifBlank { thumbnailUrl }.ifBlank { imageUrl }

    fun doesHeroImageNeedUpdating(): Boolean {
        return heroImageUrl.getImageId() != thumbnailUrl.getImageId()
    }

    val zScore = (rating - averageRating) / standardDeviation

    val whitmoreScore: Double by lazy {
        // personal rating on a linear scale of 0 - MAX_WHITMORE_RATING
        // 0 = game rated NEUTRAL_WHITMORE_RATING or less
        // >0 = linear scale from NEUTRAL_WHITMORE_RATING to MAX_WHITMORE_RATING
        if (rating < NEUTRAL_WHITMORE_RATING) 0.0
        else (rating - NEUTRAL_WHITMORE_RATING) * (MAX_WHITMORE_RATING / (10 - NEUTRAL_WHITMORE_RATING))
    }

    val modifiedWhitmoreScore: Double by lazy {
        // converts personal rating to a geometric scale from -10 to 10, centered on NEUTRAL_MODIFIED_WHITMORE_RATING
        // https://boardgamegeek.com/wiki/page/BGG_for_Android_Users_Manual#toc23
        // http://www.boardgamegeek.com/geeklist/37832
        // https://boardgamegeek.com/geeklist/39165/extended-statistics-for-zefquaavius-2009-01-28?commentid=224943
        // w = (myRating - neutralRating)² × SIGN(myRating - neutralRating) / 2.025, where neutralRating is 5.5
        if (rating == UNRATED) {
            0.0
        } else {
            val divisor = (10 - NEUTRAL_MODIFIED_WHITMORE_RATING).pow(2) / 10
            val negativeDivisor = (1 - NEUTRAL_MODIFIED_WHITMORE_RATING).pow(2) / -10
            val d = rating - NEUTRAL_MODIFIED_WHITMORE_RATING
            d * d / (if (rating < NEUTRAL_MODIFIED_WHITMORE_RATING) negativeDivisor else divisor)
        }
    }

    val isIncoming = preOrdered || (wishList && wishListPriority in 1..4) || wantToBuy || wantInTrade

    //https://boardgamegeek.com/geeklist/59785/boardgamegeek-metrics-contains-formulas-and-algori
    val hawt = 1 + (numberOfUsersWanting + numberOfUsersWishing) / (2 + numberOfUsersOwned / 500)
    //val buzz = numberOfUsersWanting + numberOfUsersWishing - numberOfUsersOwned

    //https://github.com/DrFriendless/ExtendedStats/blob/edb664d29777377dbfbee5ad0b15614f6e887df1/extended/stats/generate.py#L1156

    val friendlessFave = rating * 5 + numberOfPlays + (numberOfPlays * playingTime / 60)
    // + number of months played * 4
    // change hours played to actual

    val friendlessShouldPlay = if (rating < 7) 0.0 else lastPlayDate?.let { rating.pow(4) + it.howManyDaysOld() } ?: 0.0
    //Ideas to include unplayed or unrated games
    //- use average rating if unrated
    //- use an arbitrary date if unplayed, like the Jan 1, 1970, first day played of all time, or that minus a day

    private fun sincePlayed() = lastPlayDate?.let { System.currentTimeMillis() - it }?.milliseconds?.inWholeDays?.coerceAtLeast(1) ?: 0

    fun friendlessWhyOwn() = if (rating == UNRATED) 0.0 else (sincePlayed() / rating / rating)

    // val friendlessUtilization = numberOfPlays.toDouble().cdf(ln(0.1) / -10)

    fun getPrivateInfo(context: Context): CharSequence {
        val initialText = context.getString(R.string.acquired)
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
        private const val NEUTRAL_WHITMORE_RATING = 6.5 // Rating translated to a Whitmore score of 0
        private const val MAX_WHITMORE_RATING = 7 // Whitmore score of a 10
        private const val NEUTRAL_MODIFIED_WHITMORE_RATING = 5.5

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
            val syncedStatuses = context.preferences().getSyncStatuses()
            return (syncedStatuses.contains(CollectionStatus.Own) && own) ||
                    (syncedStatuses.contains(CollectionStatus.PreviouslyOwned) && previouslyOwned) ||
                    (syncedStatuses.contains(CollectionStatus.Preordered) && preOrdered) ||
                    (syncedStatuses.contains(CollectionStatus.ForTrade) && forTrade) ||
                    (syncedStatuses.contains(CollectionStatus.WantInTrade) && wantInTrade) ||
                    (syncedStatuses.contains(CollectionStatus.WantToBuy) && wantToBuy) ||
                    (syncedStatuses.contains(CollectionStatus.WantToPlay) && wantToPlay) ||
                    (syncedStatuses.contains(CollectionStatus.Wishlist) && wishList) ||
                    (syncedStatuses.contains(CollectionStatus.Rated) && rating != UNRATED) ||
                    (syncedStatuses.contains(CollectionStatus.Played) && numberOfPlays > 0) ||
                    (syncedStatuses.contains(CollectionStatus.Commented) && comment.isNotBlank()) ||
                    (syncedStatuses.contains(CollectionStatus.HasParts) && hasPartsList.isNotBlank()) ||
                    (syncedStatuses.contains(CollectionStatus.WantParts) && wantPartsList.isNotBlank())
        }

        private const val UNPUBLISHED_PROTOTYPE_ID = 18291

        fun Iterable<CollectionItem>.filterPublishedGames() = filter { it.gameId != UNPUBLISHED_PROTOTYPE_ID }

        fun Iterable<CollectionItem>.filterPlayed() = filter { it.numberOfPlays > 0 }

        fun Iterable<CollectionItem>.filterRated() = filter { it.rating != UNRATED }

        fun Iterable<CollectionItem>.filterUnrated() = filter { it.rating == UNRATED  }

        fun Iterable<CollectionItem>.filterUncommented() = filter { it.comment.isBlank() }

        fun Sequence<CollectionItem>.filterOwned() = filter { it.own }

        fun Sequence<CollectionItem>.filterBaseGames() = filter { it.subtype in listOf(Game.Subtype.BoardGame, null) }

        fun Sequence<CollectionItem>.filterUnplayed() = filter { it.numberOfPlays == 0 }
    }
}
