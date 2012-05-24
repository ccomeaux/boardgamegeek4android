package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class CollectionProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.COLLECTION);
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.COLLECTION_JOIN_GAMES).mapToTable(Collection._ID, Tables.COLLECTION)
				.mapToTable(Collection.GAME_ID, Tables.COLLECTION);
	}

	@Override
	protected String getPath() {
		return "collection";
	}

	@Override
	protected String getType(Uri uri) {
		return Collection.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		if (db.insertOrThrow(Tables.COLLECTION, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Collection.buildItemUri(values.getAsInteger(Collection.COLLECTION_ID));
	}
}
