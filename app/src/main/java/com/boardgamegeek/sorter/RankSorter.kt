package com.boardgamegeek.sorter

import android.content.Context
import android.util.SparseArray
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import java.text.NumberFormat

class RankSorter(context: Context) : CollectionSorter(context) {
    private val defaultHeaderText = context.resources.getString(R.string.unranked)
    private val defaultText = context.resources.getString(R.string.text_not_available)

    override val ascendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_rank

    override val descendingSortTypeResId: Int
        @StringRes
        get() = R.string.collection_sort_type_rank_desc

    override val descriptionResId: Int
        @StringRes
        get() = R.string.collection_sort_rank

    override fun sortAscending(items: Iterable<CollectionItem>) = items.sortedBy { it.rank }

    override fun sortDescending(items: Iterable<CollectionItem>) = items.sortedByDescending { it.rank }

    override fun getHeaderText(item: CollectionItem): String {
        return (0 until ranks.size())
                .map { ranks.keyAt(it) }
                .firstOrNull { item.rank <= it }
                ?.let { ranks.get(it) }
                ?: defaultHeaderText
    }

    override fun getDisplayInfo(item: CollectionItem): String {
        return if (item.rank == CollectionItem.RANK_UNKNOWN) {
            defaultText
        } else NumberFormat.getIntegerInstance().format(item.rank)
    }

    companion object {
        private val ranks = buildRanks()

        private fun buildRanks(): SparseArray<String> {
            val rankSteps = listOf(100, 250, 500, 1000, 2500, 5000, 10000)
            val ranks = SparseArray<String>()
            for (i in rankSteps.indices) {
                ranks.put(rankSteps[i], String.format("%,d - %,d", (rankSteps.getOrElse(i - 1) { 0 }) + 1, rankSteps[i]))
            }
            ranks.put(CollectionItem.RANK_UNKNOWN - 1, String.format("%,d+", rankSteps.last() + 1))
            return ranks
        }
    }
}
