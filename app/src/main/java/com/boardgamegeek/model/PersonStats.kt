package com.boardgamegeek.model

import android.content.Context
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_ACCESSORIES
import com.boardgamegeek.extensions.PlayStatPrefs.LOG_PLAY_STATS_EXPANSIONS
import com.boardgamegeek.extensions.get
import com.boardgamegeek.extensions.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PersonStats(
    val averageRating: Double,
    val whitmoreScore: Int,
    val whitmoreScoreWithExpansions: Int,
    val playCount: Int,
    val hIndex: HIndex,
) {
    companion object {
        suspend fun fromLinkedCollection(collection: List<CollectionItem>, context: Context): PersonStats = withContext(Dispatchers.Default) {
            val baseGameCollection = collection.filter { it.subtype == Game.Subtype.BOARDGAME }

            val whitmoreScore = calculateWhitmoreScore(baseGameCollection)
            val whitmoreScoreWithExpansions = calculateWhitmoreScore(collection.filter { it.subtype != Game.Subtype.BOARDGAME_ACCESSORY })

            val includeExpansions = context.preferences()[LOG_PLAY_STATS_EXPANSIONS, false] ?: false
            val includeAccessories = context.preferences()[LOG_PLAY_STATS_ACCESSORIES, false] ?: false
            val playCountsByGame = when {
                includeExpansions && includeAccessories -> collection
                includeAccessories -> collection.filter { it.subtype != Game.Subtype.BOARDGAME_EXPANSION }
                includeExpansions -> collection.filter { it.subtype != Game.Subtype.BOARDGAME_ACCESSORY }
                else -> baseGameCollection
            }
                .distinctBy { it.gameId }
                .map { it.numberOfPlays }

            PersonStats(
                averageRating = baseGameCollection.filter { it.rating > 0.0 }.map { it.rating }.average(),
                whitmoreScore = whitmoreScore,
                whitmoreScoreWithExpansions = whitmoreScoreWithExpansions,
                playCount = playCountsByGame.sum(),
                hIndex = HIndex.fromList(playCountsByGame)
            )
        }

        private const val neutralWhitmoreRating = 6.5
        private const val maxWhitmoreRating = 7

        private fun calculateWhitmoreScore(games: List<CollectionItem>) = games
            .filter { it.rating > neutralWhitmoreRating }
            .sumOf { (it.rating - neutralWhitmoreRating) * (maxWhitmoreRating / (10 - neutralWhitmoreRating)) }
            .toInt()
    }
}
