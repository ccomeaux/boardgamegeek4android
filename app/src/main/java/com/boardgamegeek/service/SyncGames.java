package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.ThingRequest;
import com.boardgamegeek.io.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.model.GameList;

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
				GameList gameList = getGameIds(GAMES_PER_FETCH);
				if (gameList.getSize() > 0) {
					Timber.i("...found %,d games to update [%s]", gameList.getSize(), gameList.getDescription());
					String detail = context.getResources().getQuantityString(R.plurals.sync_notification_games, GAMES_PER_FETCH, GAMES_PER_FETCH, gameList.getDescription());
					if (numberOfFetches > 1) {
						detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches);
					}
					updateProgressNotification(detail);

					ThingResponse response = new ThingRequest(service, gameList.getIds()).execute();
					if (response.hasError()) {
						Timber.w("Error encountered during sync: %s", response.getError());
						break;
					} else if (response.getNumberOfGames() > 0) {
						int count = new GamePersister(context).save(response.getGames(), detail);
						syncResult.stats.numUpdates += response.getNumberOfGames();
						Timber.i("...saved %,d rows for %,d games", count, response.getNumberOfGames());
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

	@NonNull
	protected abstract String getIntroLogMessage(int gamesPerFetch);

	@NonNull
	protected abstract String getExitLogMessage();

	private GameList getGameIds(int gamesPerFetch) {
		GameList list = new GameList(gamesPerFetch);
		Cursor cursor = context.getContentResolver().query(Games.CONTENT_URI,
			new String[] { Games.GAME_ID, Games.GAME_NAME },
			getSelection(),
			null,
			String.format("games.%s LIMIT %s", Games.UPDATED_LIST, gamesPerFetch));
		try {
			while (cursor != null && cursor.moveToNext()) {
				list.addGame(cursor.getInt(0), cursor.getString(1));
			}
		} finally {
			if (cursor != null) cursor.close();
		}
		return list;
	}

	protected String getSelection() {
		return null;
	}
}
