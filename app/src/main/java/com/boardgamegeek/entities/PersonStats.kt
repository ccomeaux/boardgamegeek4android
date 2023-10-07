package com.boardgamegeek.entities

import android.content.Context
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStats.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences

class PersonStats(
    val averageRating: Double,
    val whitmoreScore: Int,
    val whitmoreScoreWithExpansions: Int,
    val playCount: Int,
    val hIndex: HIndexEntity,
) {
    companion object {
        fun fromLinkedCollection(collection: List<CollectionItemEntity>, context: Context): PersonStats {
            val baseGameCollection = collection.filter { it.subtype == GameEntity.Subtype.BOARDGAME }

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
                .map { it.numberOfPlays }

            return PersonStats(
                baseGameCollection.filter { it.rating > 0.0 }.map { it.rating }.average(),
                whitmoreScore,
                whitmoreScoreWithExpansions,
                playCountsByGame.sum(),
                HIndexEntity.fromList(playCountsByGame)
            )
        }

        private fun calculateWhitmoreScore(games: List<CollectionItemEntity>, neutralRating: Double = 6.5) = games
            .filter { it.rating > neutralRating }
            .sumOf { (it.rating - neutralRating) * 2.0 }
            .toInt()
    }
}
