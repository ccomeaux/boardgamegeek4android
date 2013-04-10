package com.boardgamegeek.provider;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesDesignersIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		long id = ContentUris.parseId(uri);
		return new SelectionBuilder().table(Tables.GAMES_DESIGNERS).whereEquals(BaseColumns._ID, id);
	}

	@Override
	protected String getPath() {
		return "games/designers/#";
	}

	@Override
	protected String getType(Uri uri) {
		return Designers.CONTENT_ITEM_TYPE;
	}
}
