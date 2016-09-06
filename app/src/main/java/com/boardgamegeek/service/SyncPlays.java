package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class SyncPlays extends SyncTask {
	private SyncResult syncResult;
	private long startTime;
	private PlayPersister persister;

	public SyncPlays(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_PLAYS_DOWNLOAD;
	}

	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) throws IOException {
		Timber.i("Syncing plays...");
		try {
			if (!PreferencesUtils.getSyncPlays(context)) {
				Timber.i("...plays not set to sync");
				return;
			}

			this.syncResult = syncResult;
			startTime = System.currentTimeMillis();
			persister = new PlayPersister(context);
			PlaysResponse response;
			long newestSyncDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
			if (newestSyncDate <= 0) {
				int page = 1;
				do {
					response = executeCall(account.name, null, null, page);
					if (isCancelled()) {
						Timber.i("...cancelled early");
						return;
					}
					page++;
				} while (response != null && response.hasMorePages());
			} else {
				String date = DateTimeUtils.formatDateForApi(newestSyncDate);
				int page = 1;
				do {
					response = executeCall(account.name, date, null, page);
					if (isCancelled()) {
						Timber.i("...cancelled early");
						return;
					}
					page++;
				} while (response != null && response.hasMorePages());
				deleteUnupdatedPlaysSince(newestSyncDate);
			}

			long oldestDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
			if (oldestDate > 0) {
				String date = DateTimeUtils.formatDateForApi(oldestDate);
				int page = 1;
				do {
					response = executeCall(account.name, null, date, page);
					if (isCancelled()) {
						Timber.i("...cancelled early");
						return;
					}
					page++;
				} while (response != null && response.hasMorePages());
				deleteUnupdatedPlaysBefore(oldestDate);
				Authenticator.putLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, 0);
			}
			SyncService.hIndex(context);
		} finally {
			Timber.i("...complete!");
		}
	}

	private PlaysResponse executeCall(String username, String minDate, String maxDate, int page) throws IOException {
		showNotification(minDate, maxDate, page);
		Call<PlaysResponse> call = service.plays(username, minDate, maxDate, page);
		Response<PlaysResponse> response = call.execute();
		if (response.isSuccessful()) {
			persist(response.body());
			updateTimeStamps(response.body());
		} else {
			Timber.w("Unsuccessful plays fetch with code: %s", response.code());
			return null;
		}
		return response.body();
	}

	private void showNotification(String minDate, String maxDate, int page) {
		String message;
		if (TextUtils.isEmpty(minDate) && TextUtils.isEmpty(maxDate)) {
			message = "Syncing all plays";
		} else if (TextUtils.isEmpty(minDate)) {
			message = "Updating plays before " + maxDate;
		} else if (TextUtils.isEmpty(maxDate)) {
			message = "Updating plays since " + minDate;
		} else {
			message = "Updating plays between " + minDate + " and " + maxDate;
		}
		if (page > 1) {
			message = String.format("%s (page %,d)", message, page);
		}
		showNotification(message);
	}

	private void persist(@NonNull PlaysResponse response) {
		if (response.plays != null && response.plays.size() > 0) {
			if (persister == null) {
				persister = new PlayPersister(context);
			}
			persister.save(response.plays, startTime);
			syncResult.stats.numEntries += response.plays.size();
			Timber.i("...saved " + response.plays.size() + " plays");
		} else {
			Timber.i("...no plays to update");
		}
	}

	private void deleteUnupdatedPlaysSince(long time) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + ">=? AND " + Plays.SYNC_STATUS + "=" + Play.SYNC_STATUS_SYNCED,
			new String[] { String.valueOf(startTime), DateTimeUtils.formatDateForApi(time) });
	}

	private void deleteUnupdatedPlaysBefore(long time) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + "<=? AND " + Plays.SYNC_STATUS + "=" + Play.SYNC_STATUS_SYNCED,
			new String[] { String.valueOf(startTime), DateTimeUtils.formatDateForApi(time) });
	}

	private void deletePlays(String selection, String[] selectionArgs) {
		int count = context.getContentResolver().delete(Plays.CONTENT_URI, selection, selectionArgs);
		// TODO: verify this number is correct
		syncResult.stats.numDeletes += count;
		Timber.i("...deleted " + count + " unupdated plays");
	}

	private void updateTimeStamps(@NonNull PlaysResponse response) {
		long newestDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
		if (response.getNewestDate() > newestDate) {
			Authenticator.putLong(context, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, response.getNewestDate());
		}

		long oldestDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
		if (response.getOldestDate() < oldestDate) {
			Authenticator.putLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, response.getOldestDate());
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_plays;
	}
}
