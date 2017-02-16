package com.boardgamegeek.sorter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import java.text.DecimalFormat;
import java.util.Locale;

public abstract class Sorter {
	@NonNull protected final Context context;
	protected String orderByClause;
	private final DecimalFormat doubleFormat = new DecimalFormat("#.0");

	public Sorter(@NonNull Context context) {
		this.context = context;
	}

	@StringRes
	protected abstract int getDescriptionId();

	/**
	 * Gets the description to display in the UI when this sort is applied. Subclasses should set descriptionId
	 * to control this value.
	 */
	public String getDescription() {
		return context.getString(getDescriptionId());
	}

	/**
	 * Gets the sort order clause to use in the query.
	 */
	public String getOrderByClause() {
		return orderByClause;
	}

	/**
	 * Get the names of the columns to add to the select projection.
	 */
	@Nullable
	public String[] getColumns() {
		return null;
	}

	/**
	 * Get the text to display in the section header.
	 */
	public String getHeaderText(@Nullable Cursor cursor, int position) {
		String text = "";
		if (cursor == null || position < 0) {
			return text;
		}
		int pos = cursor.getPosition();
		if (cursor.moveToPosition(position)) {
			text = getHeaderText(cursor);
		}
		cursor.moveToPosition(pos);
		return text;
	}

	/**
	 * Get the text to display in the section header.
	 */
	protected String getHeaderText(Cursor cursor) {
		return "";
	}

	public long getHeaderId(@Nullable Cursor cursor, int position) {
		long id = 0;
		if (cursor == null || position < 0) {
			return id;
		}
		int pos = cursor.getPosition();
		if (cursor.moveToPosition(position)) {
			id = getHeaderId(cursor);
		}
		cursor.moveToPosition(pos);
		return id;
	}

	protected long getHeaderId(Cursor cursor) {
		String headerText = getHeaderText(cursor);
		if (headerText == null) {
			headerText = "";
		}
		return headerText.hashCode();
	}

	/**
	 * Get the unique type
	 */
	public abstract int getType();

	protected String getClause(String columnName, boolean isDescending) {
		if (TextUtils.isEmpty(columnName)) {
			return getDefaultSort();
		}
		return columnName + (isDescending ? " DESC, " : " ASC, ") + getDefaultSort();
	}

	protected abstract String getDefaultSort();

	protected long getLong(@NonNull Cursor cursor, String columnName) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1 || index >= cursor.getColumnCount()) {
			return 0;
		}
		return cursor.getLong(index);
	}

	protected int getInt(@NonNull Cursor cursor, String columnName) {
		return getInt(cursor, columnName, 0);
	}

	protected int getInt(@NonNull Cursor cursor, String columnName, int defaultValue) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1 || index >= cursor.getColumnCount()) {
			return defaultValue;
		}
		return cursor.getInt(index);
	}

	protected String getIntAsString(@NonNull Cursor cursor, String columnName, String defaultValue) {
		return getIntAsString(cursor, columnName, defaultValue, false);
	}

	protected String getIntAsString(@NonNull Cursor cursor, String columnName, String defaultValue, boolean treatZeroAsNull) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1 || index >= cursor.getColumnCount()) {
			return defaultValue;
		}

		int value = cursor.getInt(index);
		if (treatZeroAsNull && value == 0) {
			return defaultValue;
		}

		return String.valueOf(value);
	}

	protected String getDoubleAsString(@NonNull Cursor cursor, String columnName, String defaultValue) {
		return getDoubleAsString(cursor, columnName, defaultValue, false, doubleFormat);
	}

	protected String getDoubleAsString(@NonNull Cursor cursor, String columnName, String defaultValue, boolean treatZeroAsNull, @Nullable DecimalFormat format) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1 || index >= cursor.getColumnCount()) {
			return defaultValue;
		}

		double value = cursor.getDouble(index);
		if (treatZeroAsNull && value == 0.0) {
			return defaultValue;
		}

		if (format == null) {
			return doubleFormat.format(value);
		} else {
			return format.format(value);
		}
	}

	@NonNull
	@SuppressLint("DefaultLocale")
	protected String getFirstChar(@NonNull Cursor cursor, String columnName) {
		return getString(cursor, columnName, "-").substring(0, 1).toUpperCase(Locale.getDefault());
	}

	protected String getString(@NonNull Cursor cursor, String columnName) {
		return getString(cursor, columnName, null);
	}

	protected String getString(@NonNull Cursor cursor, String columnName, String defaultValue) {
		int index = cursor.getColumnIndex(columnName);
		if (index == -1 || index >= cursor.getColumnCount()) {
			return defaultValue;
		}
		String s = cursor.getString(index);
		if (TextUtils.isEmpty(s)) {
			return defaultValue;
		}
		return s;
	}
}