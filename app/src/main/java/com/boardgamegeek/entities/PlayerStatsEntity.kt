package com.boardgamegeek.entities

data class PlayerStatsEntity(private val players: List<PlayerEntity>) {
    val hIndex: HIndexEntity by lazy {
        HIndexEntity.fromList(players.map { it.playCount })
    }

    fun getHIndexPlayers(): List<Pair<String, Int>> {
        if (hIndex.h == 0) return emptyList()
        return players.map { it.description to it.playCount }
    }
}