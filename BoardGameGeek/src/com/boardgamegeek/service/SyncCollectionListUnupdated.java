package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.ResolverUtils;

/**
 * Syncs a limited number of collection items that have not yet been updated completely.
 */
public class SyncCollectionListUnupdated extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionListUnupdated.class);
	private static final int GAME_PER_FETCH = 25;

	public SyncCollectionListUnupdated(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing unupdated collection list...");
		try {
			int numberOfFetches = 0;
			CollectionPersister persister = new CollectionPersister(context).includePrivateInfo().includeStats();
			Map<String, String> options = new HashMap<String, String>();
			options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");

			do {
				if (isCancelled()) {
					break;
				}
				numberOfFetches++;
				List<Integer> gameIds = ResolverUtils.queryInts(context.getContentResolver(), Collection.CONTENT_URI,
					Collection.GAME_ID, "collection." + Collection.UPDATED + "=0 OR collection." + Collection.UPDATED
						+ " IS NULL", null, "collection." + Collection.UPDATED_LIST + " DESC LIMIT " + GAME_PER_FETCH);
				if (gameIds.size() > 0) {
					LOGI(TAG, "...found " + gameIds.size() + " games to update [" + TextUtils.join(", ", gameIds) + "]");

					options.put(BggService.COLLECTION_QUERY_KEY_ID, TextUtils.join(",", gameIds));
					options.remove(BggService.COLLECTION_QUERY_KEY_SUBTYPE);
					requestAndPersist(account.name, persister, options, syncResult);

					options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
					requestAndPersist(account.name, persister, options, syncResult);
				} else {
					LOGI(TAG, "...no more unupdated collection items");
					break;
				}
			} while (numberOfFetches < 100);
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	private void requestAndPersist(String username, CollectionPersister persister, Map<String, String> options,
		SyncResult syncResult) {
		// TODO games with a status of played don't get returned with this request
		CollectionResponse response = getCollectionResponse(mService, username, options);
		if (response.items != null && response.items.size() > 0) {
			int count = persister.save(response.items);
			syncResult.stats.numUpdates += response.items.size();
			LOGI(TAG, "...saved " + count + " records for " + response.items.size() + " collection items");
		} else {
			LOGI(TAG, "...no collection items found for these games");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_unupdated;
	}
}
