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

import androidx.annotation.NonNull;
import timber.log.Timber;

public class ResolverUtils {
	private ResolverUtils() {
	}

	public static @NonNull ContentProviderResult[] applyBatch(Context context, ArrayList<ContentProviderOperation> batch, String debugMessage) {
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
	 * Use the content resolver to get a list of strings from the specified column at the URI.
	 */
	public static List<String> queryStrings(ContentResolver resolver, Uri uri, String columnName) {
		List<String> list = new ArrayList<>();
		Cursor cursor = resolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			while (cursor != null && cursor.moveToNext()) {
				list.add(cursor.getString(0));
			}
		} finally {
			closeCursor(cursor);
		}
		return list;
	}

	private static void closeCursor(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}
}
