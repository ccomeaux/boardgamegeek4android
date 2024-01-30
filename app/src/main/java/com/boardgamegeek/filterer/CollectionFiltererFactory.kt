package com.boardgamegeek.filterer

import android.content.Context

class CollectionFiltererFactory(context: Context) {
    private val filterers: MutableList<CollectionFilterer>

    init {
        filterers = arrayListOf()
        filterers.add(CollectionStatusFilterer(context))
        filterers.add(CollectionNameFilter(context))
        filterers.add(PlayerNumberFilterer(context))
        filterers.add(PlayTimeFilterer(context))
        filterers.add(SuggestedAgeFilterer(context))
        filterers.add(AverageWeightFilterer(context))
        filterers.add(YearPublishedFilterer(context))
        filterers.add(AverageRatingFilterer(context))
        filterers.add(GeekRatingFilterer(context))
        filterers.add(GeekRankingFilterer(context))
        filterers.add(ExpansionStatusFilterer(context))
        filterers.add(PlayCountFilterer(context))
        filterers.add(MyRatingFilterer(context))
        filterers.add(RecommendedPlayerCountFilterer(context))
        filterers.add(FavoriteFilterer(context))

        // Price paid
        // Current price
        // Quantity
        // acquisition date
        filterers.add(AcquiredFromFilter(context))
        // inventory date
        filterers.add(InventoryLocationFilter(context))
        filterers.add(PrivateCommentFilter(context))
    }

    fun create(type: Int): CollectionFilterer? = filterers.find { it.type == type }

    fun create(type: Int, data: String): CollectionFilterer? {
        val filter = filterers.find { it.type == type }?.also { it.inflate(data) }
        return if (filter?.isValid == true) filter else null
    }

    companion object {
        const val TYPE_UNKNOWN = -1
        const val TYPE_STATUS = 1
    }
}
