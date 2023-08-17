package com.boardgamegeek.entities

import android.graphics.Color
import com.boardgamegeek.provider.BggContract

data class GameEntity(
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
    val hasStatistics: Boolean = false,
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
    val overallRank: Int = GameRankEntity.RANK_UNKNOWN,
    val ranks: List<GameRankEntity> = emptyList(),
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
    val polls: List<Poll> = emptyList(),
    val playerPoll: GamePlayerPollEntity? = null,
    val designers: List<Pair<Int, String>> = emptyList(),
    val artists: List<Pair<Int, String>> = emptyList(),
    val publishers: List<Pair<Int, String>> = emptyList(),
    val categories: List<Pair<Int, String>> = emptyList(),
    val mechanics: List<Pair<Int, String>> = emptyList(),
    val expansions: List<Triple<Int, String, Boolean>> = emptyList(),
    val families: List<Pair<Int, String>> = emptyList(),
) {
    class Poll {
        var name: String = ""
        var title: String = ""
        var totalVotes: Int = 0
        var results = arrayListOf<Results>()
    }

    class Results {
        var numberOfPlayers: String = ""
        var result = arrayListOf<GamePollResultEntity>()
        val key = "X"
    }

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
