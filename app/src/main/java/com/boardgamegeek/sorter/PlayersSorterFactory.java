package com.boardgamegeek.sorter;

import android.content.Context;
import android.support.annotation.NonNull;

public class PlayersSorterFactory {
	public static final int TYPE_NAME = 1;
	public static final int TYPE_QUANTITY = 2;
	public static final int TYPE_DEFAULT = TYPE_NAME;

	@NonNull
	public static PlayersSorter create(@NonNull Context context, int type) {
		switch (type) {
			case TYPE_QUANTITY:
				return new PlayersQuantitySorter(context);
			case TYPE_NAME:
			default:
				return new PlayersNameSorter(context);
		}
	}
}
