package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;

public class SyncGame extends UpdateTask {
	private static final String TAG = makeLogTag(SyncGame.class);
	private int mGameId;

	public SyncGame(int gameId) {
		mGameId = gameId;
	}

	@Override
	public String getDescription() {
		return "Sync game ID=" + mGameId;
	}

	@Override
	public void execute(Context context) {
		long startTime = System.currentTimeMillis();
		BggService service = Adapter.create();
		ThingResponse response = service.thing(mGameId, 1);
		GamePersister gp = new GamePersister(context, response.games, startTime);
		gp.save();
		LOGI(TAG, "Synced Game " + mGameId);
	}
}
