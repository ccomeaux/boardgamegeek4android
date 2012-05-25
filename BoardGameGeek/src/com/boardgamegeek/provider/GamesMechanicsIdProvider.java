package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesMechanicsIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long id = ContentUris.parseId(uri);
		return new SelectionBuilder().table(Tables.GAMES_MECHANICS).whereEquals(BaseColumns._ID, id);
	}

	@Override
	protected String getPath() {
		return "games/mechanics/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Mechanics.CONTENT_ITEM_TYPE;
	}
}
