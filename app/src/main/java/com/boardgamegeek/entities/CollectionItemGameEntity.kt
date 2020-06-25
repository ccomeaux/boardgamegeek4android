package com.boardgamegeek.entities

import com.boardgamegeek.provider.BggContract

data class CollectionItemGameEntity(
        val gameId: Int = BggContract.INVALID_ID,
        val gameName: String = "",
        val sortName: String = "",
        val yearPublished: Int = YEAR_UNKNOWN,
        val imageUrl: String = "",
        val thumbnailUrl: String = "",
        val minNumberOfPlayers: Int = 0,
        val maxNumberOfPlayers: Int = 0,
        val minPlayingTime: Int = 0,
        val maxPlayingTime: Int = 0,
        val playingTime: Int = 0,
        val numberOwned: Int = 0,
        val rating: Double = 0.0,
        val numberOfUsersRated: Int = 0,
        val average: Double = 0.0,
        val bayesAverage: Double = 0.0,
        val standardDeviation: Double = 0.0,
        val median: Double = 0.0,
        val numberOfPlays: Int = 0
)