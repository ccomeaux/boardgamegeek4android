package com.boardgamegeek.ui.model;


import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Mechanics;

public class GameMechanic extends GameList {
	public static final String[] PROJECTION = {
		Mechanics._ID,
		Mechanics.MECHANIC_ID,
		Mechanics.MECHANIC_NAME
	};

	public static Uri buildUri(int gameId) {
		return Games.buildMechanicsUri(gameId);
	}
}
