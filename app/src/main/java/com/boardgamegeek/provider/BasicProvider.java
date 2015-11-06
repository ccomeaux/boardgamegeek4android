package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.util.SelectionBuilder;

import hugo.weaving.DebugLog;

public abstract class BasicProvider extends BaseProvider {
	private long rowId;

	@DebugLog
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(getTable());
	}

	@DebugLog
	protected String getInsertedIdColumn() {
		return null;
	}

	protected abstract String getTable();

	@DebugLog
	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		rowId = db.insert(getTable(), null, values);
		if (rowId != -1) {
			return insertedUri(values);
		}
		return null;
	}

	@DebugLog
	protected Uri insertedUri(ContentValues values) {
		return BggContract.buildBasicUri(getPath(), getInsertedId(values));
	}

	@DebugLog
	private Long getInsertedId(ContentValues values) {
		if (!TextUtils.isEmpty(getInsertedIdColumn())) {
			return values.getAsLong(getInsertedIdColumn());
		}
		return rowId;
	}
}