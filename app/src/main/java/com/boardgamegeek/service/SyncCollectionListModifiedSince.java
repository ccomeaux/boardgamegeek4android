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

	public SyncCollectionListModifiedSince(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		AccountManager accountManager = AccountManager.get(mContext);
		long date = Authenticator.getLong(accountManager, account, SyncService.TIMESTAMP_COLLECTION_PARTIAL);

		LOGI(TAG, "Syncing collection list modified since " + new Date(date) + "...");
		try {
			CollectionPersister persister = new CollectionPersister(mContext).includeStats().includePrivateInfo();
			Map<String, String> options = new HashMap<String, String>();
			String modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(new Date(date));

			boolean cancelled = false;
			String[] statuses = PreferencesUtils.getSyncStatuses(mContext);
			for (int i = 0; i < statuses.length; i++) {
				if (isCancelled()) {
					cancelled = true;
					break;
				}
				String status = statuses[i];
				LOGI(TAG, "...syncing status [" + status + "]");
				showNotification(String.format("Syncing %1$s collection items since %2$s", status, modifiedSince));

				options.clear();
				options.put(status, "1");
				for (int j = 0; j < i; j++) {
					options.put(statuses[j], "0");
				}
				options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
				options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
				options.put(BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE, modifiedSince);
				requestAndPersist(account.name, persister, options, syncResult);

				showNotification(String.format("Syncing %1$s collection accessories since %2$s", status, modifiedSince));
				options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
				requestAndPersist(account.name, persister, options, syncResult);
			}
			if (!cancelled) {
				Authenticator.putLong(mContext, SyncService.TIMESTAMP_COLLECTION_PARTIAL, persister.getTimeStamp());
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	private void requestAndPersist(String username, CollectionPersister persister, Map<String, String> options,
		SyncResult syncResult) {
		CollectionResponse response;
		response = getCollectionResponse(mService, username, options);
		if (response.items != null && response.items.size() > 0) {
			int count = persister.save(response.items);
			syncResult.stats.numUpdates += response.items.size();
			LOGI(TAG, "...saved " + count + " records for " + response.items.size() + " collection items");
		} else {
			LOGI(TAG, "...no new collection modifications");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_partial;
	}
}
