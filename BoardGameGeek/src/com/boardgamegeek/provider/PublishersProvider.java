package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PublishersProvider extends BaseProvider {
	
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.PUBLISHERS);
	}

	@Override
	protected String getPath() {
		return "publishers";
	}

	@Override
	protected String getType(Uri uri) {
		return Publishers.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		if (db.insertOrThrow(Tables.PUBLISHERS, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Publishers.buildPublisherUri(values.getAsInteger(Publishers.PUBLISHER_ID));
	}
}
