package com.boardgamegeek.data;

import android.content.Context;
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

	public static CollectionFilterData create(Context context, int type, String data) {
		if (TextUtils.isEmpty(data)) {
			return null;
		}
		switch (type) {
			case TYPE_COLLECTION_STATUS:
				return new CollectionStatusFilterData(context, data);
			case TYPE_PLAYER_NUMBER:
				return new PlayerNumberFilterData(context, data);
			case TYPE_PLAY_TIME:
				return new PlayTimeFilterData(context, data);
			case TYPE_SUGGESTED_AGE:
				return new SuggestedAgeFilterData(context, data);
			case TYPE_AVERAGE_WEIGHT:
				return new AverageWeightFilterData(context, data);
			case TYPE_YEAR_PUBLISHED:
				return new YearPublishedFilterData(context, data);
			case TYPE_AVERAGE_RATING:
				return new AverageRatingFilterData(context, data);
			case TYPE_GEEK_RATING:
				return new GeekRatingFilterData(context, data);
			case TYPE_GEEK_RANKING:
				return new GeekRankingFilterData(context, data);
			case TYPE_EXPANSION_STATUS:
				return new ExpansionStatusFilterData(context, data);
			case TYPE_PLAY_COUNT:
				return new PlayCountFilterData(context, data);
			default:
				return null;
		}
	}
}
