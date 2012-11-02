package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.util.HttpUtils;

public class SyncGamePlays extends SyncTask {
	private static final String TAG = makeLogTag(SyncGamePlays.class);
	private int mGameId;

	public SyncGamePlays(int gameId) {
		mGameId = gameId;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		RemotePlaysHandler handler = new RemotePlaysHandler();
		executor.executeGet(HttpUtils.constructPlayUrlSpecific(mGameId, null), handler);
		setIsBggDown(handler.isBggDown());
		if (!handler.isBggDown()) {
			// TODO: update game.updated_plays with current time 
		}
		LOGI(TAG, "Synced plays for game " + mGameId);
	}
}
