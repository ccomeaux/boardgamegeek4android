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
import com.boardgamegeek.io.RemotePlaysParser;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.PreferencesUtils;

public class SyncPlays extends SyncTask {
	private static final String TAG = makeLogTag(SyncPlays.class);
	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private RemoteExecutor mExecutor;
	private Account mAccount;
	private Context mContext;
	private long mStartTime;
	private AccountManager mAccountManager;

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
			mAccount = account;
			mContext = executor.getContext();
			mStartTime = System.currentTimeMillis();
			mAccountManager = AccountManager.get(executor.getContext());

			RemotePlaysParser parser = new RemotePlaysParser(account.name);

			long newestDate = parseLong(SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
			LOGI(TAG, "...syncing plays since " + formatDate(newestDate));
			parser.setMinDate(newestDate);
			if (parseAndSave(parser)) {
				deleteUnupdatedPlaysSince(parser.getNewestDate());
			}

			long oldestDate = parseLong(SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
			if (oldestDate > 0) {
				LOGI(TAG, "...syncing plays before " + formatDate(oldestDate));
				parser.setMaxDate(oldestDate);
				if (parseAndSave(parser)) {
					deleteUnupdatedPlaysBefore(parser.getOldestDate());
					setLong(SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, 0);
				}
			}
			SyncService.hIndex(mContext);
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	/**
	 * Parses plays specified by the parser, saving them in the content resolver, updating the account time stamps.
	 * 
	 * @return <code>true</code>, if completed successfully; <code>false</code> otherwise
	 */
	public boolean parseAndSave(RemotePlaysParser parser) throws IOException, XmlPullParserException {
		boolean morePages = false;
		do {
			morePages = mExecutor.executeGet(parser);
			if (isCancelled()) {
				return false;
			}
			PlayPersister.save(mContext.getContentResolver(), parser.getPlays());
			// mSyncResult.stats.numEntries += handler.getCount();
			updateTimeStamps(parser);
			parser.nextPage();
		} while (morePages);
		return true;
	}

	private void deleteUnupdatedPlaysSince(long time) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + ">=? AND " + Plays.SYNC_STATUS + "="
			+ Play.SYNC_STATUS_SYNCED, new String[] { String.valueOf(mStartTime), formatDate(time) });
	}

	private void deleteUnupdatedPlaysBefore(long time) {
		deletePlays(Plays.UPDATED_LIST + "<? AND " + Plays.DATE + "<=? AND " + Plays.SYNC_STATUS + "="
			+ Play.SYNC_STATUS_SYNCED, new String[] { String.valueOf(mStartTime), formatDate(time) });
	}

	private void deletePlays(String selection, String[] selectionArgs) {
		int count = mContext.getContentResolver().delete(Plays.CONTENT_URI, selection, selectionArgs);
		// mSyncResult.stats.numDeletes += count;
		LOGI(TAG, "Deleted " + count + " unupdated plays");
	}

	private void updateTimeStamps(RemotePlaysParser parser) {
		long newestDate = parseLong(SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
		if (parser.getNewestDate() > newestDate) {
			setLong(SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, parser.getNewestDate());
		}
		long oldestDate = parseLong(SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);
		if (parser.getOldestDate() < oldestDate) {
			setLong(SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, parser.getOldestDate());
		}
	}

	private long parseLong(String key, long defaultValue) {
		long l = defaultValue;
		try {
			l = Long.parseLong(mAccountManager.getUserData(mAccount, key));
		} catch (NumberFormatException e) {
			// swallow and return the default value
		}
		return l;
	}

	private void setLong(String key, long l) {
		mAccountManager.setUserData(mAccount, key, String.valueOf(l));
	}

	private String formatDate(long time) {
		String date = FORMAT.format(new Date(time));
		return date;
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_plays;
	}
}
