package com.boardgamegeek.ui.model;


import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Publishers;

public class GamePublisher extends GameList {
	public static final String[] PROJECTION = {
		Publishers._ID,
		Publishers.PUBLISHER_ID,
		Publishers.PUBLISHER_NAME
	};

	public static Uri buildUri(int gameId) {
		return Games.buildPublishersUri(gameId);
	}
}
