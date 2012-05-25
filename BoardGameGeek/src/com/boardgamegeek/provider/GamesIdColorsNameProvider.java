package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.GameColors;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class GamesIdColorsNameProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int gameId = Games.getGameId(uri);
		String color = uri.getLastPathSegment();
		return new SelectionBuilder().table(Tables.GAME_COLORS).whereEquals(GameColors.GAME_ID, gameId)
				.whereEquals(GameColors.COLOR, color);
	}

	@Override
	protected String getPath() {
		return "games/#/colors/*";
	}

	@Override
	protected String getType(Uri uri) {
		return GameColors.CONTENT_ITEM_TYPE;
	}
}
