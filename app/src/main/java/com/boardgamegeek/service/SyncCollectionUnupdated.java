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
import com.boardgamegeek.util.StringUtils;

/**
 * Syncs a limited number of collection items that have not yet been updated completely.
 */
public class SyncCollectionUnupdated extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionUnupdated.class);
	private static final int GAME_PER_FETCH = 25;

	public SyncCollectionUnupdated(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing unupdated collection list...");
		try {
			int numberOfFetches = 0;
			CollectionPersister persister = new CollectionPersister(mContext).includePrivateInfo().includeStats();
			Map<String, String> options = new HashMap<String, String>();
			options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");

			do {
				if (isCancelled()) {
					break;
				}
				numberOfFetches++;
				List<Integer> gameIds = ResolverUtils.queryInts(mContext.getContentResolver(), Collection.CONTENT_URI,
					Collection.GAME_ID, "collection." + Collection.UPDATED + "=0 OR collection." + Collection.UPDATED
						+ " IS NULL AND " + Collection.COLLECTION_ID + " IS NOT NULL", null, "collection."
						+ Collection.UPDATED_LIST + " DESC LIMIT " + GAME_PER_FETCH);
				if (gameIds.size() > 0) {
					String gameIdDescription = StringUtils.formatList(gameIds);
					LOGI(TAG, "...found " + gameIds.size() + " games to update [" + gameIdDescription + "]");
					String detail = gameIds.size() + " games: " + gameIdDescription;
					if (numberOfFetches > 1) {
						detail += " (page " + numberOfFetches + ")";
					}
					showNotification(detail);

					options.put(BggService.COLLECTION_QUERY_KEY_ID, TextUtils.join(",", gameIds));
					options.remove(BggService.COLLECTION_QUERY_KEY_SUBTYPE);
					boolean success = requestAndPersist(account.name, persister, options, syncResult);

					options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
					success |= requestAndPersist(account.name, persister, options, syncResult);

					if (!success) {
						break;
					}
				} else {
					LOGI(TAG, "...no more unupdated collection items");
					break;
				}
			} while (numberOfFetches < 100);
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	private boolean requestAndPersist(String username, CollectionPersister persister, Map<String, String> options,
									  SyncResult syncResult) {
		CollectionResponse response = getCollectionResponse(mService, username, options);
		if (response.items != null && response.items.size() > 0) {
			int count = persister.save(response.items);
			syncResult.stats.numUpdates += response.items.size();
			LOGI(TAG, "...saved " + count + " records for " + response.items.size() + " collection items");
			return true;
		} else {
			LOGI(TAG, "...no collection items found for these games");
			return false;
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_unupdated;
	}
}
