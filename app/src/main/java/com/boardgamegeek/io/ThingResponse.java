package com.boardgamegeek.io;

import android.text.TextUtils;

import com.boardgamegeek.model.Game;

import java.util.List;

public class ThingResponse {
	private String error;
	private List<Game> games;

	public ThingResponse(String error) {
		this.error = error;
	}

	public ThingResponse(String format, Object... args) {
		this.error = String.format(format, args);
	}

	public ThingResponse(List<Game> games) {
		this.games = games;
	}

	public boolean hasError() {
		return !TextUtils.isEmpty(error);
	}

	public String getError() {
		return error;
	}

	public List<Game> getGames() {
		return games;
	}

	public int getNumberOfGames() {
		return games == null ? 0 : games.size();
	}
}
