package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CollectionFilterDialogFactory {
	@NonNull private final List<CollectionFilterDialog> dialogs;

	public CollectionFilterDialogFactory() {
		dialogs = new ArrayList<>();
		dialogs.add(new CollectionNameFilterDialog());
		dialogs.add(new CollectionStatusFilterDialog());
		dialogs.add(new PlayerNumberFilterDialog());
		dialogs.add(new PlayTimeFilterDialog());
		dialogs.add(new SuggestedAgeFilterDialog());
		dialogs.add(new AverageWeightFilterDialog());
		dialogs.add(new YearPublishedFilterDialog());
		dialogs.add(new AverageRatingFilterDialog());
		dialogs.add(new GeekRatingFilterDialog());
		dialogs.add(new GeekRankingFilterDialog());
		dialogs.add(new ExpansionStatusFilterDialog());
		dialogs.add(new PlayCountFilterDialog());
		dialogs.add(new MyRatingFilterDialog());
		dialogs.add(new RecommendedPlayerCountFilterDialog());
	}

	@Nullable
	public CollectionFilterDialog create(Context context, int type) {
		for (CollectionFilterDialog dialog : dialogs) {
			if (dialog.getType(context) == type) {
				return dialog;
			}
		}
		return null;
	}
}
