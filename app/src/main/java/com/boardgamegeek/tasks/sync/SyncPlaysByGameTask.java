package com.boardgamegeek.tasks.sync;


import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask.CompletedEvent;
import com.boardgamegeek.util.SelectionBuilder;

import hugo.weaving.DebugLog;
import retrofit2.Call;
import timber.log.Timber;

public class SyncPlaysByGameTask extends SyncTask<PlaysResponse, CompletedEvent> {
	private final int gameId;
	private final String username;

	public SyncPlaysByGameTask(Context context, int gameId) {
		super(context);
		this.gameId = gameId;
		username = AccountUtils.getUsername(context);
	}

	@Override
	@StringRes
	protected int getTypeDescriptionResId() {
		return R.string.title_plays;
	}

	@Override
	protected Call<PlaysResponse> createCall() {
		return bggService.playsByGame(username, gameId, getCurrentPage());
	}

	@Override
	protected boolean isRequestParamsValid() {
		return super.isRequestParamsValid() &&
			gameId != BggContract.INVALID_ID &&
			!TextUtils.isEmpty(username);
	}

	@Override
	protected void persistResponse(PlaysResponse body) {
		PlayPersister persister = new PlayPersister(context);
		persister.save(body.plays, startTime);
		Timber.i("Synced plays for game ID %s (page %,d)", gameId, getCurrentPage());
	}

	@Override
	protected boolean hasMorePages(PlaysResponse body) {
		return body.hasMorePages();
	}

	@Override
	protected void finishSync() {
		deleteUnupdatedPlays(context, startTime);
		updateGameTimestamp(context);
		if (SyncService.isPlaysSyncUpToDate(context)) {
			SyncService.calculateAndUpdateHIndex(context);
		}
	}

	@NonNull
	@Override
	protected CompletedEvent createEvent(String errorMessage) {
		return new CompletedEvent(errorMessage);
	}

	@DebugLog
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

	@DebugLog
	private void updateGameTimestamp(@NonNull Context context) {
		ContentValues values = new ContentValues(1);
		values.put(Games.UPDATED_PLAYS, System.currentTimeMillis());
		context.getContentResolver().update(Games.buildGameUri(gameId), values, null, null);
	}

	public class CompletedEvent extends SyncTask.CompletedEvent {
		public CompletedEvent(String errorMessage) {
			super(errorMessage);
		}
	}
}
