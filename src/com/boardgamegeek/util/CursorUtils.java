package com.boardgamegeek.util;

import android.database.Cursor;

public class CursorUtils {

	public static boolean getBoolean(Cursor cursor, String columnName) {
		return getBoolean(cursor, columnName, false);
	}

	public static boolean getBoolean(Cursor cursor, String columnName, boolean defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getInt(idx) != 0;
		}
	}

	public static int getInt(Cursor cursor, String columnName) {
		return getInt(cursor, columnName, 0);
	}

	public static int getInt(Cursor cursor, String columnName, int defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getInt(idx);
		}
	}

	public static long getLong(Cursor cursor, String columnName) {
		return getLong(cursor, columnName, 0);
	}

	public static long getLong(Cursor cursor, String columnName, long defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getLong(idx);
		}
	}

	public static double getDouble(Cursor cursor, String columnName) {
		return getDouble(cursor, columnName, 0.0);
	}

	public static double getDouble(Cursor cursor, String columnName, double defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getDouble(idx);
		}
	}

	public static String getString(Cursor cursor, String columnName) {
		return getString(cursor, columnName, "");
	}

	public static String getString(Cursor cursor, String columnName, String defaultValue) {
		int idx = cursor.getColumnIndex(columnName);
		if (idx == -1) {
			return defaultValue;
		} else {
			return cursor.getString(idx);
		}
	}
}
