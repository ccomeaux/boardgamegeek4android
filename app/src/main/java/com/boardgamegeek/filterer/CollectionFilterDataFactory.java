package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CollectionFilterDataFactory {
	private final List<CollectionFilterer> filterers;

	public CollectionFilterDataFactory(@NonNull Context context) {
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

	public static final int TYPE_COLLECTION_STATUS = 1;
	public static final int TYPE_PLAYER_NUMBER = 2;
	public static final int TYPE_PLAY_TIME = 3;
	public static final int TYPE_SUGGESTED_AGE = 4;
	public static final int TYPE_AVERAGE_WEIGHT = 5;
	public static final int TYPE_YEAR_PUBLISHED = 6;
	public static final int TYPE_AVERAGE_RATING = 7;
	public static final int TYPE_GEEK_RATING = 8;
	public static final int TYPE_GEEK_RANKING = 9;
	public static final int TYPE_EXPANSION_STATUS = 10;
	public static final int TYPE_PLAY_COUNT = 11;
	public static final int TYPE_MY_RATING = 12;
}
