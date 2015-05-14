package com.boardgamegeek.export.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.GameColors;
import com.google.gson.annotations.Expose;

public class Color {
	public static String[] PROJECTION = new String[] {
		GameColors.COLOR,
	};

	private static final int COLOR = 0;

	@Expose private String color;

	public String getColor() {
		return color;
	}

	public static Color fromCursor(Cursor cursor) {
		Color color = new Color();
		color.color = cursor.getString(COLOR);
		return color;
	}
}
