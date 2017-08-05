package com.boardgamegeek.ui.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Games;

public class GamePlays {
	public static final String[] PROJECTION = {
		Games.GAME_NAME,
		Games.IMAGE_URL,
		Games.THUMBNAIL_URL,
		Games.UPDATED_PLAYS,
		Games.CUSTOM_PLAYER_SORT
	};

	private static final int GAME_NAME = 0;
	private static final int IMAGE_URL = 1;
	private static final int THUMBNAIL_URL = 2;
	private static final int UPDATED_PLAYS = 3;
	private static final int CUSTOM_PLAYER_SORT = 4;

	private String name;
	private String imageUrl;
	private String thumbnailUrl;
	private long updated;
	private boolean customPlayerSort;

	private GamePlays() {
	}

	public static GamePlays fromCursor(Cursor cursor) {
		GamePlays game = new GamePlays();
		game.name = cursor.getString(GAME_NAME);
		game.imageUrl = cursor.getString(IMAGE_URL);
		game.thumbnailUrl = cursor.getString(THUMBNAIL_URL);
		game.updated = cursor.getLong(UPDATED_PLAYS);
		game.customPlayerSort = (cursor.getInt(CUSTOM_PLAYER_SORT) == 1);
		return game;
	}

	public String getName() {
		return name;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public long getUpdated() {
		return updated;
	}

	public boolean arePlayersCustomSorted() {
		return customPlayerSort;
	}
}
