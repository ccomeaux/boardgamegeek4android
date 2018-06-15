package com.boardgamegeek.sorter

import android.content.Context
import android.database.Cursor
import android.support.annotation.StringRes
import android.util.SparseArray

import com.boardgamegeek.R
import com.boardgamegeek.entities.RANK_UNKNOWN
import com.boardgamegeek.getInt
import com.boardgamegeek.provider.BggContract.Games
import java.text.NumberFormat

class RankSorter(context: Context) : CollectionSorter(context) {
    private val defaultHeaderText = context.resources.getString(R.string.unranked)
    private val defaultText = context.resources.getString(R.string.text_not_available)

    override val descriptionId: Int
        @StringRes
        get() = R.string.collection_sort_rank

    public override val typeResource: Int
        @StringRes
        get() = R.string.collection_sort_type_rank

    override val sortColumn: String
        get() = Games.GAME_RANK

    public override fun getHeaderText(cursor: Cursor): String {
        val rank = cursor.getInt(Games.GAME_RANK, RANK_UNKNOWN)
        return (0 until RANKS.size())
                .map { RANKS.keyAt(it) }
                .firstOrNull { rank <= it }
                ?.let { RANKS.get(it) }
                ?: defaultHeaderText
    }

    override fun getDisplayInfo(cursor: Cursor): String {
        val rank = cursor.getInt(Games.GAME_RANK, RANK_UNKNOWN)
        return if (rank == RANK_UNKNOWN) {
            defaultText
        } else NUMBER_FORMAT.format(rank)
    }

    companion object {
        private val NUMBER_FORMAT = NumberFormat.getIntegerInstance()
        private val RANKS = buildRanks()

        private fun buildRanks(): SparseArray<String> {
            val rankSteps = listOf(100, 250, 500, 1000, 2500, 5000, 10000)
            val ranks = SparseArray<String>()
            for (i in 0 until rankSteps.size) {
                if (i == 0) {
                    ranks.put(rankSteps[i], String.format("%,d - %,d", 1, rankSteps[i]))
                } else {
                    ranks.put(rankSteps[i], String.format("%,d - %,d", rankSteps[i - 1] + 1, rankSteps[i]))
                }
                ranks.put(Integer.MAX_VALUE, String.format("%,d+", rankSteps[i] + 1))
            }
            return ranks
        }
    }
}
