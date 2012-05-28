package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class ArtistsProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.ARTISTS);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Artists.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "artists";
	}

	@Override
	protected String getType(Uri uri) {
		return Artists.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
		if (db.insertOrThrow(Tables.ARTISTS, null, values) == -1) {
			throw new UnsupportedOperationException("Error inserting: " + uri);
		}
		return Artists.buildArtistUri(values.getAsInteger(Artists.ARTIST_ID));
	}
}
