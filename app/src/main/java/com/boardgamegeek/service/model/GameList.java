package com.boardgamegeek.service.model;

import android.text.TextUtils;

import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GameList {
	private final List<Integer> gameIds;
	private final List<String> gameNames;

	public GameList(int count) {
		gameIds = new ArrayList<>(count);
		gameNames = new ArrayList<>(count);
	}

	public void addGame(int id, String name) {
		gameIds.add(id);
		gameNames.add(name);
	}

	public int getSize() {
		return gameIds.size();
	}

	public String getDescription() {
		return StringUtils.formatList(gameNames);
	}

	public String getIds() {
		return TextUtils.join(",", gameIds);
	}
}
