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
		CollectionFilterer filterer;
		switch (type) {
			case TYPE_COLLECTION_STATUS:
				filterer = new CollectionStatusFilterer(context);
				break;
			case TYPE_PLAYER_NUMBER:
				filterer = new PlayerNumberFilterer(context);
				break;
			case TYPE_PLAY_TIME:
				filterer = new PlayTimeFilterer(context);
				break;
			case TYPE_SUGGESTED_AGE:
				filterer = new SuggestedAgeFilterer(context);
				break;
			case TYPE_AVERAGE_WEIGHT:
				filterer = new AverageWeightFilterer(context);
				break;
			case TYPE_YEAR_PUBLISHED:
				filterer = new YearPublishedFilterer(context);
				break;
			case TYPE_AVERAGE_RATING:
				filterer = new AverageRatingFilterer(context);
				break;
			case TYPE_GEEK_RATING:
				filterer = new GeekRatingFilterer(context);
				break;
			case TYPE_GEEK_RANKING:
				filterer = new GeekRankingFilterer(context);
				break;
			case TYPE_EXPANSION_STATUS:
				filterer = new ExpansionStatusFilterer(context);
				break;
			case TYPE_PLAY_COUNT:
				filterer = new PlayCountFilterer(context);
				break;
			case TYPE_MY_RATING:
				filterer = new MyRatingFilterer(context);
				break;
			default:
				return null;
		}
		if (filterer != null) {
			filterer.setData(data);
		}
		return filterer;
	}
}
