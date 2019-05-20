package com.boardgamegeek.entities

class PersonStatsEntity(
        val averageRating: Double,
        val whitmoreScore: Int,
        val whitmoreScoreWithExpansions: Int,
        val playCount: Int,
        val hIndex: Int
) {
    companion object {
        fun fromLinkedCollection(collection: List<BriefGameEntity>): PersonStatsEntity {
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

            val hIndexList = baseGameCollection
                    .distinctBy { it.gameId }
                    .sortedByDescending { it.playCount }

            var hIndexCounter = 0
            var hIndex = 0
            for (value in hIndexList) {
                hIndexCounter++
                if (hIndexCounter > value.playCount) {
                    hIndex = hIndexCounter - 1
                    break
                }
            }
            if (hIndex == 0) hIndex = hIndexCounter

            return PersonStatsEntity(averageRating, whitmoreScore, whitmoreScoreWithExpansions, playCount, hIndex)
        }
    }
}
