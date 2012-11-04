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
import com.boardgamegeek.util.StringUtils;

public class PlaysIdItemsIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int playId = Plays.getPlayId(uri);
		int objectId = PlayItems.getPlayItemId(uri);
		return new SelectionBuilder().table(Tables.PLAY_ITEMS).whereEquals(PlayItems.PLAY_ID, playId)
			.whereEquals(PlayItems.OBJECT_ID, objectId);
	}

	@Override
	protected String getPath() {
		return "plays/#/items/#";
	}

	@Override
	protected String getType(Uri uri) {
		return PlayItems.CONTENT_ITEM_TYPE;
	}

	@Override
	protected int update(Context context, SQLiteDatabase db, Uri uri, ContentValues values, String selection,
		String[] selectionArgs) {
		int rowCount = super.update(context, db, uri, values, selection, selectionArgs);
		// notify game that its plays have been updated
		int gameId = StringUtils.parseInt(uri.getLastPathSegment());
		notifyChange(context, Games.buildGameUri(gameId));
		return rowCount;
	}
}
