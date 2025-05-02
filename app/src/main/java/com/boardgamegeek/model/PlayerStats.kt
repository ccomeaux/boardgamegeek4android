package com.boardgamegeek.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerStats private constructor(private val players: List<Player>) {
    private var _hIndex: HIndex = HIndex.invalid()
    val hIndex: HIndex
        get() = _hIndex

    private var _gIndex: GIndex = GIndex.invalid()
    val gIndex: GIndex
        get() = _gIndex

    private var _hIndexPlayers: List<Pair<String, Int>> = emptyList()
    val hIndexPlayers: List<Pair<String, Int>>
        get() = _hIndexPlayers

    companion object {
        suspend fun fromList(games: List<Player>): PlayerStats = withContext(Dispatchers.Default) {
            PlayerStats(games).also {
                val playCounts = it.players.map { player -> player.playCount }
                it._hIndex = HIndex.fromList(playCounts)
                it._gIndex = GIndex.fromList(playCounts)
                if (it.hIndex.isValid()) {
                    it._hIndexPlayers = it.players.map { player -> player.description to player.playCount }
                }
            }
        }
    }
}
