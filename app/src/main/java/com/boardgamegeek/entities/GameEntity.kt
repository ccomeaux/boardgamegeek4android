package com.boardgamegeek.entities

import android.graphics.Color
import com.boardgamegeek.provider.BggContract

class GameEntity(
        val id: Int = BggContract.INVALID_ID,
        val name: String = "",
        val sortName: String = "",
        val subtype: String = "",
        override val thumbnailUrl: String = "",
        override val imageUrl: String = "",
        val description: String = "",
        val yearPublished: Int = YEAR_UNKNOWN,
        val minPlayers: Int = 0,
        val maxPlayers: Int = 0,
        val playingTime: Int = 0,
        val minPlayingTime: Int = 0,
        val maxPlayingTime: Int = 0,
        val minimumAge: Int = 0
) : ImagesEntity {
    var hasStatistics = false
    var numberOfRatings = 0
    var rating = 0.0
    var bayesAverage = 0.0
    var standardDeviation = 0.0
    var median = 0.0
    var numberOfUsersOwned = 0
    var numberOfUsersTrading = 0
    var numberOfUsersWanting: Int = 0
    var numberOfUsersWishListing: Int = 0
    var numberOfComments: Int = 0
    var numberOfUsersWeighting: Int = 0
    var averageWeight: Double = 0.0
    var overallRank: Int = RANK_UNKNOWN
    var ranks = arrayListOf<GameRankEntity>()

    override var heroImageUrl = ""
    override val imagesEntityDescription: String
        get() = "$name ($id)"

    var updated: Long = 0
    var updatedPlays: Long = 0
    var customPlayerSort: Boolean = false
    var isFavorite: Boolean = false
    var pollVoteTotal: Int = 0
    var suggestedPlayerCountPollVoteTotal: Int = 0
    var iconColor: Int = Color.TRANSPARENT
    var darkColor: Int = Color.TRANSPARENT
    var winsColor: Int = Color.TRANSPARENT
    var winnablePlaysColor: Int = Color.TRANSPARENT
    var allPlaysColor: Int = Color.TRANSPARENT

    val designers = arrayListOf<Pair<Int, String>>()
    val artists = arrayListOf<Pair<Int, String>>()
    val publishers = arrayListOf<Pair<Int, String>>()
    val categories = arrayListOf<Pair<Int, String>>()
    val mechanics = arrayListOf<Pair<Int, String>>()
    val expansions = arrayListOf<Triple<Int, String, Boolean>>()
    val families = arrayListOf<Pair<Int, String>>()

    var polls = arrayListOf<Poll>()
    var playerPoll: GamePlayerPollEntity? = null

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
            return listOf(numberOfRatings,
                    numberOfComments,
                    numberOfUsersOwned,
                    numberOfUsersTrading,
                    numberOfUsersWanting,
                    numberOfUsersWeighting,
                    numberOfUsersWishListing
            ).maxOrNull() ?: 0
        }

    override fun toString() = "$id: $name"
}
