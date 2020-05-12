package com.boardgamegeek.util;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import timber.log.Timber;

public class ResolverUtils {
	private ResolverUtils() {
	}

	public static ContentProviderResult[] applyBatch(Context context, ArrayList<ContentProviderOperation> batch) {
		return applyBatch(context, batch, null);
	}

	public static ContentProviderResult[] applyBatch(Context context, ArrayList<ContentProviderOperation> batch, String debugMessage) {
		if (batch != null && batch.size() > 0) {
			ContentResolver resolver = context.getContentResolver();
			try {
				return resolver.applyBatch(BggContract.CONTENT_AUTHORITY, batch);
			} catch (OperationApplicationException | RemoteException e) {
				String m = "Applying batch: " + debugMessage;
				Timber.e(e, m);
				throw new RuntimeException(m, e);
			}
		}
		return new ContentProviderResult[] {};
	}

	/*
	 * Determines if the URI exists in the resolver
	 */
	public static boolean rowExists(ContentResolver resolver, Uri uri) {
		return getCount(resolver, uri) > 0;
	}

	/**
	 * Get the number of rows at this URI.
	 */
	public static int getCount(ContentResolver resolver, Uri uri) {
		Cursor cursor = resolver.query(uri, new String[] { BaseColumns._ID }, null, null, null);
		if (cursor != null) {
			try {
				return cursor.getCount();
			} finally {
				closeCursor(cursor);
			}
		}
		return 0;
	}

	/*
	 * Use the content resolver to get an integer from the specified column at the URI. Returns 0 if there's not exactly
	 * one row at the URI.
	 */
	public static int queryInt(ContentResolver resolver, Uri uri, String columnName) {
		return queryInt(resolver, uri, columnName, 0);
	}

	/*
	 * Use the content resolver to get an integer from the specified column at the URI. Returns defaultValue if there's
	 * not exactly one row at the URI.
	 */
	public static int queryInt(ContentResolver resolver, Uri uri, String columnName, int defaultValue) {
		return queryInt(resolver, uri, columnName, defaultValue, null, null);
	}

	/*
	 * Use the content resolver to get an integer from the specified column at the URI with the selection applied.
	 * Returns defaultValue if there's not exactly one row at the URI.
	 */
	public static int queryInt(ContentResolver resolver, Uri uri, String columnName, int defaultValue, String selection, String[] selectionArgs) {
		Cursor cursor = resolver.query(uri, new String[] { columnName }, selection, selectionArgs, null);
		try {
			int count = cursor != null ? cursor.getCount() : 0;
			if (count != 1) {
				return defaultValue;
			}
			cursor.moveToFirst();
			return cursor.getInt(0);
		} finally {
			closeCursor(cursor);
		}
	}

	/*
	 * Use the content resolver to get a long from the specified column at the URI with the selection applied. Returns
	 * defaultValue if there's not exactly one row at the URI.
	 */
	public static long queryLong(ContentResolver resolver, Uri uri, String columnName, int defaultValue,
								 String selection, String[] selectionArgs) {
		Cursor cursor = resolver.query(uri, new String[] { columnName }, selection, selectionArgs, null);
		try {
			int count = cursor != null ? cursor.getCount() : 0;
			if (count != 1) {
				return defaultValue;
			}
			cursor.moveToFirst();
			return cursor.getLong(0);
		} finally {
			closeCursor(cursor);
		}
	}

	/*
	 * Use the content resolver to get a list of longs from the specified column at the URI.
	 */
	public static List<Long> queryLongs(ContentResolver resolver, Uri uri, String columnName, String selection, String[] selectionArgs) {
		return queryLongs(resolver, uri, columnName, selection, selectionArgs, null);
	}

	/*
	 * Use the content resolver to get a list of longs from the specified column at the URI.
	 */
	public static List<Long> queryLongs(ContentResolver resolver, Uri uri, String columnName, String selection, String[] selectionArgs, String sortOrder) {
		List<Long> list = new ArrayList<>();
		Cursor cursor = resolver.query(uri, new String[] { columnName }, selection, selectionArgs, sortOrder);
		try {
			while (cursor != null && cursor.moveToNext()) {
				list.add(cursor.getLong(0));
			}
		} finally {
			closeCursor(cursor);
		}
		return list;
	}

	/*
	 * Use the content resolver to get a list of strings from the specified column at the URI.
	 */
	public static List<String> queryStrings(ContentResolver resolver, Uri uri, String columnName) {
		return queryStrings(resolver, uri, columnName, null, null);
	}

	/*
	 * Use the content resolver to get a list of strings from the specified column at the URI.
	 */
	public static List<String> queryStrings(ContentResolver resolver, Uri uri, String columnName, String selection, String[] selectionArgs) {
		return queryStrings(resolver, uri, columnName, selection, selectionArgs, null);
	}

	/*
	 * Use the content resolver to get a list of strings from the specified column at the URI.
	 */
	public static List<String> queryStrings(ContentResolver resolver, Uri uri, String columnName, String selection, String[] selectionArgs, String sortOrder) {
		List<String> list = new ArrayList<>();
		Cursor cursor = resolver.query(uri, new String[] { columnName }, selection, selectionArgs, sortOrder);
		try {
			while (cursor != null && cursor.moveToNext()) {
				list.add(cursor.getString(0));
			}
		} finally {
			closeCursor(cursor);
		}
		return list;
	}

	/*
	 * Use the content resolver to get a string from the specified column at the URI. Returns null if there's not
	 * exactly one row at the URI.
	 */
	@Nullable
	public static String queryString(ContentResolver resolver, Uri uri, String columnName) {
		String value;
		Cursor cursor = resolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			int count = cursor != null ? cursor.getCount() : 0;
			if (count != 1) {
				return null;
			}
			cursor.moveToFirst();
			value = cursor.getString(0);
		} finally {
			closeCursor(cursor);
		}
		return value;
	}

	private static void closeCursor(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}
}