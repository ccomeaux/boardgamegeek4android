package com.boardgamegeek.ui.model;


import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;

public class GameBaseGame extends GameList {
	public static final String[] PROJECTION = {
		GamesExpansions._ID,
		GamesExpansions.EXPANSION_ID,
		GamesExpansions.EXPANSION_NAME
	};

	public static Uri buildUri(int gameId) {
		return Games.buildExpansionsUri(gameId);
	}

	public static String getSelection() {
		return GamesExpansions.INBOUND + "=?";
	}

	public static String[] getSelectionArgs() {
		return new String[] { "1" };
	}
}
