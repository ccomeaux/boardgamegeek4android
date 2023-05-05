package com.boardgamegeek.ui.dialog

import android.content.Context

class CollectionFilterDialogFactory {
    private val dialogs = mutableListOf<CollectionFilterDialog>()

    init {
        // Collection
        dialogs.add(CollectionStatusFilterDialog())
        dialogs.add(MyRatingFilterDialog())
        dialogs.add(CommentFilterDialog())

        dialogs.add(CollectionNameFilterDialog())
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
        dialogs.add(RecommendedPlayerCountFilterDialog())
        dialogs.add(FavoriteFilterDialog())

        // Private Info
        dialogs.add(AcquiredFromFilterDialog())
        dialogs.add(InventoryLocationFilterDialog())
        dialogs.add(PrivateCommentFilterDialog())
    }

    fun create(context: Context, type: Int): CollectionFilterDialog? {
        return dialogs.find { it.getType(context) == type }
    }
}
