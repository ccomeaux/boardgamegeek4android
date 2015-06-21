package com.boardgamegeek.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.SelectionBuilder;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseProvider {

	protected abstract String getPath();

	protected String addIdToPath(String path) {
		return path + "/#";
	}

	protected String addWildCardToPath(String path) {
		return path + "/*";
	}

	protected Cursor query(ContentResolver resolver, SQLiteDatabase db, Uri uri, String[] projection, String selection,
						   String[] selectionArgs, String sortOrder) {
		SelectionBuilder builder = buildExpandedSelection(uri).where(selection, selectionArgs);
		String fragment = uri.getQueryParameter(BggContract.PARAM_LIMIT);
		if (fragment != null) {
			builder.limit(fragment);
		}
		return builder.query(db, projection, getSortOrder(sortOrder));
	}

	protected String getSortOrder(String sortOrder) {
		if (TextUtils.isEmpty(sortOrder)) {
			return getDefaultSortOrder();
		} else {
			return sortOrder;
		}
	}

	protected String getDefaultSortOrder() {
		return null;
	}

	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return buildSimpleSelection(uri);
	}

	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		throw new UnsupportedOperationException("Unknown uri: " + uri);
	}

	protected String getType(Uri uri) {
		throw new UnsupportedOperationException("Unknown uri getting type: " + uri);
	}

	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		throw new UnsupportedOperationException("Unknown uri inserting: " + uri);
	}

	protected int update(Context context, SQLiteDatabase db, Uri uri, ContentValues values, String selection,
						 String[] selectionArgs) {
		int rowCount = buildSimpleSelection(uri).where(selection, selectionArgs).update(db, values);
		notifyChange(context, uri);
		return rowCount;
	}

	protected int delete(Context context, SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs) {
		int rowCount = buildSimpleSelection(uri).where(selection, selectionArgs).delete(db);
		notifyChange(context, uri);
		return rowCount;
	}

	protected void notifyChange(Context context, Uri uri) {
		context.getContentResolver().notifyChange(uri, null);
	}

	protected ParcelFileDescriptor openFile(Context context, Uri uri, String mode) throws FileNotFoundException {
		throw new FileNotFoundException("Unknown uri opening file: " + uri);
	}

	protected int queryInt(SQLiteDatabase db, SelectionBuilder builder, String columnName) {
		int value = 0;
		Cursor cursor = builder.query(db, new String[] { columnName }, null);
		try {
			if (cursor.getCount() != 1) {
				return value;
			}
			if (cursor.moveToFirst()) {
				value = cursor.getInt(0);
			}
		} finally {
			closeCursor(cursor);
		}
		return value;
	}

	protected List<String> getList(final SQLiteDatabase db, final SelectionBuilder builder, String columnName) {
		List<String> list = new ArrayList<>();
		Cursor cursor = builder.query(db, new String[] { columnName }, null);
		try {
			if (cursor.moveToNext()) {
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

	protected void notifyException(Context context, SQLException e) {
		if (PreferencesUtils.getSyncShowNotifications(context)) {
			NotificationCompat.Builder builder = NotificationUtils
				.createNotificationBuilder(context, R.string.title_error)
				.setContentText(e.getLocalizedMessage())
				.setCategory(NotificationCompat.CATEGORY_ERROR);
			builder.setStyle(new NotificationCompat.BigTextStyle().bigText(e.toString()).setSummaryText(e.getLocalizedMessage()));
			NotificationUtils.notify(context, NotificationUtils.ID_PROVIDER_ERROR, builder);
		}
	}
}
