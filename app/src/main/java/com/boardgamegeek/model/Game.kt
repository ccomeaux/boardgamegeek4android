package com.boardgamegeek.model

import android.graphics.Color
import com.boardgamegeek.extensions.getImageId
import com.boardgamegeek.provider.BggContract

data class Game(
    val id: Int = BggContract.INVALID_ID,
    val name: String = "",
    val sortName: String = "",
    val subtype: Subtype? = null,
    val thumbnailUrl: String = "",
    val imageUrl: String = "",
    val heroImageUrl: String = "",
    val description: String = "",
    val yearPublished: Int = YEAR_UNKNOWN,
    val minPlayers: Int = 0,
    val maxPlayers: Int = 0,
    val playingTime: Int = 0,
    val minPlayingTime: Int = 0,
    val maxPlayingTime: Int = 0,
    val minimumAge: Int = 0,
    val numberOfRatings: Int = 0,
    val rating: Double = UNRATED,
    val bayesAverage: Double = UNRATED,
    val standardDeviation: Double = 0.0,
    val median: Double = UNRATED,
    val numberOfUsersOwned: Int = 0,
    val numberOfUsersTrading: Int = 0,
    val numberOfUsersWanting: Int = 0,
    val numberOfUsersWishListing: Int = 0,
    val numberOfComments: Int = 0,
    val numberOfUsersWeighting: Int = 0,
    val averageWeight: Double = 0.0,
    val overallRank: Int = GameSubtype.RANK_UNKNOWN,
    val updated: Long = 0,
    val updatedPlays: Long = 0,
    val customPlayerSort: Boolean = false,
    val isFavorite: Boolean = false,
    val pollVoteTotal: Int = 0,
    val suggestedPlayerCountPollVoteTotal: Int = 0,
    val iconColor: Int = Color.TRANSPARENT,
    val darkColor: Int = Color.TRANSPARENT,
    val winsColor: Int = Color.TRANSPARENT,
    val winnablePlaysColor: Int = Color.TRANSPARENT,
    val allPlaysColor: Int = Color.TRANSPARENT,
    val playerCountsBest: Set<Int>?,
    val playerCountsRecommended: Set<Int>?,
    val playerCountsNotRecommended: Set<Int>?,
    val lastViewedTimestamp: Long,
    val lastPlayTimestamp: Long?,
) {
    val maxUsers: Int
        get() {
            return listOf(
                numberOfRatings,
                numberOfComments,
                numberOfUsersOwned,
                numberOfUsersTrading,
                numberOfUsersWanting,
                numberOfUsersWeighting,
                numberOfUsersWishListing
            ).maxOrNull() ?: 0
        }

    override fun toString() = "$id: $name"

    fun doesHeroImageNeedUpdating(): Boolean {
        return heroImageUrl.getImageId() != thumbnailUrl.getImageId()
    }

    enum class Subtype(val databaseValue: String) {
        BoardGame("boardgame"),
        BoardGameExpansion("boardgameexpansion"),
        BoardGameAccessory("boardgameaccessory"),
        Unknown(""),
    }

    companion object {
        const val YEAR_UNKNOWN = 0
        const val UNRATED = 0.0
        const val UNWEIGHTED = 0.0
    }
}
