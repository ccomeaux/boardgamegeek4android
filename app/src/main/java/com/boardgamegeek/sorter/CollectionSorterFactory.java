package com.boardgamegeek.sorter;

import android.content.Context;

public class CollectionSorterFactory {
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_COLLECTION_NAME = 1;
	public static final int TYPE_GEEK_RATING = 2;
	public static final int TYPE_PLAY_COUNT_ASC = 3;
	public static final int TYPE_PLAY_COUNT_DESC = 4;
	public static final int TYPE_YEAR_PUBLISHED_ASC = 5;
	public static final int TYPE_YEAR_PUBLISHED_DESC = 6;
	public static final int TYPE_PLAY_TIME_ASC = 7;
	public static final int TYPE_PLAY_TIME_DESC = 8;
	public static final int TYPE_AGE_ASC = 9;
	public static final int TYPE_AGE_DESC = 10;
	public static final int TYPE_AVERAGE_WEIGHT_ASC = 11;
	public static final int TYPE_AVERAGE_WEIGHT_DESC = 12;
	public static final int TYPE_WISHLIST_PRIORITY = 13;
	public static final int TYPE_LAST_VIEWED = 14;
	public static final int TYPE_MY_RATING = 15;
	public static final int TYPE_RANK = 16;
	public static final int TYPE_AVERAGE_RATING = 17;
	public static final int TYPE_DEFAULT = TYPE_COLLECTION_NAME;

	public static CollectionSorter create(int type, Context context) {
		switch (type) {
			case TYPE_COLLECTION_NAME:
				return new CollectionNameSorter(context);
			case TYPE_GEEK_RATING:
				return new GeekRatingSorter(context);
			case TYPE_YEAR_PUBLISHED_ASC:
				return new YearPublishedAscendingSorter(context);
			case TYPE_YEAR_PUBLISHED_DESC:
				return new YearPublishedDescendingSorter(context);
			case TYPE_PLAY_TIME_ASC:
				return new PlayTimeAscendingSorter(context);
			case TYPE_PLAY_TIME_DESC:
				return new PlayTimeDescendingSorter(context);
			case TYPE_AGE_ASC:
				return new SuggestedAgeAscendingSorter(context);
			case TYPE_AGE_DESC:
				return new SuggestedAgeDescendingSorter(context);
			case TYPE_AVERAGE_WEIGHT_ASC:
				return new AverageWeightAscendingSorter(context);
			case TYPE_AVERAGE_WEIGHT_DESC:
				return new AverageWeightDescendingSorter(context);
			case TYPE_PLAY_COUNT_ASC:
				return new PlayCountAscendingSorter(context);
			case TYPE_PLAY_COUNT_DESC:
				return new PlayCountDescendingSorter(context);
			case TYPE_WISHLIST_PRIORITY:
				return new WishlistPrioritySorter(context);
			case TYPE_LAST_VIEWED:
				return new LastViewedSorter(context);
			case TYPE_MY_RATING:
				return new MyRatingSorter(context);
			case TYPE_RANK:
				return new RankSorter(context);
			case TYPE_AVERAGE_RATING:
				return new AverageRatingSorter(context);
			case TYPE_UNKNOWN:
			default:
				return null;
		}
	}
}
