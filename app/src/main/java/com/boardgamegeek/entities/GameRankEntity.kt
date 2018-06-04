package com.boardgamegeek.entities

import com.boardgamegeek.io.BggService
import com.boardgamegeek.model.Constants

data class GameRankEntity(
        val id: Int = 0,
        val type: String = "",
        val name: String = "",
        val friendlyName: String = "",
        val value: Int = Constants.RANK_UNKNOWN,
        val bayesAverage: Double = 0.0
) {
    val isFamilyType: Boolean
        get() = BggService.RANK_TYPE_FAMILY == type
}