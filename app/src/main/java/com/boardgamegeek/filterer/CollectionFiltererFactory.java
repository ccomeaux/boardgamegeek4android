package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class CollectionFiltererFactory {
	public static final int TYPE_UNKNOWN = -1;

	private final List<CollectionFilterer> filterers;

	public CollectionFiltererFactory(@NonNull Context context) {
		filterers = new ArrayList<>();
		filterers.add(new CollectionStatusFilterer(context));
		filterers.add(new CollectionNameFilter(context));
		filterers.add(new PlayerNumberFilterer(context));
		filterers.add(new PlayTimeFilterer(context));
		filterers.add(new SuggestedAgeFilterer(context));
		filterers.add(new AverageWeightFilterer(context));
		filterers.add(new YearPublishedFilterer(context));
		filterers.add(new AverageRatingFilterer(context));
		filterers.add(new GeekRatingFilterer(context));
		filterers.add(new GeekRankingFilterer(context));
		filterers.add(new ExpansionStatusFilterer(context));
		filterers.add(new PlayCountFilterer(context));
		filterers.add(new MyRatingFilterer(context));
	}

	@Nullable
	public CollectionFilterer create(int type) {
		Timber.d("Finding filter " + type);
		for (CollectionFilterer filterer : filterers) {
			if (filterer.getType() == type) {
				return filterer;
			}
		}
		Timber.w("Found no filter!");
		return null;
	}
}
