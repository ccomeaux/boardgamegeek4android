package com.boardgamegeek.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerStats private constructor(private val players: List<Player>) {
    private var _hIndex: HIndex = HIndex.invalid()
    val hIndex: HIndex
        get() = _hIndex

    private var _hIndexPlayers: List<Pair<String, Int>> = emptyList()
    val hIndexPlayers: List<Pair<String, Int>>
        get() = _hIndexPlayers

    companion object {
        suspend fun fromList(games: List<Player>): PlayerStats = withContext(Dispatchers.Default) {
            PlayerStats(games).also {
                it._hIndex = HIndex.fromList(it.players.map { player ->  player.playCount })
                if (it.hIndex.isValid()) {
                    it._hIndexPlayers = it.players.map { player -> player.description to player.playCount }
                }
            }
        }
    }
}
