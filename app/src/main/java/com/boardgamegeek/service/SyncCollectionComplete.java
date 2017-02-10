package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.CollectionRequest;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.PreferencesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

/**
 * Syncs the user's collection in brief mode, one collection status at a time.
 */
public class SyncCollectionComplete extends SyncTask {
	private List<String> statuses;

	public SyncCollectionComplete(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) {
		Timber.i("Syncing full collection list...");
		try {
			CollectionPersister persister = new CollectionPersister.Builder(context).brief().build();

			statuses = getSyncableStatuses();

			for (int i = 0; i < statuses.size(); i++) {
				if (isCancelled()) {
					Timber.i("...cancelled");
					return;
				}

				String status = statuses.get(i);
				if (TextUtils.isEmpty(status)) {
					Timber.i("...skipping blank status");
					continue;
				}
				Timber.i("...syncing status [" + status + "]");

				updateProgressNotification(context.getString(R.string.sync_notification_collection_items, status));
				ArrayMap<String, String> options = createOptions(i, status);
				requestAndPersist(account.name, persister, options, syncResult);

				updateProgressNotification(context.getString(R.string.sync_notification_collection_accessories, status));
				options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
				requestAndPersist(account.name, persister, options, syncResult);
			}

			final long initialTimestamp = persister.getInitialTimestamp();
			deleteUnusedItems(initialTimestamp);
			updateTimestamps(initialTimestamp);
		} finally {
			Timber.i("...complete!");
		}
	}

	@NonNull
	private List<String> getSyncableStatuses() {
		List<String> statuses = new ArrayList<>(Arrays.asList(PreferencesUtils.getSyncStatuses(context)));
		// Played games should be synced first - they don't respect the "exclude" flag
		if (statuses.remove(BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
			statuses.add(0, BggService.COLLECTION_QUERY_STATUS_PLAYED);
		}
		return statuses;
	}

	@NonNull
	private ArrayMap<String, String> createOptions(int i, String status) {
		ArrayMap<String, String> options = new ArrayMap<>();
		options.put(BggService.COLLECTION_QUERY_KEY_BRIEF, "1");
		options.put(status, "1");
		for (int j = 0; j < i; j++) {
			options.put(statuses.get(j), "0");
		}
		return options;
	}

	private void requestAndPersist(String username, @NonNull CollectionPersister persister, ArrayMap<String, String> options, @NonNull SyncResult syncResult) {
		CollectionResponse response = new CollectionRequest(service, username, options).execute();
		if (response.items != null && response.items.size() > 0) {
			int rows = persister.save(response.items);
			syncResult.stats.numEntries += response.items.size();
			Timber.i("...saved " + rows + " records for " + response.items.size() + " collection items");
		} else {
			Timber.i("...no collection items to save");
		}
	}

	private void deleteUnusedItems(long initialTimestamp) {
		Timber.i("...deleting old collection entries");
		int count = context.getContentResolver().delete(
			Collection.CONTENT_URI,
			Collection.UPDATED_LIST + "<?",
			new String[] { String.valueOf(initialTimestamp) });
		Timber.i("...deleted " + count + " old collection entries");
		// TODO: delete games as well?!
		// TODO: delete thumbnail images associated with this list (both collection and game)
	}

	private void updateTimestamps(long initialTimestamp) {
		Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE, initialTimestamp);
		Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, initialTimestamp);
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_collection_full;
	}
}
