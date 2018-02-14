package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.model.GameList;
import com.boardgamegeek.util.SelectionBuilder;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Syncs collection items that have not yet been updated completely with stats and private info (in batches of 25).
 */
public class SyncCollectionUnupdated extends SyncTask {
	@NonNull private final Account account;
	private static final int GAME_PER_FETCH = 25;
	private String detail;

	public SyncCollectionUnupdated(Context context, BggService service, @NonNull SyncResult syncResult, @NonNull Account account) {
		super(context, service, syncResult);
		this.account = account;
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@Override
	public void execute() {
		Timber.i("Syncing unupdated collection list...");
		try {
			int numberOfFetches = 0;
			CollectionPersister persister = new CollectionPersister.Builder(context)
				.includePrivateInfo()
				.includeStats()
				.build();
			ArrayMap<String, String> options = new ArrayMap<>();
			options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
			GameList previousGameList = new GameList(0);

			do {
				if (isCancelled()) break;

				if (numberOfFetches > 0) if (wasSleepInterrupted(5000)) return;

				numberOfFetches++;
				GameList gameIds = queryGames();
				if (areGamesListsEqual(gameIds, previousGameList)) {
					Timber.i("...didn't update any games; breaking out of fetch loop");
					break;
				}
				previousGameList = gameIds;

				if (gameIds.getSize() > 0) {
					detail = context.getString(R.string.sync_notification_collection_update_games, gameIds.getSize(), gameIds.getDescription());
					if (numberOfFetches > 1) {
						detail = context.getString(R.string.sync_notification_page_suffix, detail, numberOfFetches);
					}
					updateProgressNotification(detail);
					Timber.i("...found %,d games to update [%s]", gameIds.getSize(), gameIds.getDescription());

					options.put(BggService.COLLECTION_QUERY_KEY_ID, gameIds.getIds());
					options.remove(BggService.COLLECTION_QUERY_KEY_SUBTYPE);
					int itemCount = requestAndPersist(account.name, persister, options);

					if (itemCount < 0) {
						Timber.i("...unsuccessful sync; breaking out of fetch loop");
						cancel();
						break;
					}

					options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
					int accessoryCount = requestAndPersist(account.name, persister, options);

					if (accessoryCount < 0) {
						Timber.i("...unsuccessful sync; breaking out of fetch loop");
						cancel();
						break;
					}

					if (itemCount + accessoryCount == 0) {
						Timber.i("...unsuccessful sync; breaking out of fetch loop");
						break;
					}
				} else {
					Timber.i("...no more unupdated collection items");
					break;
				}
			} while (numberOfFetches < 100);
		} finally {
			Timber.i("...complete!");
		}
	}

	@NonNull
	public GameList queryGames() {
		GameList list = new GameList(GAME_PER_FETCH);
		Cursor cursor = context.getContentResolver().query(Collection.CONTENT_URI,
			new String[] { Collection.GAME_ID, Collection.GAME_NAME },
			SelectionBuilder.whereZeroOrNull("collection." + Collection.UPDATED),
			null,
			"collection." + Collection.UPDATED_LIST + " DESC LIMIT " + GAME_PER_FETCH);
		try {
			while (cursor != null && cursor.moveToNext()) {
				list.addGame(cursor.getInt(0), cursor.getString(1));
			}
		} finally {
			if (cursor != null) cursor.close();
		}
		return list;
	}


	private int requestAndPersist(String username, @NonNull CollectionPersister persister, ArrayMap<String, String> options) {
		Timber.i("..requesting collection items with options %s", options);

		Call<CollectionResponse> call = service.collection(username, options);
		try {
			Response<CollectionResponse> response = call.execute();
			if (response.isSuccessful()) {
				final CollectionResponse body = response.body();
				if (body != null && body.getItemCount() > 0) {
					int count = persister.save(body.items).getRecordCount();
					syncResult.stats.numUpdates += body.getItemCount();
					Timber.i("...saved %,d records for %,d collection items", count, body.getItemCount());
					return body.getItemCount();
				} else {
					Timber.i("...no collection items found for these games");
					return 0;
				}
			} else {
				showError(detail, response.code());
				syncResult.stats.numIoExceptions++;
				return -1;
			}
		} catch (IOException e) {
			showError(detail, e);
			syncResult.stats.numIoExceptions++;
			return -1;
		}
	}

	private boolean areGamesListsEqual(@NonNull GameList gameList1, @NonNull GameList gameList2) {
		for (Integer i : gameList1.getIdList()) {
			if (!gameList2.getIdList().contains(i)) return false;
		}
		for (Integer i : gameList2.getIdList()) {
			if (!gameList1.getIdList().contains(i)) return false;
		}
		return true;
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_collection_unupdated;
	}
}
