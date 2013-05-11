package com.boardgamegeek.data;

import java.text.DecimalFormat;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public abstract class CollectionSortData {
	protected Context mContext;
	protected String mOrderByClause;
	protected int mDescriptionId;
	private DecimalFormat mDoubleFormat = new DecimalFormat("#.0");

	public CollectionSortData(Context context) {
		mContext = context;
	}

	public int getDescriptionId() {
		return mDescriptionId;
	}

	public String getDescription() {
		return String.format(mContext.getString(R.string.sort_description), mContext.getString(mDescriptionId));
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

	protected int getInt(Cursor cursor, String columnName) {
		return getInt(cursor, columnName, 0);
	}

	protected int getInt(Cursor cursor, String columnName, int defaultValue) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1) {
			return defaultValue;
		}
		return cursor.getInt(index);
	}

	protected String getIntAsString(Cursor cursor, String columnName, String defaultValue) {
		return getIntAsString(cursor, columnName, defaultValue, false);
	}

	protected String getIntAsString(Cursor cursor, String columnName, String defaultValue, boolean treatZeroAsNull) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1) {
			return defaultValue;
		}

		int value = cursor.getInt(index);
		if (treatZeroAsNull && value == 0) {
			return defaultValue;
		}

		return String.valueOf(value);
	}

	protected String getDoubleAsString(Cursor cursor, String columnName, String defaultValue) {
		return getIntAsString(cursor, columnName, defaultValue, false);
	}

	protected String getDoubleAsString(Cursor cursor, String columnName, String defaultValue, boolean treatZeroAsNull,
		DecimalFormat format) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1) {
			return defaultValue;
		}

		double value = cursor.getDouble(index);
		if (treatZeroAsNull && value == 0.0) {
			return defaultValue;
		}

		if (format == null) {
			return mDoubleFormat.format(value);
		} else {
			return format.format(value);
		}
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
			char firstLetter = cursor.getString(index).toUpperCase(Locale.getDefault()).charAt(0);
			return String.valueOf(firstLetter);
		}
		return null;
	}
}
