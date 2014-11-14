package com.boardgamegeek.util;

import java.util.Calendar;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;

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

	public static String[] getStringArray(Cursor cursor, int columnIndex) {
		String[] array = new String[cursor.getCount()];
		cursor.moveToPosition(-1);
		int i = 0;
		while (cursor.moveToNext()) {
			array[i++] = cursor.getString(columnIndex);
		}
		return array;
	}

	public static String getFormattedDate(Cursor cursor, Context context, int columnIndex) {
		return getFormattedDate(cursor, context, columnIndex, DateUtils.FORMAT_SHOW_DATE);
	}

	public static String getFormattedDateAbbreviated(Cursor cursor, Context context, int columnIndex) {
		return getFormattedDate(cursor, context, columnIndex, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
	}

	private static String getFormattedDate(Cursor cursor, Context context, int columnIndex, int flags) {
		String date = cursor.getString(columnIndex);
		if (!TextUtils.isEmpty(date)) {
			int year = Integer.parseInt(date.substring(0, 4));
			int month = Integer.parseInt(date.substring(5, 7)) - 1;
			int day = Integer.parseInt(date.substring(8, 10));
			Calendar c = Calendar.getInstance();
			c.set(year, month, day);
			return DateUtils.formatDateTime(context, c.getTimeInMillis(), flags);
		}
		return null;
	}
}
