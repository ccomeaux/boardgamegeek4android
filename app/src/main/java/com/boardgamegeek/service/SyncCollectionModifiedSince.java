package com.boardgamegeek.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.BoardGameGeekService;
import com.boardgamegeek.io.CollectionRequest;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;

import java.util.Date;

import timber.log.Timber;

/**
 * Syncs the user's collection modified since the date stored in the sync service, one collection status at a time.
 */
public class SyncCollectionModifiedSince extends SyncTask {
	public SyncCollectionModifiedSince(Context context, BggService bggService, BoardGameGeekService service) {
		super(context, bggService, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) {
		AccountManager accountManager = AccountManager.get(context);
		long date = Authenticator.getLong(accountManager, account, SyncService.TIMESTAMP_COLLECTION_PARTIAL);

		Timber.i("Syncing collection list modified since " + new Date(date) + "...");
		try {
			CollectionPersister persister = new CollectionPersister(context).includeStats().includePrivateInfo().validStatusesOnly();
			ArrayMap<String, String> options = new ArrayMap<>();
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

			Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, persister.getInitialTimestamp());
		} finally {
			Timber.i("...complete!");
		}
	}

	private void requestAndPersist(String username, @NonNull CollectionPersister persister, ArrayMap<String, String> options, @NonNull SyncResult syncResult) {
		CollectionResponse response;
		response = new CollectionRequest(bggService, username, options).execute();
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
