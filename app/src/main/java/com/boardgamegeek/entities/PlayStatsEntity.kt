package com.boardgamegeek.entities

import com.boardgamegeek.extensions.cdf
import com.boardgamegeek.extensions.invcdf
import kotlin.math.ln

class PlayStatsEntity(private val games: List<GameForPlayStatEntity>, private val isOwnedSynced: Boolean) {
    companion object {
        const val INVALID_FRIENDLESS = Integer.MIN_VALUE
        const val INVALID_UTILIZATION = -1.0
        const val INVALID_CFM = -1.0
        private const val PLAY_COUNT_TO_EARN_KEEP = 10
        val lambda = ln(0.1) / -10
    }

    val numberOfPlays: Int by lazy {
        games.sumBy { it.playCount }
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

    val hIndex: HIndexEntity by lazy {
        HIndexEntity.fromList(games.map { it.playCount })
    }

    fun getHIndexGames(): List<Pair<String, Int>> {
        if (hIndex.h == 0) return emptyList()
        return games.map { it.name to it.playCount }
    }

    val friendless: Int by lazy {
        when {
            !isOwnedSynced -> INVALID_FRIENDLESS
            numberOfOwnedGames == 0 -> 0
            numberOfOwnedGamesThatHaveEarnedTheirKeep >= ownedGamePlayCountsSorted.size -> ownedGamePlayCountsSorted.last().playCount
            else -> {
                val friendless = ownedGamePlayCountsSorted[ownedGamePlayCountsSorted.lastIndex - numberOfOwnedGamesThatHaveEarnedTheirKeep].playCount
                if (friendless == 0) numberOfOwnedGamesThatHaveEarnedTheirKeep - numberOfOwnedUnplayedGames else friendless
            }
        }
    }

    val cfm: Double by lazy {
        when {
            !isOwnedSynced -> INVALID_CFM
            numberOfOwnedGames == 0 -> 0.0
            else -> (totalCdf / numberOfOwnedGames).invcdf(lambda)
        }
    }

    val utilization: Double by lazy {
        when {
            !isOwnedSynced -> INVALID_UTILIZATION
            numberOfOwnedGames == 0 -> 0.0
            else -> totalCdf / numberOfOwnedGames
        }
    }

    private val numberOfOwnedGames: Int by lazy {
        games.filter { it.isOwned }.size
    }

    private val numberOfOwnedGamesThatHaveEarnedTheirKeep: Int by lazy {
        games.filter { it.isOwned && it.playCount >= PLAY_COUNT_TO_EARN_KEEP }.size
    }

    private val numberOfOwnedUnplayedGames: Int by lazy {
        games.filter { it.isOwned && it.playCount == 0 }.size
    }

    private val ownedGamePlayCountsSorted: List<GameForPlayStatEntity> by lazy {
        games.filter { it.isOwned }
    }

    private val totalCdf: Double by lazy {
        games.asSequence().filter { it.isOwned }.sumByDouble { it.playCount.toDouble().cdf(lambda) }
    }
}