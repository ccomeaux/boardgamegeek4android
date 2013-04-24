package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysPlayersProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		return new SelectionBuilder().table(Tables.PLAY_PLAYERS);
	}

	@Override
	protected SelectionBuilder buildExpandedSelection(Uri uri) {
		if (BggContract.FRAGMENT_NAME.equals(uri.getFragment())) {
			return new SelectionBuilder().table(Tables.PLAY_PLAYERS).groupBy(PlayPlayers.NAME);
		} else {
			return new SelectionBuilder().table(Tables.PLAY_PLAYERS_JOIN_PLAYS).mapToTable(Plays._ID, Tables.PLAYS)
				.mapToTable(Plays.PLAY_ID, Tables.PLAYS);
		}
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
