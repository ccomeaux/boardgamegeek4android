package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.ThingRequest;
import com.boardgamegeek.io.ThingResponse;
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
	public String getDescription(Context context) {
		if (isValid()) {
			return context.getString(R.string.sync_msg_game_valid, String.valueOf(gameId));
		}
		return context.getString(R.string.sync_msg_game_invalid);
	}

	@Override
	public boolean isValid() {
		return gameId != BggContract.INVALID_ID;
	}

	@Override
	public void execute(Context context) {
		ThingResponse response = new ThingRequest(Adapter.createForXml(), gameId).execute();
		if (response.hasError()) {
			Timber.w("Failed syncing game ID=%s", gameId);
		} else {
			int rowCount = new GamePersister(context).save(response.getGames(), "Game " + gameId);
			Timber.i("Synced game ID=%s (%,d rows)", gameId, rowCount);
		}
	}
}
