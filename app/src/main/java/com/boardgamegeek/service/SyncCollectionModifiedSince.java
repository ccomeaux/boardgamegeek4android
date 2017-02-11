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
import com.boardgamegeek.io.CollectionRequest;
import com.boardgamegeek.io.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;

import java.util.Date;

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
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) {
		AccountManager accountManager = AccountManager.get(context);
		long date = Authenticator.getLong(accountManager, account, SyncService.TIMESTAMP_COLLECTION_PARTIAL);

		Timber.i("Syncing collection list modified since " + new Date(date) + "...");
		try {
			CollectionPersister persister = new CollectionPersister.Builder(context)
				.includeStats()
				.includePrivateInfo()
				.validStatusesOnly()
				.build();
			ArrayMap<String, String> options = new ArrayMap<>();
			String modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(new Date(date));

			if (isCancelled()) {
				return;
			}

			updateProgressNotification(String.format("Syncing collection items modified since %s", modifiedSince));
			options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE, modifiedSince);
			requestAndPersist(account.name, persister, options, syncResult);

			if (isCancelled()) {
				return;
			}

			updateProgressNotification(String.format("Syncing collection accessories modified since %s", modifiedSince));
			options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
			requestAndPersist(account.name, persister, options, syncResult);

			Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, persister.getInitialTimestamp());
		} finally {
			Timber.i("...complete!");
		}
	}

	private void requestAndPersist(String username, @NonNull CollectionPersister persister, ArrayMap<String, String> options, @NonNull SyncResult syncResult) {
		CollectionResponse response = new CollectionRequest(service, username, options).execute();
		if (response.hasError()) {
			throw new RuntimeException("Failed to get a response from the 'Geek");
		} else if (response.getNumberOfItems() > 0) {
			int count = persister.save(response.getItems()).getRecordCount();
			syncResult.stats.numUpdates += response.getNumberOfItems();
			Timber.i("...saved %,d records for %,d collection items", count, response.getNumberOfItems());
		} else {
			Timber.i("...no new collection modifications");
		}
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_collection_partial;
	}
}
