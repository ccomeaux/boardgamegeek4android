package com.boardgamegeek.sorter

import android.content.Context
import timber.log.Timber

class CollectionSorterFactory(context: Context) {
    private val ascendingSorters = mutableMapOf<Int, CollectionSorter>()
    private val descendingSorters = mutableMapOf<Int, CollectionSorter>()

    init {
        add(CollectionNameSorter(context))
        add(GeekRatingSorter(context))
        add(YearPublishedSorter(context))
        add(PlayTimeSorter(context))
        add(SuggestedAgeSorter(context))
        add(AverageWeightSorter(context))
        add(PlayCountSorter(context))
        add(LastPlayDateSorter(context))
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

    private fun add(sorter: CollectionSorter) {
        sorter.apply {
            ascendingSorters += this.ascendingSortType to this
            descendingSorters += this.descendingSortType to this
        }
    }

    fun create(type: Int): Pair<CollectionSorter, Boolean>? {
        val ascendingSorter = ascendingSorters[type]
        if (ascendingSorter != null) return ascendingSorter to false

        val descendingSorter = descendingSorters[type]
        if (descendingSorter != null) return descendingSorter to true

        return if (type == TYPE_DEFAULT) {
            Timber.w("Default sort type not found.")
            null
        } else {
            Timber.i("Sort type $type not found; attempting to use default")
            create(TYPE_DEFAULT)
        }
    }

    fun reverse(type: Int): Int? {
        return ascendingSorters[type]?.descendingSortType ?: descendingSorters[type]?.ascendingSortType
    }

    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_DEFAULT = 1 // name
    }
}
