package com.boardgamegeek.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPlaysProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		String gameId = uri.getLastPathSegment();
		return new SelectionBuilder().table(Tables.PLAY_ITEMS).whereEquals(PlayItems.OBJECT_ID, gameId);
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		return new SelectionBuilder().table(Tables.PLAY_ITEMS_JOIN_PLAYS).mapToTable(BaseColumns._ID, Tables.PLAYS)
			.mapToTable(Plays.PLAY_ID, Tables.PLAYS).whereEquals(PlayItems.OBJECT_ID, gameId);
	}

	@Override
	protected String getDefaultSortOrder() {
		return Plays.DEFAULT_SORT;
	}

	@Override
	protected String getPath() {
		return "games/#/plays";
	}

	@Override
	protected String getType(Uri uri) {
		return Plays.CONTENT_TYPE;
	}
}
