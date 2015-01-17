package com.boardgamegeek.sorter;

import android.content.Context;

public class LocationsSorterFactory {
	public static final int TYPE_NAME = 1;
	public static final int TYPE_QUANTITY = 2;
	public static final int TYPE_DEFAULT = TYPE_NAME;

	public static LocationsSorter create(int type, Context context) {
		switch (type) {
			case TYPE_QUANTITY:
				return new LocationsQuantitySorter(context);
			case TYPE_NAME:
			default:
				return new LocationsNameSorter(context);
		}
	}
}
