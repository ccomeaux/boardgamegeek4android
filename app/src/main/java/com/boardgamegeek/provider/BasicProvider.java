package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.util.SelectionBuilder;

public abstract class BasicProvider extends BaseProvider {
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(getTable());
	}

	protected String getInsertedIdColumn() {
		return null;
	}

	protected abstract String getTable();

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		long rowId = db.insert(getTable(), null, values);
		if (rowId != -1) {
			return insertedUri(values, rowId);
		}
		return null;
	}

	protected Uri insertedUri(ContentValues values, long rowId) {
		if (!TextUtils.isEmpty(getInsertedIdColumn())) {
			return BggContract.buildBasicUri(getPath(), values.getAsLong(getInsertedIdColumn()));
		}
		return BggContract.buildBasicUri(getPath(), rowId);
	}
}
