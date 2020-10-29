package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.provider.BggContract.Games

class RecommendedPlayerCountFilterer(context: Context) : CollectionFilterer(context) {
    var playerCount = 4
    var recommendation = RECOMMENDED
    private val separator = "|"

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

    override fun getColumns() = arrayOf(
            Games.PLAYER_COUNTS_BEST,
            Games.PLAYER_COUNTS_RECOMMENDED,
            Games.PLAYER_COUNTS_NOT_RECOMMENDED,
    )

    override fun getSelection(): String {
        return when (recommendation) {
            BEST -> "${Games.PLAYER_COUNTS_BEST} LIKE ?"
            RECOMMENDED -> "${Games.PLAYER_COUNTS_RECOMMENDED} LIKE ?"
            else -> ""
        }
    }

    override fun getSelectionArgs(): Array<String> {
        return arrayOf("%$separator$playerCount$separator%")
    }

    companion object {
        const val RECOMMENDED = 1
        const val BEST = 2
    }
}
