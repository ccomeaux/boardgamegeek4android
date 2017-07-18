package com.boardgamegeek.ui.model;


import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;

public class GameRank {
	public static final String[] PROJECTION = {
		GameRanks.GAME_RANK_NAME,
		GameRanks.GAME_RANK_VALUE,
		GameRanks.GAME_RANK_TYPE
	};

	private static final int NAME = 0;
	private static final int VALUE = 1;
	private static final int TYPE = 2;

	private String name;
	private int rank;
	private String type;

	private GameRank() {
	}

	public static Uri buildUri(int gameId) {
		return Games.buildRanksUri(gameId);
	}

	public static GameRank fromCursor(Cursor cursor) {
		GameRank gr = new GameRank();
		gr.name = cursor.getString(NAME);
		gr.rank = cursor.getInt(VALUE);
		gr.type = cursor.getString(TYPE);
		return gr;
	}

	public String getName() {
		return name;
	}

	public int getRank() {
		return rank;
	}

	public String getType() {
		return type;
	}

	public boolean isFamilyType() {
		return BggService.RANK_TYPE_FAMILY.equals(type);
	}
}
