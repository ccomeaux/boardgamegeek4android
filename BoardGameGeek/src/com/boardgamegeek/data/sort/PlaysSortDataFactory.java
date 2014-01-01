package com.boardgamegeek.data.sort;

import android.content.Context;

public class PlaysSortDataFactory {
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_PLAY_DATE = 1;
	public static final int TYPE_PLAY_LOCATION = 2;
	public static final int TYPE_PLAY_GAME = 3;
	public static final int TYPE_PLAY_LENGTH = 4;
	public static final int TYPE_DEFAULT = TYPE_PLAY_DATE;

	public static SortData create(int type, Context context) {
		switch (type) {
			case TYPE_PLAY_DATE:
				return new PlaysDateSortData(context);
			case TYPE_PLAY_LOCATION:
				return new PlaysLocationSortData(context);
			case TYPE_PLAY_GAME:
				return new PlaysGameSortData(context);
			case TYPE_PLAY_LENGTH:
				return new PlaysLengthSortData(context);
			case TYPE_UNKNOWN:
			default:
				return null;
		}
	}
}
