package com.boardgamegeek.service;

import android.content.Context;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;
import com.boardgamegeek.provider.BggContract;

import timber.log.Timber;

public class SyncGame extends UpdateTask {
	private int mGameId;

	public SyncGame(int gameId) {
		mGameId = gameId;
	}

	@Override
	public String getDescription() {
		if (mGameId == BggContract.INVALID_ID) {
			return "update an unknown game";
		}
		return "update game " + mGameId;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		GamePersister gp = new GamePersister(context);
		ThingResponse response = service.thing(mGameId, 1);
		gp.save(response.getGames(), "Game " + mGameId);
		Timber.i("Synced Game " + mGameId);
	}
}
