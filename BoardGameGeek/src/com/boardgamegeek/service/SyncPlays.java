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

			long newestDate = parseLong(SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, 0);
			long oldestDate = parseLong(SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE);

			boolean success = false;
			RemotePlaysParser parser = new RemotePlaysParser(account.name);
			if (newestDate == 0 && oldestDate == Long.MAX_VALUE) {
				// attempt to get all plays
				LOGI(TAG, "...syncing all plays");

				if (parseAndSave(parser, syncResult)) {
					setLong(SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, parser.getNewestDate());
					setLong(SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, parser.getOldestDate());
					success = true;
					// TODO: delete all unupdated
				}
			} else {
				if (newestDate > 0) {
					LOGI(TAG, "...syncing plays since " + newestDate);
					parser.setMinDate(newestDate);
					if (parseAndSave(parser, syncResult)) {
						setLong(SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, parser.getNewestDate());
						deleteMissingPlays(parser.getNewestDate(), true, syncResult);
					}
				}

				if (oldestDate > 0 && oldestDate < Long.MAX_VALUE) {
					LOGI(TAG, "...syncing plays before " + oldestDate);
					parser.setMaxDate(oldestDate);
					if (parseAndSave(parser, syncResult)) {
						setLong(SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, parser.getOldestDate());
						deleteMissingPlays(parser.getOldestDate(), false, syncResult);
						success = true;
					}
				}
			}

			if (success) {
				setLong(SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, 0);
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	private void setLong(String key, long l) {
		mAccountManager.setUserData(mAccount, key, String.valueOf(l));
	}

	private long parseLong(String key, long defaultValue) {
		long l = defaultValue;
		try {
			l = Long.parseLong(mAccountManager.getUserData(mAccount, key));
		} catch (NumberFormatException e) {
			// swallow
		}
		return l;
	}

	/**
	 * 
	 * @return <code>true</code>, if completed successfully; <code>false</code> otherwise
	 */
	public boolean parseAndSave(RemotePlaysParser parser, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		while (mExecutor.executeGet(parser)) {
			if (isCancelled()) {
				return false;
			}
			PlayPersister.save(mContext.getContentResolver(), parser.getPlays());
			// syncResult.stats.numEntries += handler.getCount();
			parser.nextPage();
		}
		return true;
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
