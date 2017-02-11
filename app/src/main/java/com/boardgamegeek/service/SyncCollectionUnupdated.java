package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.CollectionRequest;
import com.boardgamegeek.io.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.List;

import timber.log.Timber;

/**
 * Syncs a limited number of collection items that have not yet been updated completely.
 */
public class SyncCollectionUnupdated extends SyncTask {
	private static final int GAME_PER_FETCH = 25;

	public SyncCollectionUnupdated(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) {
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

			do {
				if (isCancelled()) {
					break;
				}
				numberOfFetches++;
				List<Integer> gameIds = ResolverUtils.queryInts(context.getContentResolver(),
					Collection.CONTENT_URI,
					Collection.GAME_ID,
					"(collection." + Collection.UPDATED + "=0 OR collection." + Collection.UPDATED + " IS NULL) AND " + Collection.COLLECTION_ID + " IS NOT NULL",
					null,
					"collection." + Collection.UPDATED_LIST + " DESC LIMIT " + GAME_PER_FETCH);
				if (gameIds.size() > 0) {
					String gameIdDescription = StringUtils.formatList(gameIds);
					Timber.i("...found " + gameIds.size() + " games to update [" + gameIdDescription + "]");
					String detail = gameIds.size() + " games: " + gameIdDescription;
					if (numberOfFetches > 1) {
						detail += " (page " + numberOfFetches + ")";
					}
					updateProgressNotification(detail);

					options.put(BggService.COLLECTION_QUERY_KEY_ID, TextUtils.join(",", gameIds));
					options.remove(BggService.COLLECTION_QUERY_KEY_SUBTYPE);
					boolean success = requestAndPersist(account.name, persister, options, syncResult);

					options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
					success |= requestAndPersist(account.name, persister, options, syncResult);

					if (!success) {
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

	private boolean requestAndPersist(String username, @NonNull CollectionPersister persister, ArrayMap<String, String> options, @NonNull SyncResult syncResult) {
		Timber.i("..requesting collection items with options %s", options);
		CollectionResponse response = new CollectionRequest(service, username, options).execute();
		if (response.hasError()) {
			Timber.w(response.getError());
			return false;
		} else if (response.getNumberOfItems() > 0) {
			int count = persister.save(response.getItems()).getRecordCount();
			syncResult.stats.numUpdates += response.getNumberOfItems();
			Timber.i("...saved %,d records for %,d collection items", count, response.getNumberOfItems());
			return true;
		} else {
			Timber.i("...no collection items found for these games");
			return false;
		}
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_collection_unupdated;
	}
}
