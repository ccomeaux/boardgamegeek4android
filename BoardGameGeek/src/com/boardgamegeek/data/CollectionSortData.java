package com.boardgamegeek.data;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Collection;

public abstract class CollectionSortData {
	protected Context mContext;
	protected String mOrderByClause;
	protected int mDescriptionId;

	public CollectionSortData(Context context) {
		mContext = context;
	}

	public int getDescriptionId() {
		return mDescriptionId;
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

	protected String getClause(String columnName, boolean isDescending) {
		return columnName + (isDescending ? " DESC, " : " ASC, ") + Collection.DEFAULT_SORT;
	}

	protected String getIntAsString(Cursor cursor, String columnName, String defaultValue) {
		int index = cursor.getColumnIndex(columnName);
		if (index != -1) {
			return String.valueOf(cursor.getInt(index));
		}
		return defaultValue;
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
