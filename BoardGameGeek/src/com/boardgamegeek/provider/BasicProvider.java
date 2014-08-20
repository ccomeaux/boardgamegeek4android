package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.util.SelectionBuilder;

public abstract class BasicProvider extends BaseProvider {

	private long mRowId;

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(getTable());
	}

	protected Integer getInsertedId(ContentValues values) {
		return (int) mRowId;
	}

	protected abstract String getTable();

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		mRowId = db.insert(getTable(), null, values);
		if (mRowId != -1) {
			return insertedUri(values);
		}
		return null;
	}

	protected Uri insertedUri(ContentValues values) {
		return BggContract.buildBasicUri(getPath(), getInsertedId(values));
	}
}