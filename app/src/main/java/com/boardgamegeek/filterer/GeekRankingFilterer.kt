package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.entities.GameRankEntity
import com.boardgamegeek.extensions.IntervalDelegate
import java.util.*

class GeekRankingFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(lowerBound, lowerBound, upperBound)
    var max by IntervalDelegate(upperBound, lowerBound, upperBound)
    var includeUnranked = false

    override val typeResourceId = R.string.collection_filter_type_geek_ranking

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toIntOrNull() ?: lowerBound
        max = d.getOrNull(1)?.toIntOrNull() ?: upperBound
        includeUnranked = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUnranked) "1" else "0"}"

    override fun toShortDescription() = describe(R.string.unranked_abbr)

    override fun toLongDescription() = context.getString(R.string.ranked) + " " + describe(R.string.unranked)

    private fun describe(@StringRes unrankedResId: Int): String {
        val range: String = when {
            max == upperBound -> String.format(Locale.getDefault(), "%,d+", min)
            min == max -> String.format(Locale.getDefault(), "%,d", max)
            min == lowerBound -> String.format(Locale.getDefault(), "<%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        val unranked = if (includeUnranked) " (+${context.getString(unrankedResId)})" else ""
        return "#$range$unranked"
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        return when {
            item.rank == GameRankEntity.RANK_UNKNOWN -> includeUnranked
            max == upperBound -> item.rank >= min
            min == lowerBound -> item.rank <= max
            min == max -> item.rank == min
            else -> item.rank in min..max
        }
    }

    companion object {
        const val lowerBound = 1
        const val upperBound = 2000
    }
}
