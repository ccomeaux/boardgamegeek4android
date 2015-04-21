package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RetryableException;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;
import com.boardgamegeek.util.StringUtils;
import com.crashlytics.android.Crashlytics;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public abstract class SyncGames extends SyncTask {
	private static final int GAMES_PER_FETCH = 16;
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_BACKOFF_IN_MS = 500;
	private int mGamesPerFetch;

	public SyncGames(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		Timber.i(getIntroLogMessage());
		try {
			mGamesPerFetch = GAMES_PER_FETCH;
			int numberOfFetches = 0;
			do {
				if (isCancelled()) {
					break;
				}
				numberOfFetches++;
				List<String> gameIds = getGameIds(mGamesPerFetch);
				if (gameIds.size() > 0) {
					String gameIdDescription = StringUtils.formatList(gameIds);
					Timber.i("...found " + gameIds.size() + " games to update [" + gameIdDescription + "]");
					String detail = mGamesPerFetch + " games: " + gameIdDescription;
					if (numberOfFetches > 1) {
						detail += " (page " + numberOfFetches + ")";
					}
					showNotification(detail);

					GamePersister persister = new GamePersister(mContext);
					ThingResponse response = getThingResponse(mService, gameIds);
					if (response.games != null && response.games.size() > 0) {
						int count = persister.save(response.games, detail);
						syncResult.stats.numUpdates += response.games.size();
						Timber.i("...saved " + count + " rows for " + response.games.size() + " games");
					} else {
						Timber.i("...no games returned (shouldn't happen)");
					}
				} else {
					Timber.i(getExitLogMessage());
					break;
				}
			} while (numberOfFetches < getMaxFetchCount());
		} finally {
			Timber.i("...complete!");
		}
	}

	protected int getMaxFetchCount() {
		return 1;
	}

	protected ThingResponse getThingResponse(BggService service, List<String> gameIds) {
		int retries = 0;
		while (true) {
			try {
				String ids = TextUtils.join(",", gameIds);
				if (Fabric.isInitialized()) {
					Crashlytics.setString("GAME_IDS", ids);
				}
				return service.thing(ids, 1);
			} catch (Exception e) {
				if (e.getCause() instanceof SocketTimeoutException) {
					if (mGamesPerFetch == 1) {
						Timber.i("...timeout with only 1 game; aborting.");
						break;
					}
					mGamesPerFetch = mGamesPerFetch / 2;
					Timber.i("...timeout - reducing games per fetch to " + mGamesPerFetch);
				} else if (e.getCause() instanceof RetryableException) {
					retries++;
					if (retries > MAX_RETRIES) {
						break;
					}
					try {
						Timber.i("...retrying #" + retries);
						Thread.sleep(retries * retries * RETRY_BACKOFF_IN_MS);
					} catch (InterruptedException e1) {
						Timber.i("Interrupted while sleeping before retry " + retries);
						break;
					}
				} else {
					throw e;
				}
			}
		}
		ThingResponse response = new ThingResponse();
		response.games = new ArrayList<>();
		return response;
	}

	protected abstract String getIntroLogMessage();

	protected abstract String getExitLogMessage();

	protected abstract List<String> getGameIds(int gamesPerFetch);
}
