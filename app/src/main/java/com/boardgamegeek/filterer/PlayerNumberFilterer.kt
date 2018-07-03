package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.clamp
import com.boardgamegeek.provider.BggContract.Games
import java.util.*

class PlayerNumberFilterer(context: Context) : CollectionFilterer(context) {
    var min = MIN_RANGE
    var max = MAX_RANGE
    var isExact = false

    override val typeResourceId = R.string.collection_filter_type_number_of_players

    override fun setData(data: String) {
        val d = data.split(DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        min = d.getOrNull(0)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MIN_RANGE
        max = d.getOrNull(1)?.toIntOrNull()?.clamp(MIN_RANGE, MAX_RANGE) ?: MAX_RANGE
        isExact = d.getOrNull(2) == "1"
    }

    override fun flatten() = "$min$DELIMITER$max$DELIMITER${if (isExact) "1" else "0"}"

    override fun getDisplayText(): String {
        var range = if (isExact) {
            context.getString(R.string.exactly) + " "
        } else {
            ""
        }
        range += when (min) {
            max -> String.format(Locale.getDefault(), "%,d", max)
            else -> String.format(Locale.getDefault(), "%,d-%,d", min, max)
        }
        return range + " " + context.getString(R.string.players)
    }

    override fun getSelection(): String {
        return when {
            isExact -> "${Games.MIN_PLAYERS}=? AND ${Games.MAX_PLAYERS}=?"
            else -> "${Games.MIN_PLAYERS}<=? AND (${Games.MAX_PLAYERS}>=? OR ${Games.MAX_PLAYERS} IS NULL)"
        }
    }

    override fun getSelectionArgs(): Array<String>? {
        return arrayOf(min.toString(), max.toString())
    }

    companion object {
        const val MIN_RANGE = 1
        const val MAX_RANGE = 12
    }
}
