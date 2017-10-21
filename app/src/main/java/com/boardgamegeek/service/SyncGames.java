package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Game;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.model.GameList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
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

				if (numberOfFetches > 0) if (wasSleepInterrupted(5000)) return;

				numberOfFetches++;
				GameList gameList = getGameIds(GAMES_PER_FETCH);
				if (gameList.getSize() > 0) {
					Timber.i("...found %,d games to update [%s]", gameList.getSize(), gameList.getDescription());
					String detail = context.getResources().getQuantityString(R.plurals.sync_notification_games, gameList.getSize(), gameList.getSize(), gameList.getDescription());
					if (numberOfFetches > 1) {
						detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches);
					}
					updateProgressNotification(detail);

					Call<ThingResponse> call = service.thing(gameList.getIds(), 1);
					try {
						Response<ThingResponse> response = call.execute();
						if (response.isSuccessful()) {
							final List<Game> games = response.body() == null ? new ArrayList<Game>(0) : response.body().getGames();
							if (games.size() > 0) {
								int count = new GamePersister(context).save(games, detail);
								syncResult.stats.numUpdates += games.size();
								Timber.i("...saved %,d rows for %,d games", count, games.size());
							} else {
								Timber.i("...no games returned");
								break;
							}
						} else {
							Timber.w("Received response %s while syncing games.", response.code());
							if (response.code() >= 500) {
								showError(context.getString(R.string.msg_sync_response_500, response.code()));
								return;
							} else if (response.code() == 429) {
								showError(context.getString(R.string.msg_sync_response_429));
								if (wasSleepInterrupted(5000)) return;
							} else {
								showError(context.getString(R.string.msg_sync_unsuccessful_response,
									context.getString(R.string.game),
									response.code()));
								return;
							}
						}
					} catch (IOException e) {
						showError(e.getLocalizedMessage());
						syncResult.stats.numIoExceptions++;
						break;
					} catch (RuntimeException e) {
						Throwable cause = e.getCause();
						if (cause instanceof ClassNotFoundException &&
							cause.getMessage().startsWith("Didn't find class \"messagebox error\" on path")) {
							syncResult.stats.numParseExceptions++;
							Timber.i("Invalid list of game IDs: %s", gameList.getIds());
							for (int i = 0; i < gameList.getSize(); i++) {
								final boolean shouldBreak = syncGame(gameList.getId(i), syncResult, gameList.getName(i));
								if (shouldBreak) break;
							}
						} else {
							Timber.w(e);
							showError(e.getLocalizedMessage());
							syncResult.stats.numParseExceptions++;
							break;
						}
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

	private boolean syncGame(Integer id, @NonNull SyncResult syncResult, String gameName) {
		Call<ThingResponse> call = service.thing(id, 1);
		try {
			Response<ThingResponse> response = call.execute();
			if (response.isSuccessful()) {
				final List<Game> games = response.body() == null ? new ArrayList<Game>(0) : response.body().getGames();
				String detail = context.getResources().getQuantityString(R.plurals.sync_notification_games, 1, 1, gameName);
				int count = new GamePersister(context).save(games, detail);
				syncResult.stats.numUpdates += games.size();
				Timber.i("...saved %,d rows for %,d games", count, games.size());
			} else {
				Timber.w("Received response %s while syncing games.", response.code());
				if (response.code() >= 500) {
					showError(context.getString(R.string.msg_sync_response_500, response.code()));
					return true;
				} else if (response.code() == 429) {
					showError(context.getString(R.string.msg_sync_response_429));
					if (wasSleepInterrupted(5000)) return true;
				} else {
					showError(context.getString(R.string.msg_sync_unsuccessful_response,
						context.getString(R.string.game),
						response.code()));
					return true;
				}
			}
		} catch (IOException e) {
			showError(e.getLocalizedMessage());
			syncResult.stats.numIoExceptions++;
			return true;
		}
		return false;
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
