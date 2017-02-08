package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPlaysProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		String gameId = uri.getLastPathSegment();
		return new SelectionBuilder()
			.table(Tables.PLAYS)
			.whereEquals(Plays.OBJECT_ID, gameId);
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
