package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
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

public class SyncPlays extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlays.class);
	private Context mContext;
	private long mStartTime;

	public SyncPlays(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing plays...");
		try {
			if (!PreferencesUtils.getSyncPlays(context)) {
				LOGI(TAG, "...plays not set to sync");
				return;
			}

			mContext = context;
			mStartTime = System.currentTimeMillis();

			PlaysResponse response = null;
			long newestSyncDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
			if (newestSyncDate > 0) {
				String date = DateTimeUtils.formatDateForApi(newestSyncDate);
				LOGI(TAG, "...syncing plays since " + date);
				int page = 1;
				do {
					LOGI(TAG, "......syncing page " + page);
					response = mService.playsByMinDate(account.name, date, page);
					PlayPersister.save(mContext, response.plays, mStartTime);
					updateTimeStamps(response);
					if (isCancelled()) {
						LOGI(TAG, "...cancelled early");
						return;
					}
					page++;
				} while (response.hasMorePages());
				deleteUnupdatedPlaysSince(newestSyncDate);
			} else {
				LOGI(TAG, "...syncing all plays");
				int page = 1;
				do {
					LOGI(TAG, "......syncing page " + page);
					response = mService.plays(account.name, page);
					PlayPersister.save(mContext, response.plays, mStartTime);
					updateTimeStamps(response);
					if (isCancelled()) {
						LOGI(TAG, "...cancelled early");
						return;
					}
					page++;
				} while (response.hasMorePages());
			}

			long oldestDate = Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
			if (oldestDate > 0) {
				String date = DateTimeUtils.formatDateForApi(oldestDate);
				LOGI(TAG, "...syncing plays before " + date);
				int page = 1;
				do {
					LOGI(TAG, "......syncing page " + page);
					response = mService.playsByMaxDate(account.name, date, page);
					PlayPersister.save(mContext, response.plays, mStartTime);
					updateTimeStamps(response);
					if (isCancelled()) {
						LOGI(TAG, "...cancelled early");
						return;
					}
					page++;
				} while (response.hasMorePages());
				deleteUnupdatedPlaysBefore(oldestDate);
				Authenticator.putLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, 0);
			}
			SyncService.hIndex(mContext);
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	private void deleteUnupdatedPlaysSince(long time) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + ">=? AND " + Plays.SYNC_STATUS + "="
			+ Play.SYNC_STATUS_SYNCED,
			new String[] { String.valueOf(mStartTime), DateTimeUtils.formatDateForApi(time) });
	}

	private void deleteUnupdatedPlaysBefore(long time) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + "<=? AND " + Plays.SYNC_STATUS + "="
			+ Play.SYNC_STATUS_SYNCED,
			new String[] { String.valueOf(mStartTime), DateTimeUtils.formatDateForApi(time) });
	}

	private void deletePlays(String selection, String[] selectionArgs) {
		int count = mContext.getContentResolver().delete(Plays.CONTENT_URI, selection, selectionArgs);
		// mSyncResult.stats.numDeletes += count;
		LOGI(TAG, "Deleted " + count + " unupdated plays");
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
