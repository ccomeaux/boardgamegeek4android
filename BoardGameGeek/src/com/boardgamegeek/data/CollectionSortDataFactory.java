package com.boardgamegeek.data;

public class CollectionSortDataFactory {
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

	public static CollectionSortData create(int type) {
		switch (type) {
			case TYPE_COLLECTION_NAME:
				return new CollectionNameSortData();
			case TYPE_GEEK_RATING:
				return new GeekRatingSortData();
			case TYPE_YEAR_PUBLISHED_ASC:
				return new YearPublishedAscendingSortData();
			case TYPE_YEAR_PUBLISHED_DESC:
				return new YearPublishedDescendingSortData();
			case TYPE_PLAY_COUNT_ASC:
			case TYPE_PLAY_COUNT_DESC:
			case TYPE_PLAY_TIME_ASC:
			case TYPE_PLAY_TIME_DESC:
			case TYPE_AGE_ASC:
			case TYPE_AGE_DESC:
			case TYPE_AVERAGE_WEIGHT_ASC:
			case TYPE_AVERAGE_WEIGHT_DESC:
				// TODO
			case TYPE_UNKNOWN:
			default:
				return null;
		}
	}
}
