package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteGameHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.util.HttpUtils;

public class SyncGame extends SyncTask {
	private static final String TAG = makeLogTag(SyncGame.class);
	private int mGameId;

	public SyncGame(int gameId) {
		mGameId = gameId;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		RemoteGameHandler handler = new RemoteGameHandler();
		handler.setParsePolls();
		executor.executeGet(HttpUtils.constructGameUrl(mGameId), handler);
		setIsBggDown(handler.isBggDown());
		LOGI(TAG, "Synced Game " + mGameId);
	}
}
