package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Games

class RecommendedPlayerCountFilterer(context: Context) : CollectionFilterer(context) {
    var playerCount = 4
    var recommendation = RECOMMENDED

    override val typeResourceId = R.string.collection_filter_type_recommended_player_count

    override fun inflate(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
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

    override fun getColumns(): Array<String>? {
        return arrayOf(Games.createRecommendedPlayerCountColumn(playerCount.toString()))
    }

    override fun getSelection() = ""

    override fun getSelectionArgs(): Array<String>? = null

    override fun getHaving(): String? {
        return Games.createRecommendedPlayerCountColumn(playerCount.toString()) + if (recommendation == BEST) "=2" else ">0"
    }

    companion object {
        const val RECOMMENDED = 1
        const val BEST = 2
    }
}
