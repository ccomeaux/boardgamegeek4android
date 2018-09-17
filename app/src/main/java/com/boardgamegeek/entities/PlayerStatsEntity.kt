package com.boardgamegeek.entities

data class PlayerStatsEntity(private val players: List<PlayerEntity>) {
    val hIndex: Int by lazy {
        var hIndexCounter = 0
        var gameHIndex = 0
        for (it in players.filter { it.playCount > 0 }) {
            hIndexCounter++
            if (hIndexCounter > it.playCount) {
                gameHIndex = hIndexCounter - 1
                break
            }
        }
        if (gameHIndex == 0) hIndexCounter else gameHIndex
    }

    fun getHIndexPlayers(): List<Pair<String, Int>> {
        if (hIndex == 0) return emptyList()
        return players.map { it.description to it.playCount }
    }
}