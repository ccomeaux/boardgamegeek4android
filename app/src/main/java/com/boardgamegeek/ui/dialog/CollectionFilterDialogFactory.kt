package com.boardgamegeek.ui.dialog

import android.content.Context
import java.util.*

class CollectionFilterDialogFactory {
    private val dialogs: MutableList<CollectionFilterDialog> = ArrayList()

    init {
        dialogs.add(CollectionNameFilterDialog())
        dialogs.add(CollectionStatusFilterDialog())
        dialogs.add(PlayerNumberFilterDialog())
        dialogs.add(PlayTimeFilterDialog())
        dialogs.add(SuggestedAgeFilterDialog())
        dialogs.add(AverageWeightFilterDialog())
        dialogs.add(YearPublishedFilterDialog())
        dialogs.add(AverageRatingFilterDialog())
        dialogs.add(GeekRatingFilterDialog())
        dialogs.add(GeekRankingFilterDialog())
        dialogs.add(ExpansionStatusFilterDialog())
        dialogs.add(PlayCountFilterDialog())
        dialogs.add(MyRatingFilterDialog())
        dialogs.add(RecommendedPlayerCountFilterDialog())
        dialogs.add(FavoriteFilterDialog())
    }

    fun create(context: Context, type: Int): CollectionFilterDialog? {
        return dialogs.find { it.getType(context) == type }
    }
}
