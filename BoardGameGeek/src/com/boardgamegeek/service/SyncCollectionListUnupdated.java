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
			List<Integer> gameIds = ResolverUtils.queryInts(context.getContentResolver(), Collection.CONTENT_URI,
				Collection.GAME_ID, "collection." + Collection.UPDATED + "=0 OR collection." + Collection.UPDATED
					+ " IS NULL", null, Collection.COLLECTION_ID + " LIMIT " + GAME_PER_FETCH);
			LOGI(TAG, "...found " + gameIds.size() + " collection items to update");
			if (gameIds.size() > 0) {
				CollectionPersister persister = new CollectionPersister(context).includePrivateInfo().includeStats();

				Map<String, String> options = new HashMap<String, String>();
				options.put(BggService.COLLECTION_QUERY_KEY_ID, TextUtils.join(",", gameIds));
				options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
				options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");

				CollectionResponse response = getCollectionResponse(mService, account.name, options);
				persister.save(response.items);
				// TODO games with a status of played don't get returned with this request
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_unupdated;
	}
}
