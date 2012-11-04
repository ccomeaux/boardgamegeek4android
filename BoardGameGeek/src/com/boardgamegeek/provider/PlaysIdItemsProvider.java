package com.boardgamegeek.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysIdItemsProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int playId = Plays.getPlayId(uri);
		return new SelectionBuilder().table(Tables.PLAY_ITEMS).whereEquals(PlayItems.PLAY_ID, playId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return PlayItems.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "plays/#/items";
	}

	@Override
	protected String getType(Uri uri) {
		return PlayItems.CONTENT_TYPE;
	}

	@Override
	protected Uri insert(Context context, SQLiteDatabase db, Uri uri, ContentValues values) {
		Uri insertedUri = null;
		int playId = Plays.getPlayId(uri);
		values.put(PlayItems.PLAY_ID, playId);
		if (db.insertOrThrow(Tables.PLAY_ITEMS, null, values) != -1) {
			insertedUri = Plays.buildItemUri(playId, values.getAsInteger(PlayItems.OBJECT_ID));
			// notify game that its plays have been updated
			int gameId = values.getAsInteger(PlayItems.OBJECT_ID);
			notifyChange(context, Games.buildGameUri(gameId));
		}
		return insertedUri;
	}
}
