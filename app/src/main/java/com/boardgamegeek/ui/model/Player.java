package com.boardgamegeek.ui.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.PlayPlayers;

public class Player {

	public static String[] PROJECTION = {
		PlayPlayers._ID,
		PlayPlayers.NAME,
		PlayPlayers.USER_NAME,
		PlayPlayers.SUM_QUANTITY
	};

	private static final int NAME = 1;
	private static final int USER_NAME = 2;
	private static final int SUM_QUANTITY = 3;

	private String name;
	private String username;
	private int playCount;

	public static Player fromCursor(Cursor cursor) {
		Player location = new Player();
		location.name = cursor.getString(NAME);
		location.username = cursor.getString(USER_NAME);
		location.playCount = cursor.getInt(SUM_QUANTITY);
		return location;
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}

	public int getPlayCount() {
		return playCount;
	}
}
