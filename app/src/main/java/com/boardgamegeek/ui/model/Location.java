package com.boardgamegeek.ui.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Plays;

public class Location {

	public static final String[] PROJECTION = {
		Plays._ID,
		Plays.LOCATION,
		Plays.SUM_QUANTITY
	};

	private static final int LOCATION = 1;
	private static final int SUM_QUANTITY = 2;

	private String name;
	private int playCount;

	public static Location fromCursor(Cursor cursor) {
		Location location = new Location();
		location.name = cursor.getString(LOCATION);
		location.playCount = cursor.getInt(SUM_QUANTITY);
		return location;
	}

	public String getName() {
		return name;
	}

	public int getPlayCount() {
		return playCount;
	}
}
