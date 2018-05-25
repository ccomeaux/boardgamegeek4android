package com.boardgamegeek.ui.model


import com.boardgamegeek.io.BggService

class GameRank(
        val name: String,
        val rank: Int,
        val type: String) {

    val isFamilyType: Boolean
        get() = BggService.RANK_TYPE_FAMILY == type
}
