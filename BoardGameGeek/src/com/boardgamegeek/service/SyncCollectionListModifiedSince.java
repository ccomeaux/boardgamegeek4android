package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.util.PreferencesUtils;

/**
 * Syncs the user's collection modified since the date stored in the sync service, one collection status at a time.
 */
public class SyncCollectionListModifiedSince extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionListModifiedSince.class);

	public SyncCollectionListModifiedSince(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		AccountManager accountManager = AccountManager.get(context);
		long date = getLong(account, accountManager, SyncService.TIMESTAMP_COLLECTION_PARTIAL);

		LOGI(TAG, "Syncing collection list modified since " + new Date(date) + "...");
		try {
			CollectionPersister persister = new CollectionPersister(context).includeStats();
			Map<String, String> options = new HashMap<String, String>();
			String modifiedSince = BggService.COLLECTION_QUERY_DATE_FORMAT.format(new Date(date));

			boolean cancelled = false;
			String[] statuses = PreferencesUtils.getSyncStatuses(context);
			for (int i = 0; i < statuses.length; i++) {
				if (isCancelled()) {
					cancelled = true;
					break;
				}
				LOGI(TAG, "...syncing status [" + statuses[i] + "]");

				options.clear();
				options.put(statuses[i], "1");
				options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
				options.put(BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE, modifiedSince);

				CollectionResponse response = getCollectionResponse(mService, account.name, options);
				int itemCount = 0;
				if (response.items != null) {
					itemCount = response.items.size();
					int count = persister.save(response.items);
					// syncResult.stats
					LOGI(TAG, "...saved " + count + " rows for " + itemCount + " collection items");
				} else {
					LOGI(TAG, "...no new collection modifications");
				}
			}
			if (!cancelled) {
				Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, persister.getTimeStamp());
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_partial;
	}

	private long getLong(Account account, AccountManager accountManager, String key) {
		String l = accountManager.getUserData(account, key);
		return TextUtils.isEmpty(l) ? 0 : Long.parseLong(l);
	}
}
