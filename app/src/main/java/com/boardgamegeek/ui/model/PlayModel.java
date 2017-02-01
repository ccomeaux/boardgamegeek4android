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
		Plays.PLAYER_COUNT,
		Games.THUMBNAIL_URL,
		Games.IMAGE_URL,
		Plays.COMMENTS,
		Plays.DELETE_TIMESTAMP,
		Plays.UPDATE_TIMESTAMP,
		Plays.DIRTY_TIMESTAMP
	};

	private static final int PLAY_ID = 1;
	private static final int DATE = 2;
	private static final int GAME_NAME = 3;
	private static final int GAME_ID = 4;
	private static final int LOCATION = 5;
	private static final int QUANTITY = 6;
	private static final int LENGTH = 7;
	private static final int PLAYER_COUNT = 8;
	private static final int THUMBNAIL_URL = 9;
	private static final int IMAGE_URL = 10;
	private static final int COMMENTS = 11;
	private static final int DELETE_TIMESTAMP = 12;
	private static final int UPDATE_TIMESTAMP = 13;
	private static final int DIRTY_TIMESTAMP = 14;

	private int playId;
	private int gameId;
	private String name;
	private String date;
	private String location;
	private int quantity;
	private int length;
	private int playerCount;
	private String comments;
	private String thumbnailUrl;
	private String imageUrl;
	private long deleteTimestamp;
	private long updateTimestamp;
	private long dirtyTimestamp;

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
		play.thumbnailUrl = cursor.getString(THUMBNAIL_URL);
		play.imageUrl = cursor.getString(IMAGE_URL);
		play.deleteTimestamp = cursor.getLong(DELETE_TIMESTAMP);
		play.updateTimestamp = cursor.getLong(UPDATE_TIMESTAMP);
		play.dirtyTimestamp = cursor.getLong(DIRTY_TIMESTAMP);
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

	public long getUpdateTimestamp() {
		return updateTimestamp;
	}

	public long getDirtyTimestamp() {
		return dirtyTimestamp;
	}
}
