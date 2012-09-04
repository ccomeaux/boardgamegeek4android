package com.boardgamegeek.provider;

import android.content.ContentValues;
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
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		mRowId = db.insertOrThrow(getTable(), null, values);
		if (mRowId != -1) {
			return BggContract.buildBasicUri(getPath(), getInsertedId(values));
		}
		return null;
	}
}