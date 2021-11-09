package com.boardgamegeek.entities

import com.boardgamegeek.io.BggService

data class GameRankEntity(
    val id: Int = 0,
    val type: String = "",
    val name: String = "",
    val friendlyName: String = "",
    val value: Int = RANK_UNKNOWN,
    val bayesAverage: Double = 0.0
) {
    val isFamilyType: Boolean
        get() = BggService.RANK_TYPE_FAMILY == type

    val isSubType: Boolean
        get() = BggService.RANK_TYPE_SUBTYPE == type

    companion object {
        const val RANK_UNKNOWN = Integer.MAX_VALUE
    }
}
