package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class CollectionFilterDataFactory {
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

	public static CollectionFilterer create(@NonNull Context context, int type, @NonNull String data) {
		if (TextUtils.isEmpty(data)) {
			return null;
		}
		switch (type) {
			case TYPE_COLLECTION_STATUS:
				return new CollectionStatusFilterer(context, data);
			case TYPE_PLAYER_NUMBER:
				return new PlayerNumberFilterer(context, data);
			case TYPE_PLAY_TIME:
				return new PlayTimeFilterer(context, data);
			case TYPE_SUGGESTED_AGE:
				return new SuggestedAgeFilterer(context, data);
			case TYPE_AVERAGE_WEIGHT:
				return new AverageWeightFilterer(context, data);
			case TYPE_YEAR_PUBLISHED:
				return new YearPublishedFilterer(context, data);
			case TYPE_AVERAGE_RATING:
				return new AverageRatingFilterer(context, data);
			case TYPE_GEEK_RATING:
				return new GeekRatingFilterer(context, data);
			case TYPE_GEEK_RANKING:
				return new GeekRankingFilterer(context, data);
			case TYPE_EXPANSION_STATUS:
				return new ExpansionStatusFilterer(context, data);
			case TYPE_PLAY_COUNT:
				return new PlayCountFilterer(context, data);
			case TYPE_MY_RATING:
				return new MyRatingFilterer(context, data);
			default:
				return null;
		}
	}
}
