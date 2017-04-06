package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.tasks.CalculatePlayStatsTask;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.SelectionBuilder;
import com.boardgamegeek.util.TaskUtils;

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

			long newestSyncDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
			if (newestSyncDate <= 0) {
				if (executeCall(account.name, null, null)) return;
			} else {
				String date = DateTimeUtils.formatDateForApi(newestSyncDate);
				if (executeCall(account.name, date, null)) return;
				deleteUnupdatedPlaysSince(newestSyncDate);
			}

			long oldestDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
			if (oldestDate > 0) {
				String date = DateTimeUtils.formatDateForApi(oldestDate);
				if (executeCall(account.name, null, date)) return;
				deleteUnupdatedPlaysBefore(oldestDate);
				Authenticator.putLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, 0);
			}
			TaskUtils.executeAsyncTask(new CalculatePlayStatsTask(context));
		} finally {
			Timber.i("...complete!");
		}
	}

	private boolean executeCall(String username, String minDate, String maxDate) {
		Response<PlaysResponse> response;
		int page = 1;
		do {
			if (isCancelled()) {
				Timber.i("...cancelled early");
				return true;
			}

			if (page != 1) if (wasSleepInterrupted(3000)) return true;

			showNotification(minDate, maxDate, page);
			Call<PlaysResponse> call = service.plays(username, minDate, maxDate, page);
			try {
				response = call.execute();
				if (!response.isSuccessful()) {
					showError(String.format("Unsuccessful plays fetch with code: %s", response.code()));
					syncResult.stats.numIoExceptions++;
					return true;
				}
			} catch (IOException e) {
				showError(String.format("Unsuccessful plays fetch with exception: %s", e.getLocalizedMessage()));
				syncResult.stats.numIoExceptions++;
				return true;
			}
			persist(response.body());
			updateTimestamps(response.body());
			page++;
		} while (response.body().hasMorePages());
		return false;
	}

	private void showNotification(String minDate, String maxDate, int page) {
		String message;
		if (TextUtils.isEmpty(minDate) && TextUtils.isEmpty(maxDate)) {
			message = context.getString(R.string.sync_notification_plays_all);
		} else if (TextUtils.isEmpty(minDate)) {
			message = context.getString(R.string.sync_notification_plays_old, maxDate);
		} else if (TextUtils.isEmpty(maxDate)) {
			message = context.getString(R.string.sync_notification_plays_new, minDate);
		} else {
			message = context.getString(R.string.sync_notification_plays_between, minDate, maxDate);
		}
		if (page > 1) {
			message = context.getString(R.string.sync_notification_page_suffix, message, page);
		}
		updateProgressNotification(message);
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
		deleteUnupdatedPlays(time, ">=");
	}

	private void deleteUnupdatedPlaysBefore(long time) {
		deleteUnupdatedPlays(time, "<=");
	}

	private void deleteUnupdatedPlays(long time, final String dateComparator) {
		deletePlays(Plays.SYNC_TIMESTAMP + "<? AND " + Plays.DATE + dateComparator + "? AND " +
				SelectionBuilder.whereZeroOrNull(Plays.UPDATE_TIMESTAMP) + " AND " +
				SelectionBuilder.whereZeroOrNull(Plays.DELETE_TIMESTAMP) + " AND " +
				SelectionBuilder.whereZeroOrNull(Plays.DIRTY_TIMESTAMP),
			new String[] { String.valueOf(startTime), DateTimeUtils.formatDateForApi(time) });
	}

	private void deletePlays(String selection, String[] selectionArgs) {
		int count = context.getContentResolver().delete(Plays.CONTENT_URI, selection, selectionArgs);
		syncResult.stats.numDeletes += count;
		Timber.i("...deleted %,d unupdated plays", count);
	}

	private void updateTimestamps(@NonNull PlaysResponse response) {
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
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_plays;
	}
}
