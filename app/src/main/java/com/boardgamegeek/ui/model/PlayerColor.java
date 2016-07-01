package com.boardgamegeek.ui.model;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.PlayerColors;

public class PlayerColor {
	public static String[] PROJECTION = {
		PlayerColors._ID,
		PlayerColors.PLAYER_COLOR,
		PlayerColors.PLAYER_COLOR_SORT_ORDER
	};

	private static final int PLAYER_COLOR = 1;
	private static final int SORT_ORDER = 2;

	private String color;

	private int sortOrder;

	public PlayerColor(String color, int order) {
		this.color = color;
		this.sortOrder = order;
	}

	@NonNull
	public static PlayerColor fromCursor(Cursor cursor) {
		return new PlayerColor(cursor.getString(PLAYER_COLOR), cursor.getInt(SORT_ORDER));
	}

	public String getColor() {
		return color;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	@Override
	public String toString() {
		return sortOrder + ": " + color;
	}
}
