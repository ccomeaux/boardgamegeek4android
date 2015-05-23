package com.boardgamegeek.export.model;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Games;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

public class Game {
	public static String[] PROJECTION = new String[] {
		Games.GAME_ID,
	};

	private static final int GAME_ID = 0;

	@Expose private int id;
	@Expose private List<Color> colors;

	public int getGameId() {
		return id;
	}

	public boolean hasColors() {
		return colors != null && colors.size() > 0;
	}

	public List<Color> getColors() {
		return colors;
	}

	public static Game fromCursor(Cursor cursor) {
		Game game = new Game();
		game.id = cursor.getInt(GAME_ID);
		return game;
	}

	public void addColors(Context context) {
		colors = new ArrayList<>();

		final Cursor cursor = context.getContentResolver().query(
			Games.buildColorsUri(id),
			Color.PROJECTION,
			null, null, null);

		if (cursor == null) {
			return;
		}

		try {
			while (cursor.moveToNext()) {
				Color color = Color.fromCursor(cursor);
				colors.add(color);
			}
		} finally {
			cursor.close();
		}
	}
}
