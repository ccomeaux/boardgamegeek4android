package com.boardgamegeek.entities

class PersonStatsEntity(
        val averageRating: Double,
        val whitmoreScore: Int,
        val whitmoreScoreWithExpansions: Int
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

            return PersonStatsEntity(averageRating, whitmoreScore, whitmoreScoreWithExpansions)
        }
    }
}
