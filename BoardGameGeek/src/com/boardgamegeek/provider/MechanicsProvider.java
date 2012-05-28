package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class MechanicsProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.MECHANICS);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Mechanics.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "mechanics";
	}

	@Override
	protected String getType(Uri uri) {
		return Mechanics.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		if (db.insertOrThrow(Tables.MECHANICS, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Mechanics.buildMechanicUri(values.getAsInteger(Mechanics.MECHANIC_ID));
	}
}
