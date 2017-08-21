package com.boardgamegeek.util.shortcut;

import android.content.Context;
import android.content.Intent;

import com.boardgamegeek.ui.GameActivity;

public class GameShortcutTask extends ShortcutTask {
	private final int gameId;
	private final String gameName;

	public GameShortcutTask(Context context, int gameId, String gameName, String thumbnailUrl) {
		super(context, thumbnailUrl);
		this.gameId = gameId;
		this.gameName = gameName;
	}

	@Override
	protected String getShortcutName() {
		return gameName;
	}

	@Override
	protected Intent createIntent() {
		return GameActivity.createIntentAsShortcut(gameId, gameName);
	}

	@Override
	protected String getId() {
		return "game-" + gameId;
	}
}
