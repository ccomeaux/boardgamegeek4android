package com.boardgamegeek.sorter

import android.content.Context
import timber.log.Timber

class CollectionSorterFactory(context: Context) {
    private val sorters = mutableListOf<CollectionSorter>()

    init {
        with(sorters) {
            clear()
            add(CollectionNameSorter(context))
            add(GeekRatingSorter(context))
            add(YearPublishedAscendingSorter(context))
            add(YearPublishedDescendingSorter(context))
            add(PlayTimeAscendingSorter(context))
            add(PlayTimeDescendingSorter(context))
            add(SuggestedAgeAscendingSorter(context))
            add(SuggestedAgeDescendingSorter(context))
            add(AverageWeightAscendingSorter(context))
            add(AverageWeightDescendingSorter(context))
            add(PlayCountAscendingSorter(context))
            add(PlayCountDescendingSorter(context))
            add(LastPlayDateDescendingSorter(context))
            add(LastPlayDateAscendingSorter(context))
            add(WishlistPrioritySorter(context))
            add(LastViewedSorter(context))
            add(MyRatingSorter(context))
            add(RankSorter(context))
            add(AverageRatingSorter(context))
            add(AcquisitionDateSorter(context))
            add(AcquiredFromSorter(context))
            add(PricePaidSorter(context))
            add(CurrentValueSorter(context))
            add(InventoryLocationSorter(context))
            add(LastModifiedDateSorter(context))
        }
    }

    fun create(type: Int): CollectionSorter? {
        return sorters
                .filter { it.type == type }
                .firstOrNull { return it }
                ?: if (type == TYPE_DEFAULT) {
                    Timber.w("Default sort type not found.")
                    null
                } else {
                    Timber.i("Sort type $type not found; attempting to use default")
                    create(TYPE_DEFAULT)
                }
    }

    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_DEFAULT = 1 // name
    }
}
