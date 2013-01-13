package com.boardgamegeek.database;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract;

public class ResolverUtils {
	private static final String TAG = makeLogTag(ResolverUtils.class);

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
		return getCount(resolver, uri) == 1;
	}

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
		Cursor cursor = resolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			int count = cursor.getCount();
			if (count != 1) {
				return 0;
			}
			cursor.moveToFirst();
			return cursor.getInt(0);
		} finally {
			closeCursor(cursor);
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
			closeCursor(cursor);
		}
		return list;
	}

	/*
	 * Use the content resolver to get a list of strings from the specified column at the URI
	 */
	public static List<String> queryStrings(ContentResolver resolver, Uri uri, String columnName) {
		return queryStrings(resolver, uri, columnName, null, null);
	}

	/*
	 * Use the content resolver to get a list of strings from the specified column at the URI
	 */
	public static List<String> queryStrings(ContentResolver resolver, Uri uri, String columnName, String selection,
		String[] selectionArgs) {
		return queryStrings(resolver, uri, columnName, selection, selectionArgs, null);
	}

	/*
	 * Use the content resolver to get a list of strings from the specified column at the URI
	 */
	public static List<String> queryStrings(ContentResolver resolver, Uri uri, String columnName, String selection,
		String[] selectionArgs, String sortOrder) {
		List<String> list = new ArrayList<String>();
		Cursor cursor = resolver.query(uri, new String[] { columnName }, selection, selectionArgs, sortOrder);
		try {
			while (cursor.moveToNext()) {
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
	public static String queryString(ContentResolver resolver, Uri uri, String columnName) {
		String value;
		Cursor cursor = resolver.query(uri, new String[] { columnName }, null, null, null);
		try {
			int count = cursor.getCount();
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

	/*
	 * Loads a bitmap from the URI. Returns null if the URI is invalid or the bitmap can't be created.
	 */
	public static Bitmap getBitmapFromContentProvider(ContentResolver resolver, Uri uri) {
		InputStream stream = null;
		try {
			stream = resolver.openInputStream(uri);
		} catch (FileNotFoundException e) {
			 LOGD(TAG, "Couldn't find drawable: " + uri, e);
		}
		if (stream != null) {
			Bitmap bitmap = BitmapFactory.decodeStream(stream);
			closeStream(stream);
			return bitmap;
		}
		return null;
	}

	/*
	 * Saves a bitmap at the URI.
	 */
	public static void putBitmapInContentProvider(ContentResolver resolver, Uri uri, Bitmap bitmap) {
		OutputStream stream = null;
		try {
			stream = resolver.openOutputStream(uri);
		} catch (FileNotFoundException e) {
			LOGE(TAG, "Couldn't find drawable: " + uri, e);
		}
		if (stream != null) {
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			closeStream(stream);
		}
	}

	private static void closeCursor(Cursor cursor) {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	private static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				LOGE(TAG, "Could not close stream", e);
			}
		}
	}
}