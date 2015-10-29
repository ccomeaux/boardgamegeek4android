package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;
import com.boardgamegeek.provider.BggContract;

import timber.log.Timber;

public class SyncGame extends UpdateTask {
	private final int gameId;

	public SyncGame(int gameId) {
		this.gameId = gameId;
	}

	@NonNull
	@Override
	public String getDescription() {
		// TODO use resources for description
		if (gameId == BggContract.INVALID_ID) {
			return "update an unknown game";
		}
		return "update game " + gameId;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		GamePersister gp = new GamePersister(context);
		ThingResponse response = service.thing(gameId, 1);
		gp.save(response.getGames(), "Game " + gameId);
		Timber.i("Synced Game " + gameId);
	}
}
