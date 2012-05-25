package com.boardgamegeek.provider;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdPollsNameProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String pollName = uri.getLastPathSegment();
		return new SelectionBuilder().table(Tables.GAME_POLLS).whereEquals(GamePolls.GAME_ID, gameId)
				.whereEquals(GamePolls.POLL_NAME, pollName);
	}

	@Override
	protected String getPath() {
		return "games/#/polls/*";
	}

	@Override
	protected String getType(Uri uri) {
		return GamePolls.CONTENT_ITEM_TYPE;
	}

	@Override
	protected void deleteChildren(SQLiteDatabase db, SelectionBuilder builder) {
		new GamesIdPollsProvider().deleteChildren(db, builder);
	}
}
