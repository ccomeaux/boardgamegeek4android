package com.boardgamegeek.export.model;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class CollectionView {
	public static String[] PROJECTION = new String[] {
		CollectionViews._ID,
		CollectionViews.NAME,
		CollectionViews.SORT_TYPE,
		CollectionViews.STARRED
	};

	private static final int _ID = 0;
	private static final int NAME = 1;
	private static final int SORT_TYPE = 2;
	private static final int STARRED = 3;

	private int id;
	@Expose private String name;
	@Expose private int sortType;
	@Expose private boolean starred;
	@Expose private List<Filter> filters;

	public String getName() {
		return name;
	}

	public int getSortType() {
		return sortType;
	}

	public boolean isStarred() {
		return starred;
	}

	public List<Filter> getFilters() {
		return filters;
	}

	public static CollectionView fromCursor(Cursor cursor) {
		CollectionView cv = new CollectionView();
		cv.id = cursor.getInt(_ID);
		cv.name = cursor.getString(NAME);
		cv.sortType = cursor.getInt(SORT_TYPE);
		cv.starred = cursor.getInt(STARRED) == 1;
		return cv;
	}

	public void addFilters(Context context) {
		filters = new ArrayList<>();

		final Cursor cursor = context.getContentResolver().query(
			CollectionViews.buildViewFilterUri(id),
			Filter.PROJECTION,
			null, null, null);

		if (cursor == null) {
			return;
		}

		try {
			while (cursor.moveToNext()) {
				Filter filter = Filter.fromCursor(cursor);
				filters.add(filter);
			}
		} finally {
			cursor.close();
		}
	}
}
