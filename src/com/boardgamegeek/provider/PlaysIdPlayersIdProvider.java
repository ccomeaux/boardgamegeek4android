package com.boardgamegeek.provider;

import android.net.Uri;

import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggDatabase.Tables;
import com.boardgamegeek.util.SelectionBuilder;

public class PlaysIdPlayersIdProvider extends BaseProvider {

	@Override
	protected SelectionBuilder buildSimpleSelection(Uri uri) {
		int playId = Plays.getPlayId(uri);
		long rowId = PlayPlayers.getPlayPlayerId(uri);
		return new SelectionBuilder().table(Tables.PLAY_PLAYERS).whereEquals(PlayPlayers.PLAY_ID, playId)
				.whereEquals(PlayPlayers._ID, rowId);
	}

	@Override
	protected String getPath() {
		return "plays/#/players/#";
	}

	@Override
	protected String getType(Uri uri) {
		return PlayPlayers.CONTENT_ITEM_TYPE;
	}
}
