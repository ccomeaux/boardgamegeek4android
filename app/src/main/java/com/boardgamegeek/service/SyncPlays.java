package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;

import timber.log.Timber;

public class SyncPlays extends SyncTask {
	private PlayPersister mPersister;
	private long mStartTime;

	public SyncPlays(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		Timber.i("Syncing plays...");
		try {
			if (!PreferencesUtils.getSyncPlays(mContext)) {
				Timber.i("...plays not set to sync");
				return;
			}

			mStartTime = System.currentTimeMillis();
			mPersister = new PlayPersister(mContext);
			PlaysResponse response = null;
			long newestSyncDate = Authenticator.getLong(mContext, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
			if (newestSyncDate > 0) {
				String date = DateTimeUtils.formatDateForApi(newestSyncDate);
				Timber.i("...syncing plays since " + date);
				int page = 1;
				do {
					Timber.i("......syncing page " + page);
					showNotification(paginateDetail("Updating plays since " + date, page));

					response = mService.playsByMinDate(account.name, date, page);
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

					response = mService.plays(account.name, page);
					persist(response, syncResult);
					updateTimeStamps(response);
					if (isCancelled()) {
						Timber.i("...cancelled early");
						return;
					}
					page++;
				} while (response.hasMorePages());
			}

			long oldestDate = Authenticator.getLong(mContext, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
			if (oldestDate > 0) {
				String date = DateTimeUtils.formatDateForApi(oldestDate);
				Timber.i("...syncing plays before " + date);
				int page = 1;
				do {
					Timber.i("......syncing page " + page);
					showNotification(paginateDetail("Updating plays before " + date, page));

					response = mService.playsByMaxDate(account.name, date, page);
					persist(response, syncResult);
					updateTimeStamps(response);
					if (isCancelled()) {
						Timber.i("...cancelled early");
						return;
					}
					page++;
				} while (response.hasMorePages());
				deleteUnupdatedPlaysBefore(oldestDate, syncResult);
				Authenticator.putLong(mContext, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, 0);
			}
			SyncService.hIndex(mContext);
		} finally {
			Timber.i("...complete!");
		}
	}

	protected String paginateDetail(String detail, int page) {
		if (page > 1) {
			return detail + " (page " + page + ")";
		}
		return detail;
	}

	private void persist(PlaysResponse response, SyncResult syncResult) {
		if (response.plays != null && response.plays.size() > 0) {
			mPersister.save(response.plays, mStartTime);
			syncResult.stats.numEntries += response.plays.size();
			Timber.i("...saved " + response.plays);
		} else {
			Timber.i("...no plays to update");
		}
	}

	private void deleteUnupdatedPlaysSince(long time, SyncResult syncResult) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + ">=? AND " + Plays.SYNC_STATUS + "="
			+ Play.SYNC_STATUS_SYNCED,
			new String[] { String.valueOf(mStartTime), DateTimeUtils.formatDateForApi(time) }, syncResult);
	}

	private void deleteUnupdatedPlaysBefore(long time, SyncResult syncResult) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + "<=? AND " + Plays.SYNC_STATUS + "="
			+ Play.SYNC_STATUS_SYNCED,
			new String[] { String.valueOf(mStartTime), DateTimeUtils.formatDateForApi(time) }, syncResult);
	}

	private void deletePlays(String selection, String[] selectionArgs, SyncResult syncResult) {
		int count = mContext.getContentResolver().delete(Plays.CONTENT_URI, selection, selectionArgs);
		// TODO: verify this number is correct
		syncResult.stats.numDeletes += count;
		Timber.i("...deleted " + count + " unupdated plays");
	}

	private void updateTimeStamps(PlaysResponse response) {
		long newestDate = Authenticator.getLong(mContext, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
		if (response.getNewestDate() > newestDate) {
			Authenticator.putLong(mContext, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, response.getNewestDate());
		}

		long oldestDate = Authenticator.getLong(mContext, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
		if (response.getOldestDate() < oldestDate) {
			Authenticator.putLong(mContext, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, response.getOldestDate());
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_plays;
	}
}
