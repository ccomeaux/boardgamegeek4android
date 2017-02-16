package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.SelectionBuilder;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class SyncGamePlays extends UpdateTask {
	private final int gameId;

	@DebugLog
	public SyncGamePlays(int gameId) {
		this.gameId = gameId;
	}

	@DebugLog
	@NonNull
	@Override
	public String getDescription(Context context) {
		if (isValid()) {
			return context.getString(R.string.sync_msg_game_plays_valid, String.valueOf(gameId));
		}
		return context.getString(R.string.sync_msg_game_plays_invalid);
	}

	@DebugLog
	@Override
	public boolean isValid() {
		return gameId != BggContract.INVALID_ID;
	}

	@DebugLog
	@Override
	public void execute(@NonNull Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		BggService service = Adapter.createForXml();
		PlayPersister persister = new PlayPersister(context);
		PlaysResponse response;
		try {
			final long startTime = System.currentTimeMillis();
			int page = 1;
			do {
				response = service.playsByGame(account.name, gameId, page).execute().body();
				persister.save(response.plays, startTime);
				page++;
			} while (response.hasMorePages());
			deleteUnupdatedPlays(context, startTime);
			updateGameTimestamp(context);
			SyncService.hIndex(context);
			Timber.i("Synced plays for game id=" + gameId);
		} catch (Exception e) {
			// TODO bubble error up?
			Timber.w(e, "Problem syncing plays for game id=" + gameId);
		}
	}

	@DebugLog
	private void updateGameTimestamp(@NonNull Context context) {
		ContentValues values = new ContentValues(1);
		values.put(Games.UPDATED_PLAYS, System.currentTimeMillis());
		context.getContentResolver().update(Games.buildGameUri(gameId), values, null, null);
	}

	private void deleteUnupdatedPlays(@NonNull Context context, long startTime) {
		int count = context.getContentResolver().delete(Plays.CONTENT_URI,
			Plays.SYNC_TIMESTAMP + "<? AND " +
				Plays.OBJECT_ID + "=? AND " +
				SelectionBuilder.whereZeroOrNull(Plays.UPDATE_TIMESTAMP) + " AND " +
				SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP) + " AND " +
				SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP),
			new String[] { String.valueOf(startTime), String.valueOf(gameId) });
		Timber.i("Deleted %,d unupdated play(s) of game ID=%s", count, gameId);
	}
}
