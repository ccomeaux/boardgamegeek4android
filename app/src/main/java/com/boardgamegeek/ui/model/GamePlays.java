package com.boardgamegeek.ui.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Games;

public class GamePlays {
	public static final String[] PROJECTION = {
		Games.GAME_NAME,
		Games.IMAGE_URL,
		Games.THUMBNAIL_URL,
		Games.UPDATED_PLAYS,
		Games.CUSTOM_PLAYER_SORT,
		Games.HERO_IMAGE_URL,
		Games.ICON_COLOR
	};

	private static final int GAME_NAME = 0;
	private static final int IMAGE_URL = 1;
	private static final int THUMBNAIL_URL = 2;
	private static final int UPDATED_PLAYS = 3;
	private static final int CUSTOM_PLAYER_SORT = 4;
	private static final int HERO_IMAGE_URL = 5;
	private static final int ICON_COLOR = 6;

	private String name;
	private String imageUrl;
	private String thumbnailUrl;
	private String heroImageUrl;
	private int iconColor;
	private long syncTimestamp;
	private boolean customPlayerSort;

	private GamePlays() {
	}

	public static GamePlays fromCursor(Cursor cursor) {
		GamePlays game = new GamePlays();
		game.name = cursor.getString(GAME_NAME);
		game.imageUrl = cursor.getString(IMAGE_URL);
		game.thumbnailUrl = cursor.getString(THUMBNAIL_URL);
		game.heroImageUrl = cursor.getString(HERO_IMAGE_URL);
		game.syncTimestamp = cursor.getLong(UPDATED_PLAYS);
		game.customPlayerSort = (cursor.getInt(CUSTOM_PLAYER_SORT) == 1);
		game.iconColor = cursor.getInt(ICON_COLOR);
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

	public String getHeroImageUrl() {
		return heroImageUrl;
	}

	public long getSyncTimestamp() {
		return syncTimestamp;
	}

	public boolean arePlayersCustomSorted() {
		return customPlayerSort;
	}

	public int getIconColor() {
		return iconColor;
	}
}
