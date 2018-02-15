package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

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
	@NonNull private final Account account;
	private String[] statusEntries;
	private String[] statusValues;
	private CollectionPersister persister;

	@DebugLog
	public SyncCollectionComplete(Context context, BggService service, @NonNull SyncResult syncResult, @NonNull Account account) {
		super(context, service, syncResult);
		this.account = account;
	}

	@DebugLog
	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@DebugLog
	@Override
	public void execute() {
		Timber.i("Syncing full collection list...");
		try {
			persister = new CollectionPersister.Builder(context)
				.includePrivateInfo()
				.includeStats()
				.build();

			statusEntries = context.getResources().getStringArray(R.array.pref_sync_status_entries);
			statusValues = context.getResources().getStringArray(R.array.pref_sync_status_values);

			List<String> statuses = getSyncableStatuses();
			for (int i = 0; i < statuses.size(); i++) {
				if (isCancelled()) {
					Timber.i("...cancelled");
					return;
				}
				if (i > 0) {
					updateProgressNotification(context.getString(R.string.sync_notification_sleep));
					if (wasSleepInterrupted(5000)) return;
				}

				List<String> excludedStatuses = new ArrayList<>();
				for (int j = 0; j < i; j++) {
					excludedStatuses.add(statuses.get(j));
				}
				syncByStatus(statuses.get(i), excludedStatuses.toArray(new String[excludedStatuses.size()]));
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

	private void syncByStatus(String status, String... excludedStatuses) {
		if (TextUtils.isEmpty(status)) {
			Timber.i("...skipping blank status");
			return;
		}
		Timber.i("...syncing status [%s]", status);
		Timber.i("...while excluding statuses [%s]", StringUtils.formatList(excludedStatuses));

		String statusDescription = getStatusDescription(status);

		ArrayMap<String, String> options = new ArrayMap<>();
		options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
		options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
		options.put(status, "1");
		for (String excludedStatus : excludedStatuses) {
			options.put(excludedStatus, "0");
		}

		fetchAndPersist(options, statusDescription, context.getString(R.string.items), excludedStatuses.length > 0);

		if (isCancelled()) {
			Timber.i("...cancelled");
			return;
		}
		updateProgressNotification(context.getString(R.string.sync_notification_sleep));
		if (wasSleepInterrupted(2000)) return;

		options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
		fetchAndPersist(options, statusDescription, context.getString(R.string.accessories), excludedStatuses.length > 0);
	}

	private void fetchAndPersist(ArrayMap<String, String> options, String statusDescription, String type, boolean hasExclusions) {
		@StringRes int downloadingResId = hasExclusions ?
			R.string.sync_notification_collection_downloading_exclusions :
			R.string.sync_notification_collection_downloading;
		updateProgressNotification(context.getString(downloadingResId, statusDescription, type));
		Call<CollectionResponse> call = service.collection(account.name, options);
		try {
			Response<CollectionResponse> response = call.execute();
			if (response.isSuccessful()) {
				CollectionResponse body = response.body();
				if (body != null && body.getItemCount() > 0) {
					@StringRes int savingResId = hasExclusions ?
						R.string.sync_notification_collection_saving_exclusions :
						R.string.sync_notification_collection_saving;
					updateProgressNotification(context.getString(savingResId, body.getItemCount(), statusDescription, type));
					int count = persister.save(body.items).getRecordCount();
					syncResult.stats.numUpdates += body.getItemCount();
					Timber.i("...saved %,d records for %,d collection %s", count, body.getItemCount(), type);
				} else {
					Timber.i("...no collection %s found for these games", type);
				}
			} else {
				showError(context.getString(R.string.sync_notification_collection_detail, statusDescription, type), response.code());
				syncResult.stats.numIoExceptions++;
				cancel();
			}
		} catch (IOException e) {
			showError(context.getString(R.string.sync_notification_collection_detail, statusDescription, type), e);
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
