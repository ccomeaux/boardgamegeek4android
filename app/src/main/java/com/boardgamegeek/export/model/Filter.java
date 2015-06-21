package com.boardgamegeek.export.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.google.gson.annotations.Expose;

public class Filter {
	public static String[] PROJECTION = new String[] {
		CollectionViewFilters._ID,
		CollectionViewFilters.TYPE,
		CollectionViewFilters.DATA
	};

	private static final int TYPE = 1;
	private static final int DATA = 2;

	@Expose private int type;
	@Expose private String data;

	public int getType() {
		return type;
	}

	public String getData() {
		return data;
	}

	public static Filter fromCursor(Cursor cursor) {
		Filter filter = new Filter();
		filter.type = cursor.getInt(TYPE);
		filter.data = cursor.getString(DATA);
		return filter;
	}
}
