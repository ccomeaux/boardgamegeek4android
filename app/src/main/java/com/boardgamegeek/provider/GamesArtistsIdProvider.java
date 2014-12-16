package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesArtistsIdProvider extends BaseProvider {
	
	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		final long id = ContentUris.parseId(uri);
		return new SelectionBuilder().table(Tables.GAMES_ARTISTS).whereEquals(BaseColumns._ID, id);
	}

	@Override
	protected String getPath() {
		return "games/artists/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Artists.CONTENT_ITEM_TYPE;
	}
}
