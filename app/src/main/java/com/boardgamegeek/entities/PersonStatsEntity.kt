package com.boardgamegeek.entities

import android.content.Context
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences

class PersonStatsEntity(
    val averageRating: Double,
    val whitmoreScore: Int,
    val whitmoreScoreWithExpansions: Int,
    val playCount: Int,
    val hIndex: HIndexEntity
) {
    companion object {
        fun fromLinkedCollection(collection: List<BriefGameEntity>, context: Context): PersonStatsEntity {
            val baseGameCollection = collection.filter { it.subtype == GameEntity.Subtype.BOARDGAME }

            val ratedGames = baseGameCollection.filter { it.personalRating > 0.0 }
            val averageRating = if (ratedGames.isEmpty()) 0.0 else (ratedGames.sumOf { it.personalRating }) / ratedGames.size

            val whitmoreScore = calculateWhitmoreScore(baseGameCollection)
            val whitmoreScoreWithExpansions = calculateWhitmoreScore(collection.filter { it.subtype != GameEntity.Subtype.BOARDGAME_ACCESSORY })

            val prefs = context.preferences()
            val includeExpansions = prefs[LOG_PLAY_STATS_EXPANSIONS, false] ?: false
            val includeAccessories = prefs[LOG_PLAY_STATS_ACCESSORIES, false] ?: false
            val playCountsByGame = when {
                includeExpansions && includeAccessories -> collection
                includeAccessories -> collection.filter { it.subtype != GameEntity.Subtype.BOARDGAME_EXPANSION }
                includeExpansions -> collection.filter { it.subtype != GameEntity.Subtype.BOARDGAME_ACCESSORY }
                else -> baseGameCollection
            }
                .distinctBy { it.gameId }
                .map { it.playCount }

            val playCount = playCountsByGame.sum()

            val hIndex = HIndexEntity.fromList(playCountsByGame)

            return PersonStatsEntity(averageRating, whitmoreScore, whitmoreScoreWithExpansions, playCount, hIndex)
        }

        private fun calculateWhitmoreScore(games: List<BriefGameEntity>, neutralRating: Double = 6.5) = games
            .filter { it.personalRating > neutralRating }
            .sumOf { (it.personalRating - neutralRating) * 2.0 }
            .toInt()
    }
}
