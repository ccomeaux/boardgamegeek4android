package com.boardgamegeek.entities

data class PlayerStats(private val players: List<Player>) {
    val hIndex: HIndex by lazy {
        HIndex.fromList(players.map { it.playCount })
    }

    fun getHIndexPlayers(): List<Pair<String, Int>> {
        if (hIndex.h == 0) return emptyList()
        return players.map { it.description to it.playCount }
    }
}