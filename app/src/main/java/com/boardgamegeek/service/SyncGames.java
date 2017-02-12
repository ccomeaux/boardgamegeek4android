package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.ThingRequest;
import com.boardgamegeek.model.Game;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;
import com.boardgamegeek.util.StringUtils;

import java.util.List;

import timber.log.Timber;

public abstract class SyncGames extends SyncTask {
	private static final int GAMES_PER_FETCH = 10;

	public SyncGames(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, @NonNull SyncResult syncResult) {
		Timber.i(getIntroLogMessage(GAMES_PER_FETCH));
		try {
			int numberOfFetches = 0;
			do {
				if (isCancelled()) break;

				numberOfFetches++;
				List<String> gameIds = getGameIds(GAMES_PER_FETCH);
				if (gameIds.size() > 0) {
					String gameIdDescription = StringUtils.formatList(gameIds);
					Timber.i("...found %,d games to update [%s]", gameIds.size(), gameIdDescription);
					String detail = context.getResources().getQuantityString(R.plurals.sync_notification_games, GAMES_PER_FETCH, GAMES_PER_FETCH, gameIdDescription);
					if (numberOfFetches > 1) {
						detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches);
					}
					updateProgressNotification(detail);

					ThingResponse response = getThingResponse(service, gameIds);
					final List<Game> games = response.getGames();
					if (games != null && games.size() > 0) {
						int count = new GamePersister(context).save(games, detail);
						syncResult.stats.numUpdates += games.size();
						Timber.i("...saved %,d rows for %,d games", count, games.size());
					} else {
						Timber.i("...no games returned ");
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

	private ThingResponse getThingResponse(BggService service, List<String> gameIds) {
		String ids = TextUtils.join(",", gameIds);
		return new ThingRequest(service, ids).execute();
	}

	@NonNull
	protected abstract String getIntroLogMessage(int gamesPerFetch);

	@NonNull
	protected abstract String getExitLogMessage();

	protected abstract List<String> getGameIds(int gamesPerFetch);
}
