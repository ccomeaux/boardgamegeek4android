package com.boardgamegeek.util;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

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

	/*
	 * Use the content resolver to get a list of integers from the specified column at the URI
	 */
	public static List<Integer> queryInts(ContentResolver resolver, Uri uri, String columnName) {
		List<Integer> list = new ArrayList<Integer>();
		Cursor cursor = resolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			while (cursor.moveToNext()) {
				list.add(cursor.getInt(0));
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return list;
	}
}
