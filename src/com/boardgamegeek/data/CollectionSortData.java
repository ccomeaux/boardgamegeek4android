package com.boardgamegeek.data;

import android.database.Cursor;

public class CollectionSortData {
	protected String mOrderByClause;
	protected int mDescription;

	public CollectionSortData() {
	}

	public int getDescriptionId() {
		return mDescription;
	}

	public String[] getColumns() {
		return null;
	}

	public String getDisplayInfo(Cursor cursor) {
		return null;
	}

	public String getOrderByClause() {
		return mOrderByClause;
	}

	public String getScrollText(Cursor cursor) {
		return null;
	}

	public int getType() {
		return CollectionSortDataFactory.TYPE_UNKNOWN;
	}

	protected Double getDouble(Cursor cursor, String columnName) {
		int index = cursor.getColumnIndex(columnName);
		if (index != -1) {
			return cursor.getDouble(index);
		}
		return null;
	}

	protected String getFirstChar(Cursor cursor, String columnName) {
		int index = cursor.getColumnIndex(columnName);
		if (index != -1) {
			char firstLetter = cursor.getString(index).toUpperCase().charAt(0);
			return String.valueOf(firstLetter);
		}
		return null;
	}
}
