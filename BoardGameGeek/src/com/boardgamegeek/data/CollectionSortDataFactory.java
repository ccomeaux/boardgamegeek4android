package com.boardgamegeek.data;

public class CollectionSortDataFactory {
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_COLLECTION_NAME = 1;
	public static final int TYPE_GEEK_RATING = 2;

	public static CollectionSortData create(int type) {
		switch (type) {
			case TYPE_COLLECTION_NAME:
				return new CollectionNameSortData();
			case TYPE_GEEK_RATING:
				return new GeekRatingSortData();
			case TYPE_UNKNOWN:
			default:
				return null;
		}
	}
}
