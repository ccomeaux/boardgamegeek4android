package com.boardgamegeek.model

import com.boardgamegeek.extensions.cdf
import com.boardgamegeek.extensions.inverseCdf
import kotlin.math.ln

class PlayStats private constructor(private val games: List<GameForPlayStats>, private val ownedQuantity: Int = OWNED_QUANTITY_UNKNOWN) {
    private var _hIndex: HIndex = HIndex.invalid()
    val hIndex: HIndex
        get() = _hIndex

    private var _gIndex: GIndex = GIndex.invalid()
    val gIndex: GIndex
        get() = _gIndex

    val numberOfPlays: Int by lazy {
        games.sumOf { it.playCount }
    }

    val numberOfPlayedGames: Int by lazy {
        games.filter { it.playCount > 0 }.size
    }

    val numberOfNickels: Int by lazy {
        games.filter { it.playCount in 5..9 }.size
    }

    val numberOfDimes: Int by lazy {
        games.filter { it.playCount in 10..24 }.size
    }

    val numberOfQuarters: Int by lazy {
        games.filter { it.playCount in 25..49 }.size
    }

    val numberOfHalfDollars: Int by lazy {
        games.filter { it.playCount in 50..99 }.size
    }

    val numberOfDollars: Int by lazy {
        games.filter { it.playCount >= 100 }.size
    }

    val top100Count: Int by lazy {
        games.filter { it.playCount > 0 }.filter { it.bggRank in 1..100 }.size
    }

    fun getHIndexGames(): List<Pair<String, Int>> {
        if (hIndex.h == 0) return emptyList()
        return games.map { it.name to it.playCount }.sortedByDescending { it.second }
    }


    val friendless: Int by lazy {
        val numberOfOwnedGamesThatHaveEarnedTheirKeep = ownedAndPlayedGames.filter { it.playCount >= PLAY_COUNT_TO_EARN_KEEP }.size
        val numberOfOwnedUnplayedGames = ownedQuantity - numberOfOwnedAndPlayedGames
        when {
            ownedQuantity == OWNED_QUANTITY_UNKNOWN -> INVALID_FRIENDLESS
            numberOfOwnedAndPlayedGames == 0 -> 0
            numberOfOwnedGamesThatHaveEarnedTheirKeep >= ownedAndPlayedGames.size -> ownedAndPlayedGames.minOfOrNull { it.playCount } ?: INVALID_FRIENDLESS
            else -> {
                val index = ownedAndPlayedGames.lastIndex - numberOfOwnedGamesThatHaveEarnedTheirKeep
                val friendless = ownedAndPlayedGames.sortedByDescending { it.playCount }.getOrNull(index)?.playCount ?: INVALID_FRIENDLESS
                if (friendless == 0) numberOfOwnedGamesThatHaveEarnedTheirKeep - numberOfOwnedUnplayedGames else friendless
            }
        }
    }

    val cfm: Double by lazy {
        // continuous Friendless metric
        when {
            ownedQuantity == OWNED_QUANTITY_UNKNOWN -> INVALID_CFM
            numberOfOwnedAndPlayedGames == 0 -> 0.0
            else -> (totalCdf / numberOfOwnedAndPlayedGames).inverseCdf(lambda)
        }
    }

    val utilization: Double by lazy {
        // “the amount of novelty you've gained from that game compared to all the novelty that can ever be had”
        // every 10 plays utilizes another 90%
        when {
            ownedQuantity == OWNED_QUANTITY_UNKNOWN -> INVALID_UTILIZATION
            numberOfOwnedAndPlayedGames == 0 -> 0.0
            else -> totalCdf / ownedQuantity
        }
    }

    private val ownedAndPlayedGames: List<GameForPlayStats> by lazy {
        games.filter { it.isOwned }
    }

    private val numberOfOwnedAndPlayedGames: Int by lazy {
        ownedAndPlayedGames.size
    }

    private val totalCdf: Double by lazy {
        ownedAndPlayedGames.sumOf { it.playCount.toDouble().cdf(lambda) }
    }

    companion object {
        const val OWNED_QUANTITY_UNKNOWN = -1
        const val INVALID_FRIENDLESS = Integer.MIN_VALUE
        const val INVALID_UTILIZATION = -1.0
        const val INVALID_CFM = -1.0
        private const val PLAY_COUNT_TO_EARN_KEEP = 10
        private val lambda = ln(0.1) / -10

        suspend fun fromList(games: List<GameForPlayStats>, ownedQuantity: Int): PlayStats {
            return PlayStats(games, ownedQuantity).also {
                val playCounts = games.map { game -> game.playCount }
                it._hIndex = HIndex.fromList(playCounts)
                it._gIndex = GIndex.fromList(playCounts)
            }
        }
    }
}
