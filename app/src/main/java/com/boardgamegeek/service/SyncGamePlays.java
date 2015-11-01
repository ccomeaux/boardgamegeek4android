package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;

import timber.log.Timber;

public class SyncGamePlays extends UpdateTask {
	private final int gameId;

	public SyncGamePlays(int gameId) {
		this.gameId = gameId;
	}

	@NonNull
	@Override
	public String getDescription() {
		// TODO use resources for description
		if (isValid()) {
			return "update plays for game " + gameId;
		}
		return "update plays for unknown game";
	}

	@Override
	public boolean isValid() {
		return gameId != BggContract.INVALID_ID;
	}

	@Override
	public void execute(@NonNull Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		BggService service = Adapter.create();
		PlayPersister persister = new PlayPersister(context);
		PlaysResponse response;
		try {
			long startTime = System.currentTimeMillis();
			response = service.playsByGame(account.name, gameId);
			persister.save(response.plays, startTime);
			updateGameTimestamp(context);
			SyncService.hIndex(context);
			Timber.i("Synced plays for game id=" + gameId);
		} catch (Exception e) {
			// TODO bubble error up?
			Timber.w(e, "Problem syncing plays for game id=" + gameId);
		}
	}

	private void updateGameTimestamp(@NonNull Context context) {
		ContentValues values = new ContentValues(1);
		values.put(Games.UPDATED_PLAYS, System.currentTimeMillis());
		context.getContentResolver().update(Games.buildGameUri(gameId), values, null, null);
	}
}
