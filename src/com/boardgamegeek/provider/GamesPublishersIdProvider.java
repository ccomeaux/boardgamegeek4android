package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesPublishersIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long id = ContentUris.parseId(uri);
		return new SelectionBuilder().table(Tables.GAMES_PUBLISHERS).whereEquals(BaseColumns._ID, id);
	}

	@Override
	protected String getPath() {
		return "games/publishers/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Publishers.CONTENT_ITEM_TYPE;
	}
}
