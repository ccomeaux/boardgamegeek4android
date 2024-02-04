package com.boardgamegeek.model

import com.boardgamegeek.extensions.cdf
import com.boardgamegeek.extensions.inverseCdf
import kotlin.math.ln

class PlayStats private constructor(private val games: List<GameForPlayStats>, private val isOwnedSynced: Boolean) {
    private var _hIndex: HIndex = HIndex.invalid()
    val hIndex: HIndex
        get() = _hIndex

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
        val numberOfOwnedGamesThatHaveEarnedTheirKeep = ownedGames.filter { it.playCount >= PLAY_COUNT_TO_EARN_KEEP }.size
        val numberOfOwnedUnplayedGames = ownedGames.filter { it.playCount == 0 }.size
        when {
            !isOwnedSynced -> INVALID_FRIENDLESS
            numberOfOwnedGames == 0 -> 0
            numberOfOwnedGamesThatHaveEarnedTheirKeep >= ownedGames.size -> ownedGames.minOfOrNull { it.playCount } ?: 0
            else -> {
                val friendless = ownedGames.sortedByDescending { it.playCount }[ownedGames.lastIndex - numberOfOwnedGamesThatHaveEarnedTheirKeep].playCount
                if (friendless == 0) numberOfOwnedGamesThatHaveEarnedTheirKeep - numberOfOwnedUnplayedGames else friendless
            }
        }
    }

    val cfm: Double by lazy {
        // continuous Friendless metric
        when {
            !isOwnedSynced -> INVALID_CFM
            numberOfOwnedGames == 0 -> 0.0
            else -> (totalCdf / numberOfOwnedGames).inverseCdf(lambda)
        }
    }

    val utilization: Double by lazy {
        // “the amount of novelty you've gained from that game compared to all the novelty that can ever be had”
        // every 10 plays utilizes another 90%
        when {
            !isOwnedSynced -> INVALID_UTILIZATION
            numberOfOwnedGames == 0 -> 0.0
            else -> totalCdf / numberOfOwnedGames
        }
    }

    private val ownedGames: List<GameForPlayStats> by lazy {
        games.filter { it.isOwned }
    }

    private val numberOfOwnedGames: Int by lazy {
        ownedGames.size
    }

    private val totalCdf: Double by lazy {
        ownedGames.sumOf { it.playCount.toDouble().cdf(lambda) }
    }

    companion object {
        const val INVALID_FRIENDLESS = Integer.MIN_VALUE
        const val INVALID_UTILIZATION = -1.0
        const val INVALID_CFM = -1.0
        private const val PLAY_COUNT_TO_EARN_KEEP = 10
        private val lambda = ln(0.1) / -10

        suspend fun fromList(games: List<GameForPlayStats>, isOwnedSynced: Boolean): PlayStats {
            return PlayStats(games, isOwnedSynced).also {
                it._hIndex = HIndex.fromList(games.map { game -> game.playCount })
            }
        }
    }
}
