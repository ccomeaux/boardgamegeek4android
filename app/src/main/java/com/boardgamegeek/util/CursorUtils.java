package com.boardgamegeek.util;

import android.database.Cursor;

/**
 * Static methods for getting data out of a cursor.
 */
public class CursorUtils {
	private CursorUtils() {
	}

	/**
	 * Gets a boolean from the specified column on the current row of the cursor. Returns false if the column doesn't exist.
	 */
	public static boolean getBoolean(Cursor cursor, String columnName) {
		return getBoolean(cursor, columnName, false);
	}

	/**
	 * Gets a boolean from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist.
	 */
	public static boolean getBoolean(Cursor cursor, String columnName, boolean defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		return getBoolean(cursor, idx, defaultValue);
	}

	public static boolean getBoolean(Cursor cursor, int idx) {
		return getBoolean(cursor, idx, false);
	}

	public static boolean getBoolean(Cursor cursor, int idx, boolean defaultValue) {
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getInt(idx) != 0;
		}
	}

	/**
	 * Gets an integer from the specified column on the current row of the cursor. Returns 0 if the column doesn't exist.
	 */
	public static int getInt(Cursor cursor, String columnName) {
		return getInt(cursor, columnName, 0);
	}

	/**
	 * Gets an integer from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist.
	 */
	public static int getInt(Cursor cursor, String columnName, int defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getInt(idx);
		}
	}

	/**
	 * Gets a long from the specified column on the current row of the cursor. Returns 0 if the column doesn't exist.
	 */
	public static long getLong(Cursor cursor, String columnName) {
		return getLong(cursor, columnName, 0);
	}

	/**
	 * Gets a long from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist.
	 */
	public static long getLong(Cursor cursor, String columnName, long defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getLong(idx);
		}
	}

	/**
	 * Gets a double from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist.
	 */
	public static double getDouble(Cursor cursor, String columnName, double defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getDouble(idx);
		}
	}

	/**
	 * Gets a string from the specified column on the current row of the cursor. Returns an empty string if the column doesn't exist or is null.
	 */
	public static String getString(Cursor cursor, String columnName) {
		return getString(cursor, columnName, "");
	}

	/**
	 * Gets a string from the specified column on the current row of the cursor. Returns an empty string if the column doesn't exist or is null.
	 */
	public static String getString(Cursor cursor, int columnIndex) {
		return getString(cursor, columnIndex, "");
	}

	/**
	 * Gets a string from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist or is null.
	 */
	public static String getString(Cursor cursor, String columnName, String defaultValue) {
		return getString(cursor, cursor.getColumnIndex(columnName), defaultValue);
	}

	/**
	 * Gets a string from the specified column on the current row of the cursor. Returns defaultValue if the column doesn't exist or is null.
	 */
	public static String getString(Cursor cursor, int columnIndex, String defaultValue) {
		if (columnIndex == -1) {
			return defaultValue;
		} else {
			String value = cursor.getString(columnIndex);
			if (value == null) {
				return defaultValue;
			}
			return value;
		}
	}

	/**
	 * Gets string array from the cursor based on the columnIndex. Returns with the cursor at the original position.
	 */
	public static String[] getStringArray(Cursor cursor, int columnIndex) {
		String[] array = new String[cursor.getCount()];
		int position = cursor.getPosition();
		try {
			cursor.moveToPosition(-1);
			int i = 0;
			while (cursor.moveToNext()) {
				array[i++] = cursor.getString(columnIndex);
			}
		} finally {
			cursor.moveToPosition(position);
		}
		return array;
	}
}
