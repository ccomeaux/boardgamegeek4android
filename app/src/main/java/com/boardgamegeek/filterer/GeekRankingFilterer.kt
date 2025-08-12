package com.boardgamegeek.filterer

import android.content.Context
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.IntervalDelegate
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.model.Game
import java.util.Locale

class GeekRankingFilterer(context: Context) : CollectionFilterer(context) {
    var min by IntervalDelegate(LOWER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var max by IntervalDelegate(UPPER_BOUND, LOWER_BOUND, UPPER_BOUND)
    var includeUnranked = false

    override val typeResourceId = R.string.collection_filter_type_geek_ranking

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        min = d.getOrNull(0)?.toIntOrNull() ?: LOWER_BOUND
        max = d.getOrNull(1)?.toIntOrNull() ?: UPPER_BOUND
        includeUnranked = d.getOrNull(2) == "1"
    }

    override fun deflate() = "$min$DELIMITER$max$DELIMITER${if (includeUnranked) "1" else "0"}"

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_emoji_events_24

    override fun chipText() = describe(R.string.unranked_abbr)

    override fun description() = "${context.getString(R.string.ranked)} ${describe(R.string.unranked)}"

    private fun describe(@StringRes unrankedResId: Int): String {
        val unranked = if (includeUnranked) " (+${context.getString(unrankedResId)})" else ""
        return "#${describeRange()}$unranked"
    }

    fun describeRange() = when {
        max == UPPER_BOUND -> String.format(Locale.getDefault(), "%,d+", min)
        min == max -> String.format(Locale.getDefault(), "%,d", max)
        min == LOWER_BOUND -> String.format(Locale.getDefault(), "<%,d", max)
        else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
    }

    override fun filter(item: CollectionItem): Boolean {
        return when {
            item.rank == Game.RANK_UNKNOWN -> includeUnranked
            max == UPPER_BOUND -> item.rank >= min
            min == LOWER_BOUND -> item.rank <= max
            min == max -> item.rank == min
            else -> item.rank in min..max
        }
    }

    companion object {
        const val LOWER_BOUND = 1
        const val UPPER_BOUND = 2000
    }
}
