package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysPlayersProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.PLAY_PLAYERS).groupBy(PlayPlayers.NAME);
	}

	@Override
	protected String getDefaultSortOrder() {
		return PlayPlayers.NAME;
	}

	@Override
	protected String getPath() {
		return "plays/players";
	}
}
