package com.boardgamegeek.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SyncResult;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Syncs the user's collection modified since the date stored in the sync service, one collection status at a time.
 */
public class SyncCollectionModifiedSince extends SyncTask {
	public SyncCollectionModifiedSince(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION;
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		AccountManager accountManager = AccountManager.get(mContext);
		long date = Authenticator.getLong(accountManager, account, SyncService.TIMESTAMP_COLLECTION_PARTIAL);

		Timber.i("Syncing collection list modified since " + new Date(date) + "...");
		try {
			CollectionPersister persister = new CollectionPersister(mContext).includeStats().includePrivateInfo().validStatusesOnly();
			Map<String, String> options = new HashMap<>();
			String modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(new Date(date));

			if (isCancelled()) {
				return;
			}

			showNotification(String.format("Syncing collection items modified since %s", modifiedSince));
			options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE, modifiedSince);
			requestAndPersist(account.name, persister, options, syncResult);

			if (isCancelled()) {
				return;
			}

			showNotification(String.format("Syncing collection accessories modified since %s", modifiedSince));
			options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
			requestAndPersist(account.name, persister, options, syncResult);

			Authenticator.putLong(mContext, SyncService.TIMESTAMP_COLLECTION_PARTIAL, persister.getTimeStamp());
		} finally {
			Timber.i("...complete!");
		}
	}

	private void requestAndPersist(String username, CollectionPersister persister, Map<String, String> options,
								   SyncResult syncResult) {
		CollectionResponse response;
		response = getCollectionResponse(mService, username, options);
		if (response.items != null && response.items.size() > 0) {
			int count = persister.save(response.items);
			syncResult.stats.numUpdates += response.items.size();
			Timber.i("...saved " + count + " records for " + response.items.size() + " collection items");
		} else {
			Timber.i("...no new collection modifications");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_partial;
	}
}
