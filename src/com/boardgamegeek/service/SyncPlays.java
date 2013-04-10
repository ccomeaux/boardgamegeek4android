package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SyncResult;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePlaysHandler;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.PlaysUrlBuilder;
import com.boardgamegeek.util.PreferencesUtils;

public class SyncPlays extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlays.class);

	private RemoteExecutor mExecutor;
	private Context mContext;
	private long mStartTime;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing plays...");
		try {
			if (!PreferencesUtils.getSyncPlays(executor.getContext())) {
				LOGI(TAG, "...plays not set to sync");
				return;
			}

			mExecutor = executor;
			mContext = executor.getContext();
			mStartTime = System.currentTimeMillis();

			AccountManager accountManager = AccountManager.get(executor.getContext());
			long newestDate = 0;
			try {
				newestDate = Long.parseLong(accountManager
					.getUserData(account, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE));
			} catch (NumberFormatException e) {
				// swallow
			}
			long oldestDate = Long.MAX_VALUE;
			try {
				oldestDate = Long.parseLong(accountManager
					.getUserData(account, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE));
			} catch (NumberFormatException e) {
				// swallow
			}

			RemotePlaysHandler handler = new RemotePlaysHandler();
			PlaysUrlBuilder builder = new PlaysUrlBuilder(account.name);
			if (newestDate == 0 && oldestDate == Long.MAX_VALUE) {
				// attempt to get all plays
				LOGI(TAG, "...syncing all plays");
				handlePage(handler, builder, syncResult);
				accountManager.setUserData(account, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE,
					String.valueOf(handler.getNewestDate()));
				accountManager.setUserData(account, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE,
					String.valueOf(handler.getOldestDate()));
				// TODO: delete all unupdated
			} else {
				if (newestDate > 0) {
					LOGI(TAG, "...syncing new plays since " + newestDate);
					builder = builder.minDate(newestDate);
					handlePage(handler, builder, syncResult);
					accountManager.setUserData(account, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE,
						String.valueOf(handler.getNewestDate()));
					deleteMissingPlays(handler.getNewestDate(), true, syncResult);
				}

				if (oldestDate > 0 && oldestDate < Long.MAX_VALUE) {
					LOGI(TAG, "...syncing old plays from " + oldestDate);
					builder = builder.maxDate(oldestDate);
					handlePage(handler, builder, syncResult);
					accountManager.setUserData(account, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE,
						String.valueOf(handler.getOldestDate()));
					deleteMissingPlays(handler.getOldestDate(), false, syncResult);
				}
			}

			accountManager.setUserData(account, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, "0");
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	public void handlePage(RemotePlaysHandler handler, PlaysUrlBuilder builder, SyncResult syncResult)
		throws IOException, XmlPullParserException {
		int page = 1;
		while (mExecutor.executeGet(builder.build() + "&page=" + page, handler)) {
			// syncResult.stats.numEntries += handler.getCount();
			page++;
		}
	}

	private void deleteMissingPlays(long time, boolean greaterThan, SyncResult syncResult) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		String date = df.format(new Date(time));

		String selection = Plays.UPDATED_LIST + "<? AND " + Plays.DATE + (greaterThan ? ">" : "<") + "=? AND "
			+ Plays.SYNC_STATUS + "=" + Play.SYNC_STATUS_SYNCED;
		String[] selectionArgs = new String[] { String.valueOf(mStartTime), date };
		int count = mContext.getContentResolver().delete(Plays.CONTENT_URI, selection, selectionArgs);
		// syncResult.stats.numDeletes += count;
		LOGI(TAG, "Deleted " + count + " plays");
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_plays;
	}
}
