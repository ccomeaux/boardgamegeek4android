package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.GamePlayerPollEntity

class RecommendedPlayerCountFilterer(context: Context) : CollectionFilterer(context) {
    var playerCount = 4
    var recommendation = RECOMMENDED

    override val typeResourceId = R.string.collection_filter_type_recommended_player_count

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        playerCount = d.getOrNull(0)?.toInt() ?: 4
        recommendation = d.getOrNull(1)?.toInt() ?: RECOMMENDED
    }

    override fun deflate() = "$playerCount$DELIMITER$recommendation"

    override fun toShortDescription(): String {
        return context.getString(R.string.recommended_player_count_description_abbr,
                when (recommendation) {
                    BEST -> context.getString(R.string.best)
                    else -> context.getString(R.string.good)
                },
                playerCount)
    }

    override fun toLongDescription(): String {
        return context.resources.getQuantityString(R.plurals.recommended_player_count_description,
                playerCount,
                context.getString(when (recommendation) {
                    BEST -> R.string.best
                    else -> R.string.recommended
                }),
                playerCount)
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        val seg = "${GamePlayerPollEntity.separator}$playerCount${GamePlayerPollEntity.separator}"
        return when (recommendation) {
            BEST -> item.bestPlayerCounts.contains(seg)
            RECOMMENDED -> item.recommendedPlayerCounts.contains(seg)
            else -> true
        }
    }

    companion object {
        const val RECOMMENDED = 1
        const val BEST = 2
    }
}
