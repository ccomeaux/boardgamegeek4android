package com.boardgamegeek.export.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.GameColors;
import com.google.gson.annotations.Expose;

import androidx.annotation.NonNull;

public class Color {
	public static final String[] PROJECTION = new String[] {
		GameColors.COLOR,
	};

	private static final int COLOR = 0;

	@Expose private String color;

	public String getColor() {
		return color;
	}

	@NonNull
	public static Color fromCursor(@NonNull Cursor cursor) {
		Color color = new Color();
		color.color = cursor.getString(COLOR);
		return color;
	}

	@Override
	public String toString() {
		return color;
	}
}
