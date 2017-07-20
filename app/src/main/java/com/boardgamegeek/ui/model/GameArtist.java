package com.boardgamegeek.ui.model;


import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Games;

public class GameArtist extends GameList {
	public static final String[] PROJECTION = {
		Artists._ID,
		Artists.ARTIST_ID,
		Artists.ARTIST_NAME
	};

	public static Uri buildUri(int gameId) {
		return Games.buildArtistsUri(gameId);
	}
}
