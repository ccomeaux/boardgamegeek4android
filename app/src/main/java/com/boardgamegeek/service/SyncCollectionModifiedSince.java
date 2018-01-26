package com.boardgamegeek.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;

import java.io.IOException;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Syncs the user's collection modified since the date stored in the sync service.
 */
public class SyncCollectionModifiedSince extends SyncTask {
	private SyncResult syncResult;
	private String username;
	private CollectionPersister persister;

	public SyncCollectionModifiedSince(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) {
		this.syncResult = syncResult;
		this.username = account.name;
		AccountManager accountManager = AccountManager.get(context);

		try {
			if (isCancelled()) {
				Timber.i("...cancelled");
				return;
			}

			persister = new CollectionPersister.Builder(context)
				.includeStats()
				.includePrivateInfo()
				.validStatusesOnly()
				.build();

			long date = Authenticator.getLong(accountManager, account, SyncService.TIMESTAMP_COLLECTION_PARTIAL);
			String modifiedSince = BggService.COLLECTION_QUERY_DATE_TIME_FORMAT.format(new Date(date));
			final String formattedDateTime = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);

			ArrayMap<String, String> options = new ArrayMap<>();
			options.put(BggService.COLLECTION_QUERY_KEY_STATS, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_SHOW_PRIVATE, "1");
			options.put(BggService.COLLECTION_QUERY_KEY_MODIFIED_SINCE, modifiedSince);
			fetchAndPersist(context.getString(R.string.sync_notification_collection_items_since, formattedDateTime), options, "items");

			if (isCancelled()) {
				Timber.i("...cancelled");
				return;
			}
			if (wasSleepInterrupted(2000)) return;

			options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
			fetchAndPersist(context.getString(R.string.sync_notification_collection_accessories_since, formattedDateTime), options, "accessories");

			Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, persister.getInitialTimestamp());
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
					Timber.i("...no new collection %s modifications", type);
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

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_collection_partial;
	}
}
