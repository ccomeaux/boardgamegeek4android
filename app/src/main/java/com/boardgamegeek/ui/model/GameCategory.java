package com.boardgamegeek.ui.model;


import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Games;

public class GameCategory extends GameList {
	public static final String[] PROJECTION = {
		Categories._ID,
		Categories.CATEGORY_ID,
		Categories.CATEGORY_NAME
	};

	public static Uri buildUri(int gameId) {
		return Games.buildCategoriesUri(gameId);
	}
}
