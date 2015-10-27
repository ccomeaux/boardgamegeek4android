package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

public class PlaysSorterFactory {
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_PLAY_DATE = 1;
	public static final int TYPE_PLAY_LOCATION = 2;
	public static final int TYPE_PLAY_GAME = 3;
	public static final int TYPE_PLAY_LENGTH = 4;
	public static final int TYPE_DEFAULT = TYPE_PLAY_DATE;

	public static PlaysSorter create(@NonNull Context context, int type) {
		switch (type) {
			case TYPE_PLAY_DATE:
				return new PlaysDateSorter(context);
			case TYPE_PLAY_LOCATION:
				return new PlaysLocationSorter(context);
			case TYPE_PLAY_GAME:
				return new PlaysGameSorter(context);
			case TYPE_PLAY_LENGTH:
				return new PlaysLengthSorter(context);
			case TYPE_UNKNOWN:
			default:
				return null;
		}
	}
}
