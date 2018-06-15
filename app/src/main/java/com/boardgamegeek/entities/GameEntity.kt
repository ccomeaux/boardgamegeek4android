package com.boardgamegeek.entities

import com.boardgamegeek.provider.BggContract

class GameEntity {
    var id = BggContract.INVALID_ID
    var name = ""
    var sortName = ""
    var subtype = ""
    var thumbnailUrl = ""
    var imageUrl = ""
    var description = ""
    var yearPublished = YEAR_UNKNOWN
    var minPlayers = 0
    var maxPlayers = 0
    var playingTime = 0
    var minPlayingTime = 0
    var maxPlayingTime = 0
    var minAge = 0
    var hasStatistics = false
    var numberOfRatings = 0
    var average = 0.0
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
    var overallRank: Int = Int.MAX_VALUE
    var ranks = arrayListOf<GameRankEntity>()

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

    override fun toString() = "$id: $name"
}
