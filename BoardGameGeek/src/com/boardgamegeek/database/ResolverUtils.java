package com.boardgamegeek.database;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract;

public class ResolverUtils {
	public static void applyBatch(ContentResolver resolver, ArrayList<ContentProviderOperation> batch) {
		if (batch.size() > 0) {
			try {
				resolver.applyBatch(BggContract.CONTENT_AUTHORITY, batch);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			} catch (OperationApplicationException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/*
	 * Determines if the URI exists in the resolver
	 */
	public static boolean rowExists(ContentResolver resolver, Uri uri) {
		Cursor cursor = resolver.query(uri, new String[] { BaseColumns._ID }, null, null, null);
		try {
			return (cursor.getCount() == 0);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	/*
	 * Use the content resolver to get a list of integers from the specified column at the URI
	 */
	public static List<Integer> queryInts(ContentResolver resolver, Uri uri, String columnName) {
		return queryInts(resolver, uri, columnName, null, null);
	}

	/*
	 * Use the content resolver to get a list of integers from the specified column at the URI
	 */
	public static List<Integer> queryInts(ContentResolver resolver, Uri uri, String columnName, String selection,
			String[] selectionArgs) {
		List<Integer> list = new ArrayList<Integer>();
		Cursor cursor = resolver.query(uri, new String[] { columnName }, selection, selectionArgs, null);
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

	/*
	 * Use the content resolver to get a list of string from the specified column at the URI
	 */
	public static List<String> queryStrings(ContentResolver resolver, Uri uri, String columnName) {
		List<String> list = new ArrayList<String>();
		Cursor cursor = resolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			while (cursor.moveToNext()) {
				list.add(cursor.getString(0));
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return list;
	}
}
