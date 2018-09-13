package com.boardgamegeek.entities

import com.boardgamegeek.ui.model.HIndexEntry

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

    fun getHIndexPlayers(): List<HIndexEntry> {
        if (hIndex == 0) return emptyList()

        val fromIndex = maxOf(0, players.indexOfFirst { it.playCount <= hIndex } - 1)
        val toIndex = minOf(players.lastIndex, players.indexOfLast { it.playCount >= hIndex } + 1)

        return players.mapIndexed { index, p -> HIndexEntry(p.playCount, index + 1, p.description) }.subList(
                players.indexOfFirst { it.playCount == players[fromIndex].playCount },
                players.indexOfLast { it.playCount == players[toIndex].playCount } + 1
        )
    }
}