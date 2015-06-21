package com.boardgamegeek.ui.model;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.PlayerColors;

public class BuddyColor {
	public static String[] PROJECTION = {
		PlayerColors._ID,
		PlayerColors.PLAYER_COLOR,
		PlayerColors.PLAYER_COLOR_SORT_ORDER
	};

	private static final int PLAYER_COLOR = 1;
	private static final int SORT_ORDER = 2;

	private String color;

	private int sortOrder;

	public BuddyColor(String color, int order) {
		this.color = color;
		this.sortOrder = order;
	}

	@NonNull
	public static BuddyColor fromCursor(Cursor cursor) {
		return new BuddyColor(cursor.getString(PLAYER_COLOR), cursor.getInt(SORT_ORDER));
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
