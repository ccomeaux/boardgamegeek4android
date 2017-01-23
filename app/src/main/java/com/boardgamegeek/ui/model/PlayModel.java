package com.boardgamegeek.ui.model;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.CursorUtils;

public class PlayModel {
	public static final String[] PROJECTION = {
		Plays._ID,
		Plays.PLAY_ID,
		Plays.DATE,
		Plays.ITEM_NAME,
		Plays.OBJECT_ID,
		Plays.LOCATION,
		Plays.QUANTITY,
		Plays.LENGTH,
		Plays.SYNC_STATUS,
		Plays.PLAYER_COUNT,
		Games.THUMBNAIL_URL,
		Games.IMAGE_URL,
		Plays.COMMENTS,
		Plays.DELETE_TIMESTAMP
	};

	private static final int PLAY_ID = 1;
	private static final int DATE = 2;
	private static final int GAME_NAME = 3;
	private static final int GAME_ID = 4;
	private static final int LOCATION = 5;
	private static final int QUANTITY = 6;
	private static final int LENGTH = 7;
	private static final int SYNC_STATUS = 8;
	private static final int PLAYER_COUNT = 9;
	private static final int THUMBNAIL_URL = 10;
	private static final int IMAGE_URL = 11;
	private static final int COMMENTS = 12;
	private static final int DELETE_TIMESTAMP = 13;

	private int playId;
	private int gameId;
	private String name;
	private String date;
	private String location;
	private int quantity;
	private int length;
	private int playerCount;
	private String comments;
	private int status;
	private String thumbnailUrl;
	private String imageUrl;
	private long deleteTimestamp;

	public static PlayModel fromCursor(Cursor cursor, Context context) {
		PlayModel play = new PlayModel();
		play.playId = cursor.getInt(PLAY_ID);
		play.name = cursor.getString(GAME_NAME);
		play.gameId = cursor.getInt(GAME_ID);
		play.date = CursorUtils.getFormattedDateAbbreviated(cursor, context, DATE);
		play.location = cursor.getString(LOCATION);
		play.quantity = cursor.getInt(QUANTITY);
		play.length = cursor.getInt(LENGTH);
		play.playerCount = cursor.getInt(PLAYER_COUNT);
		play.comments = CursorUtils.getString(cursor, COMMENTS).trim();
		play.status = cursor.getInt(SYNC_STATUS);
		play.thumbnailUrl = cursor.getString(THUMBNAIL_URL);
		play.imageUrl = cursor.getString(IMAGE_URL);
		play.deleteTimestamp = cursor.getLong(DELETE_TIMESTAMP);
		return play;
	}

	public int getPlayId() {
		return playId;
	}

	public String getName() {
		return name;
	}

	public String getDate() {
		return date;
	}

	public String getLocation() {
		return location;
	}

	public int getQuantity() {
		return quantity;
	}

	public int getLength() {
		return length;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public String getComments() {
		return comments;
	}

	public int getStatus() {
		return status;
	}

	public int getGameId() {
		return gameId;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public long getDeleteTimestamp() {
		return deleteTimestamp;
	}
}
