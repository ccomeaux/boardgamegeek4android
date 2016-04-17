package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.BoardGameGeekService;
import com.boardgamegeek.io.PlaysRequest;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;

import timber.log.Timber;

public class SyncPlays extends SyncTask {
	private PlayPersister persister;
	private long startTime;

	public SyncPlays(Context context, BggService bggService, BoardGameGeekService service) {
		super(context, bggService, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_PLAYS_DOWNLOAD;
	}

	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) {
		Timber.i("Syncing plays...");
		try {
			if (!PreferencesUtils.getSyncPlays(context)) {
				Timber.i("...plays not set to sync");
				return;
			}

			startTime = System.currentTimeMillis();
			persister = new PlayPersister(context);
			PlaysResponse response;
			long newestSyncDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
			if (newestSyncDate > 0) {
				String date = DateTimeUtils.formatDateForApi(newestSyncDate);
				Timber.i("...syncing plays since " + date);
				int page = 1;
				do {
					Timber.i("......syncing page " + page);
					showNotification(paginateDetail("Updating plays since " + date, page));

					response = new PlaysRequest(service, PlaysRequest.TYPE_MIN, account.name, page, date).execute();
					persist(response, syncResult);
					updateTimeStamps(response);
					if (isCancelled()) {
						Timber.i("...cancelled early");
						return;
					}
					page++;
				} while (response.hasMorePages());
				deleteUnupdatedPlaysSince(newestSyncDate, syncResult);
			} else {
				Timber.i("...syncing all plays");
				int page = 1;
				do {
					Timber.i("......syncing page " + page);
					showNotification(paginateDetail("Updating all plays", page));

					response = new PlaysRequest(service, account.name, page).execute();
					persist(response, syncResult);
					updateTimeStamps(response);
					if (isCancelled()) {
						Timber.i("...cancelled early");
						return;
					}
					page++;
				} while (response.hasMorePages());
			}

			long oldestDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
			if (oldestDate > 0) {
				String date = DateTimeUtils.formatDateForApi(oldestDate);
				Timber.i("...syncing plays before " + date);
				int page = 1;
				do {
					Timber.i("......syncing page " + page);
					showNotification(paginateDetail("Updating plays before " + date, page));

					response = new PlaysRequest(service, PlaysRequest.TYPE_MAX, account.name, page, date).execute();
					persist(response, syncResult);
					updateTimeStamps(response);
					if (isCancelled()) {
						Timber.i("...cancelled early");
						return;
					}
					page++;
				} while (response.hasMorePages());
				deleteUnupdatedPlaysBefore(oldestDate, syncResult);
				Authenticator.putLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, 0);
			}
			SyncService.hIndex(context);
		} finally {
			Timber.i("...complete!");
		}
	}

	private String paginateDetail(String detail, int page) {
		if (page > 1) {
			return detail + " (page " + page + ")";
		}
		return detail;
	}

	private void persist(@NonNull PlaysResponse response, @NonNull SyncResult syncResult) {
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

	private void deleteUnupdatedPlaysSince(long time, @NonNull SyncResult syncResult) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + ">=? AND " + Plays.SYNC_STATUS + "="
				+ Play.SYNC_STATUS_SYNCED,
			new String[] { String.valueOf(startTime), DateTimeUtils.formatDateForApi(time) }, syncResult);
	}

	private void deleteUnupdatedPlaysBefore(long time, @NonNull SyncResult syncResult) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + "<=? AND " + Plays.SYNC_STATUS + "="
				+ Play.SYNC_STATUS_SYNCED,
			new String[] { String.valueOf(startTime), DateTimeUtils.formatDateForApi(time) }, syncResult);
	}

	private void deletePlays(String selection, String[] selectionArgs, @NonNull SyncResult syncResult) {
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
