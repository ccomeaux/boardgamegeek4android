package com.boardgamegeek.sorter;

import android.content.Context;

public class PlayersSorterFactory {
	public static final int TYPE_NAME = 1;
	public static final int TYPE_QUANTITY = 2;
	public static final int TYPE_DEFAULT = TYPE_NAME;

	public static PlayersSorter create(int type, Context context) {
		switch (type) {
			case TYPE_QUANTITY:
				return new PlayersQuantitySorter(context);
			case TYPE_NAME:
			default:
				return new PlayersNameSorter(context);
		}
	}
}
