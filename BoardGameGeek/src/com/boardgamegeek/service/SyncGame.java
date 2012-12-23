package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteGameHandler;
import com.boardgamegeek.util.GameUrlBuilder;

public class SyncGame extends UpdateTask {
	private static final String TAG = makeLogTag(SyncGame.class);
	private int mGameId;

	public SyncGame(int gameId) {
		mGameId = gameId;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) {
		RemoteGameHandler handler = new RemoteGameHandler();
		handler.setParsePolls();
		String url = new GameUrlBuilder(mGameId).stats().build();
		executor.safelyExecuteGet(url, handler);
		LOGI(TAG, "Synced Game " + mGameId);
	}
}
