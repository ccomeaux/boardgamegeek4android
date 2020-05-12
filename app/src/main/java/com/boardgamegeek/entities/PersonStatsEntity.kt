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
            val baseGameCollection = collection.filter { it.subtype == "boardgame" }

            val averageRating =
                    (baseGameCollection.sumByDouble { it.personalRating }) /
                            baseGameCollection.filter { it.personalRating > 0.0 }.size

            val whitmoreScore = baseGameCollection
                    .filter { it.personalRating > 6.5 }
                    .sumByDouble { it.personalRating * 2.0 - 13.0 }
                    .toInt()
            val whitmoreScoreWithExpansions = collection
                    .filter { it.subtype != "boardgameaccessory" }
                    .filter { it.personalRating > 6.5 }
                    .sumByDouble { it.personalRating * 2.0 - 13.0 }
                    .toInt()

            val playCount = baseGameCollection.sumBy { it.playCount } // TODO handle expansions

            val prefs = context.preferences()
            val exp = prefs[LOG_PLAY_STATS_EXPANSIONS, false] ?: false
            val acc = prefs[LOG_PLAY_STATS_ACCESSORIES, false] ?: false
            val hIndexList = when {
                exp && acc -> collection
                acc -> collection.filter { it.subtype != "boardgameexpansion" }
                acc -> collection.filter { it.subtype != "boardgameaccessory" }
                else -> baseGameCollection
            }
                    .distinctBy { it.gameId }
                    .map { it.playCount }

            val hIndex = HIndexEntity.fromList(hIndexList)

            return PersonStatsEntity(averageRating, whitmoreScore, whitmoreScoreWithExpansions, playCount, hIndex)
        }
    }
}
