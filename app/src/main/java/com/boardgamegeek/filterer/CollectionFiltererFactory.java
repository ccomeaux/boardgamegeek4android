package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CollectionFiltererFactory {
	public static final int TYPE_UNKNOWN = -1;

	private final List<CollectionFilterer> filterers;

	public CollectionFiltererFactory(@NonNull Context context) {
		filterers = new ArrayList<>();
		filterers.add(new CollectionStatusFilterer(context));
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

	public CollectionFilterer create(int type) {
		for (CollectionFilterer filterer : filterers) {
			if (filterer.getType() == type) {
				return filterer;
			}
		}
		return null;
	}
}
