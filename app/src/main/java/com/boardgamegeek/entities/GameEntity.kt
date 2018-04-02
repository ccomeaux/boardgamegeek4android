package com.boardgamegeek.entities

import android.text.TextUtils
import com.boardgamegeek.model.Constants
import com.boardgamegeek.provider.BggContract

class GameEntity {

    var id: Int = BggContract.INVALID_ID
    var name: String = ""
    var sortName: String = ""
    var subtype: String = ""
    var thumbnailUrl: String = ""
    var imageUrl: String = ""
    var description: String = ""
    var yearPublished: Int = Constants.YEAR_UNKNOWN
    var minPlayers: Int = 0
    var maxPlayers: Int = 0
    var playingTime: Int = 0
    var minPlayingTime: Int = 0
    var maxPlayingTime: Int = 0
    var minAge: Int = 0
    var hasStatistics: Boolean = false
    var numberOfRatings: Int = 0
    var average: Double = 0.toDouble()
    var bayesAverage: Double = 0.toDouble()
    var standardDeviation: Double = 0.toDouble()
    var median: Double = 0.toDouble()
    var numberOfUsersOwned: Int = 0
    var numberOfUsersTrading: Int = 0
    var numberOfUsersWanting: Int = 0
    var numberOfUsersWishListing: Int = 0
    var numberOfComments: Int = 0
    var numberOfUsersWeighting: Int = 0
    var averageWeight: Double = 0.toDouble()
    var overallRank: Int = Int.MAX_VALUE
    var ranks = arrayListOf<Rank>()

    val designers = arrayListOf<Pair<Int, String>>()
    val artists = arrayListOf<Pair<Int, String>>()
    val publishers = arrayListOf<Pair<Int, String>>()
    val categories = arrayListOf<Pair<Int, String>>()
    val mechanics = arrayListOf<Pair<Int, String>>()
    val expansions = arrayListOf<Triple<Int, String, Boolean>>()
    val families = arrayListOf<Pair<Int, String>>()

    var updated: Long = 0
    var updatedList: Long = 0

    var polls = arrayListOf<Poll>()

    class Poll {
        var name: String = ""
        var title: String = ""
        var totalVotes: Int = 0
        var results = arrayListOf<Results>()
    }

    class Results {
        var numberOfPlayers: String = ""
        var result = arrayListOf<Result>()

        val key: String
            get() = if (TextUtils.isEmpty(numberOfPlayers)) {
                "X"
            } else numberOfPlayers

        override fun toString(): String {
            return key
        }
    }

    data class Result(
            var level: Int = 0,
            var value: String = "",
            var numberOfVotes: Int = 0
    )

    data class Rank(
            val id: Int = 0,
            val type: String = "",
            val name: String = "",
            val friendlyName: String = "",
            val value: Int = 0,
            val bayesAverage: Double = 0.toDouble()
    )

    override fun toString(): String = "$id: $name"
}
