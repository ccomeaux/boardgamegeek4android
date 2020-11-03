package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.entities.CollectionItemEntity
import com.boardgamegeek.extensions.joinTo

class CollectionStatusFilterer(context: Context) : CollectionFilterer(context) {
    var selectedStatuses: BooleanArray = BooleanArray(0)
    var shouldJoinWithOr: Boolean = false


    override val typeResourceId = R.string.collection_filter_type_collection_status

    override fun inflate(data: String) {
        val d = data.split(DELIMITER)
        shouldJoinWithOr = d.getOrNull(0) == "1"
        val ss = BooleanArray(d.size - 1)
        for (i in 0 until d.size - 1) {
            ss[i] = d[i + 1] == "1"
        }
        selectedStatuses = ss
    }

    override fun deflate(): String {
        val sb = StringBuilder(if (shouldJoinWithOr) "1" else "0")
        selectedStatuses.forEach { selected ->
            sb.append(DELIMITER).append(if (selected) "1" else "0")
        }
        return sb.toString()
    }

    override fun toShortDescription(): String {
        val entries = context.resources.getStringArray(R.array.collection_status_filter_entries)
        val selectedEntries = mutableListOf<String>()
        selectedStatuses.indices
                .filter { selectedStatuses[it] }
                .mapTo(selectedEntries) { entries[it] }
        return selectedEntries.joinTo(if (shouldJoinWithOr) " | " else " & ").toString()
    }

    override fun toLongDescription(): String {
        return context.getString(R.string.status_of_prefix, toShortDescription())
    }

    override fun filter(item: CollectionItemEntity): Boolean {
        val statuses = selectedStatuses.indices.filter { selectedStatuses[it] }

         if (shouldJoinWithOr) {
            statuses.forEach {
                when (it) {
                    own -> if (item.own) return true
                    previouslyOwned -> if (item.previouslyOwned) return true
                    forTrade -> if (item.forTrade) return true
                    wantInTrade -> if (item.wantInTrade) return true
                    wantToBuy -> if (item.wantToBuy) return true
                    wishList -> if (item.wishList) return true
                    wantToPlay -> if (item.wantToPlay) return true
                    preOrdered -> if (item.preOrdered) return true
                }
            }
            return false
        } else {
            statuses.forEach {
                when (it) {
                    own -> if (!item.own) return false
                    previouslyOwned -> if (!item.previouslyOwned) return false
                    forTrade -> if (!item.forTrade) return false
                    wantInTrade -> if (!item.wantInTrade) return false
                    wantToBuy -> if (!item.wantToBuy) return false
                    wishList -> if (!item.wishList) return false
                    wantToPlay -> if (!item.wantToPlay) return false
                    preOrdered -> if (!item.preOrdered) return false
                }
            }
            return true
        }
    }

    /**
     * @return a set of status values representing the statuses currently selected within this filter.
     */
    fun getSelectedStatusesSet(): Set<String> {
        val selectedStatusesSet = hashSetOf<String>()
        val values = context.resources.getStringArray(R.array.pref_sync_status_values)
        selectedStatuses.indices
                .filter { selectedStatuses[it] }
                .mapTo(selectedStatusesSet) { values[it] }
        return selectedStatusesSet
    }

    companion object {
        const val own = 0
        const val previouslyOwned = 1
        const val forTrade = 2
        const val wantInTrade = 3
        const val wantToBuy = 4
        const val wishList = 5
        const val wantToPlay = 6
        const val preOrdered = 7
    }
}
