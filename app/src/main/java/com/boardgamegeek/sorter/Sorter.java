package com.boardgamegeek.sorter;

import java.text.DecimalFormat;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.boardgamegeek.R;

public abstract class Sorter {
	protected Context mContext;
	protected String mOrderByClause;
	protected int mDescriptionId;
	protected int mSubDescriptionId;
	private DecimalFormat mDoubleFormat = new DecimalFormat("#.0");

	public Sorter(Context context) {
		mContext = context;
	}

	/**
	 * Gets the description to display in the UI when this sort is applied. Subclasses should set mDescriptionId and
	 * mSubDescriptionId to control this value.
	 */
	public String getDescription() {
		String decription = String.format(mContext.getString(R.string.sort_description),
			mContext.getString(mDescriptionId));
		if (mSubDescriptionId > 0) {
			decription += " - " + mContext.getString(mSubDescriptionId);
		}
		return decription;
	}

	/**
	 * Gets the sort order clause to use in the query.
	 */
	public String getOrderByClause() {
		return mOrderByClause;
	}

	/**
	 * Get the names of the columns to add to the select projection.
	 */
	public String[] getColumns() {
		return null;
	}

	/**
	 * Get the text to display in the section header.
	 */
	protected String getHeaderText(Cursor cursor) {
		return "";
	}

	/**
	 * Get the text to display in the section header.
	 */
	public String getHeaderText(Cursor cursor, int position) {
		if (cursor == null || position < 0) {
			return "";
		}
		int pos = cursor.getPosition();
		cursor.moveToPosition(position);
		String text = getHeaderText(cursor);
		cursor.moveToPosition(pos);
		return text;
	}

	protected long getHeaderId(Cursor cursor) {
		return getHeaderText(cursor).hashCode();
	}

	public long getHeaderId(Cursor cursor, int position) {
		if (cursor == null || position < 0) {
			return 0;
		}
		int pos = cursor.getPosition();
		cursor.moveToPosition(position);
		long id = getHeaderId(cursor);
		cursor.moveToPosition(pos);
		return id;
	}

	/**
	 * Gets the text to display on each row.
	 */
	public String getDisplayInfo(Cursor cursor) {
		return getHeaderText(cursor);
	}

	/**
	 * Get the unique type
	 */
	public int getType() {
		return SorterFactory.TYPE_UNKNOWN;
	}

	protected String getClause(String columnName, boolean isDescending) {
		return columnName + (isDescending ? " DESC, " : " ASC, ") + getDefaultSort();
	}

	protected abstract String getDefaultSort();

	protected long getLong(Cursor cursor, String columnName) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1) {
			return 0;
		}
		return cursor.getLong(index);
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

	protected Double getDouble(Cursor cursor, String columnName) {
		int index = cursor.getColumnIndex(columnName);
		if (index != -1) {
			return cursor.getDouble(index);
		}
		return null;
	}

	protected String getDoubleAsString(Cursor cursor, String columnName, String defaultValue) {
		return getDoubleAsString(cursor, columnName, defaultValue, false, mDoubleFormat);
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

	@SuppressLint("DefaultLocale")
	protected String getFirstChar(Cursor cursor, String columnName) {
		int index = cursor.getColumnIndex(columnName);
		if (index != -1) {
			char firstLetter = cursor.getString(index).toUpperCase(Locale.getDefault()).charAt(0);
			return String.valueOf(firstLetter);
		}
		return null;
	}

	protected String getString(Cursor cursor, String columnName) {
		return getString(cursor, columnName, null);
	}

	protected String getString(Cursor cursor, String columnName, String defaultValue) {
		int index = cursor.getColumnIndex(columnName);
		if (index != -1) {
			String s = cursor.getString(index);
			if (TextUtils.isEmpty(s)) {
				return defaultValue;
			}
			return s;
		}
		return defaultValue;
	}
}