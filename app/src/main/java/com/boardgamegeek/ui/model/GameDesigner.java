package com.boardgamegeek.ui.model;


import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.Games;

public class GameDesigner extends GameList {
	public static final String[] PROJECTION = {
		Designers._ID,
		Designers.DESIGNER_ID,
		Designers.DESIGNER_NAME
	};

	public static Uri buildUri(int gameId) {
		return Games.buildDesignersUri(gameId);
	}
}
