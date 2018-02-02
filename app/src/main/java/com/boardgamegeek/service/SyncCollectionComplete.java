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
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.PreferencesUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Syncs the user's complete collection in brief mode, one collection status at a time, deleting all items from the local
 * database that weren't synced.
 */
public class SyncCollectionComplete extends SyncTask {
	private List<String> statuses;
	private String[] statusEntries;
	private String[] statusValues;
	private SyncResult syncResult;
	private String username;
	private CollectionPersister persister;

	@DebugLog
	public SyncCollectionComplete(Context context, BggService service) {
		super(context, service);
	}

	@DebugLog
	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@DebugLog
	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) {
		this.syncResult = syncResult;
		this.username = account.name;
		Timber.i("Syncing full collection list...");
		try {
			persister = new CollectionPersister.Builder(context)
				.includePrivateInfo()
				.includeStats()
				.build();

			statusEntries = context.getResources().getStringArray(R.array.pref_sync_status_entries);
			statusValues = context.getResources().getStringArray(R.array.pref_sync_status_values);
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
				Timber.i("...syncing status [%s]", status);

				String statusDescription = getStatusDescription(status);

				if (i > 0) if (wasSleepInterrupted(5000)) return;

				ArrayMap<String, String> options = createOptions(i, status);
				fetchAndPersist(context.getString(R.string.sync_notification_collection_items, statusDescription), options, "items");

				if (isCancelled()) {
					Timber.i("...cancelled");
					return;
				}

				if (wasSleepInterrupted(2000)) return;

				options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
				fetchAndPersist(context.getString(R.string.sync_notification_collection_accessories, statusDescription), options, "accessories");
			}

			if (isCancelled()) {
				Timber.i("...cancelled");
				return;
			}

			final long initialTimestamp = persister.getInitialTimestamp();
			deleteUnusedItems(initialTimestamp);
			updateTimestamps(initialTimestamp);
		} finally {
			Timber.i("...complete!");
		}
	}

	private void fetchAndPersist(String detail, ArrayMap<String, String> options, final String type) {
		updateProgressNotification(detail);
		Call<CollectionResponse> call = service.collection(username, options);
		try {
			Response<CollectionResponse> response = call.execute();
			if (response.isSuccessful()) {
				CollectionResponse body = response.body();
				if (body != null && body.getItemCount() > 0) {
					int count = persister.save(body.items).getRecordCount();
					syncResult.stats.numUpdates += body.getItemCount();
					Timber.i("...saved %,d records for %,d collection %s", count, body.getItemCount(), type);
				} else {
					Timber.i("...no collection %s found for these games", type);
				}
			} else {
				showError(detail, response.code());
				syncResult.stats.numIoExceptions++;
				cancel();
			}
		} catch (IOException e) {
			showError(detail, e);
			syncResult.stats.numIoExceptions++;
			cancel();
		}
	}

	@DebugLog
	@NonNull
	private List<String> getSyncableStatuses() {
		List<String> statuses = new ArrayList<>(PreferencesUtils.getSyncStatuses(context));
		// Played games should be synced first - they don't respect the "exclude" flag
		if (statuses.remove(BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
			statuses.add(0, BggService.COLLECTION_QUERY_STATUS_PLAYED);
		}
		return statuses;
	}

	@DebugLog
	private String getStatusDescription(String status) {
		for (int i = 0; i < statusEntries.length; i++) {
			if (statusValues[i].equalsIgnoreCase(status)) {
				return statusEntries[i];
			}
		}
		return status;
	}

	@DebugLog
	@NonNull
	private ArrayMap<String, String> createOptions(int i, String status) {
		ArrayMap<String, String> options = new ArrayMap<>();
		options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
		options.put(status, "1");
		for (int j = 0; j < i; j++) {
			options.put(statuses.get(j), "0");
		}
		return options;
	}

	@DebugLog
	private void deleteUnusedItems(long initialTimestamp) {
		Timber.i("...deleting old collection entries");
		int count = context.getContentResolver().delete(
			Collection.CONTENT_URI,
			Collection.UPDATED_LIST + "<?",
			new String[] { String.valueOf(initialTimestamp) });
		Timber.i("...deleted %,d old collection entries", count);
		// TODO: delete thumbnail images associated with this list (both collection and game)
	}

	@DebugLog
	private void updateTimestamps(long initialTimestamp) {
		Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE, initialTimestamp);
		Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, initialTimestamp);
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_collection_full;
	}
}
