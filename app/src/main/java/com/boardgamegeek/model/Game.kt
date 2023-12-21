package com.boardgamegeek.model

import android.graphics.Color
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
    val overallRank: Int = GameRank.RANK_UNKNOWN,
    val ranks: List<GameRank> = emptyList(),
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
    val playerCountsBest: String?,
    val playerCountsRecommended: String?,
    val playerCountsNotRecommended: String?,
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

    enum class Subtype(val code: String) {
        BOARDGAME("boardgame"),
        BOARDGAME_EXPANSION("boardgameexpansion"),
        BOARDGAME_ACCESSORY("boardgameaccessory"),
    }

    companion object {
        const val YEAR_UNKNOWN = 0
        const val UNRATED = 0.0
    }
}
