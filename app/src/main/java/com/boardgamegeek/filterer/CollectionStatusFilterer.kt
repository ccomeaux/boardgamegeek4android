package com.boardgamegeek.filterer

import android.content.Context
import com.boardgamegeek.R
import com.boardgamegeek.model.CollectionItem
import com.boardgamegeek.extensions.joinTo
import com.boardgamegeek.mappers.mapToEnum
import com.boardgamegeek.model.CollectionStatus

class CollectionStatusFilterer(context: Context) : CollectionFilterer(context) {
    var shouldJoinWithOr = false
    var selectedStatuses = BooleanArray(0)

    override val typeResourceId = R.string.collection_filter_type_collection_status

    override fun inflate(data: String) {
        shouldJoinWithOr = data.substringBefore(DELIMITER, "0") == "1"
        selectedStatuses = data.substringAfter(DELIMITER).split(DELIMITER).map { it == "1" }.toBooleanArray()
    }

    override fun deflate(): String {
        return if (shouldJoinWithOr) "1" else "0" +
                DELIMITER +
                selectedStatuses.map { if (it) "1" else "0" }.joinTo(DELIMITER)
    }

    override val iconResourceId: Int
        get() = R.drawable.ic_baseline_library_books_24

    override fun chipText(): String {
        val entries = context.resources.getStringArray(R.array.collection_status_filter_entries)
        return selectedStatuses.indices
            .filter { selectedStatuses[it] }
            .map { entries[it] }
            .joinTo(if (shouldJoinWithOr) " | " else " & ")
            .toString()
    }

    override fun description(): String {
        return context.getString(R.string.status_of_prefix, chipText())
    }

    override fun filter(item: CollectionItem): Boolean {
        val statuses = selectedStatuses.indices.filter { selectedStatuses[it] }

        if (shouldJoinWithOr) {
            statuses.forEach {
                when (it) {
                    OWN -> if (item.own) return true
                    PREVIOUSLY_OWNED -> if (item.previouslyOwned) return true
                    FOR_TRADE -> if (item.forTrade) return true
                    WANT_IN_TRADE -> if (item.wantInTrade) return true
                    WANT_TO_BUY -> if (item.wantToBuy) return true
                    WISHLIST -> if (item.wishList) return true
                    WANT_TO_PLAY -> if (item.wantToPlay) return true
                    PREORDERED -> if (item.preOrdered) return true
                }
            }
            return false
        } else {
            statuses.forEach {
                when (it) {
                    OWN -> if (!item.own) return false
                    PREVIOUSLY_OWNED -> if (!item.previouslyOwned) return false
                    FOR_TRADE -> if (!item.forTrade) return false
                    WANT_IN_TRADE -> if (!item.wantInTrade) return false
                    WANT_TO_BUY -> if (!item.wantToBuy) return false
                    WISHLIST -> if (!item.wishList) return false
                    WANT_TO_PLAY -> if (!item.wantToPlay) return false
                    PREORDERED -> if (!item.preOrdered) return false
                }
            }
            return true
        }
    }

    /**
     * @return a set of status values representing the statuses currently selected within this filter.
     */
    fun getSelectedStatusesSet(): Set<CollectionStatus> {
        val values = context.resources.getStringArray(R.array.pref_sync_status_values)
        return selectedStatuses.indices
            .filter { selectedStatuses[it] }
            .map { values[it].mapToEnum() }
            .toSet()
    }

    companion object {
        private const val OWN = 0
        private const val PREVIOUSLY_OWNED = 1
        private const val FOR_TRADE = 2
        private const val WANT_IN_TRADE = 3
        private const val WANT_TO_BUY = 4
        private const val WISHLIST = 5
        private const val WANT_TO_PLAY = 6
        private const val PREORDERED = 7
    }
}
