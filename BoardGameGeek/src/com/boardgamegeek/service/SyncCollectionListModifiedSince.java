package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.Date;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.url.CollectionUrlBuilder;

public class SyncCollectionListModifiedSince extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionListModifiedSince.class);

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		AccountManager accountManager = AccountManager.get(executor.getContext());
		long modifiedSince = getLong(account, accountManager, SyncService.TIMESTAMP_COLLECTION_PARTIAL);

		LOGI(TAG, "Syncing collection list modified since " + new Date(modifiedSince).toString() + "...");
		try {
			String[] statuses = PreferencesUtils.getSyncStatuses(executor.getContext());
			if (statuses == null || statuses.length == 0) {
				LOGI(TAG, "...no statuses set to sync");
				return;
			}

			long lastFullSync = getLong(account, accountManager, SyncService.TIMESTAMP_COLLECTION_COMPLETE);
			if (DateTimeUtils.howManyHoursOld(lastFullSync) < 3) {
				LOGI(TAG, "...skipping; we just did a complete sync");
			}

			final long startTime = System.currentTimeMillis();
			boolean cancelled = false;
			for (int i = 0; i < statuses.length; i++) {
				if (isCancelled()) {
					cancelled = true;
					break;
				}
				LOGI(TAG, "...syncing status [" + statuses[i] + "]");
				try {
					RemoteCollectionHandler handler = new RemoteCollectionHandler(startTime, false, true);
					String url = new CollectionUrlBuilder(account.name).status(statuses[i])
						.modifiedSince(modifiedSince).stats().build();
					executor.executeGet(url, handler);
					// syncResult.stats.numInserts += handler.getNumInserts();
					// syncResult.stats.numUpdates += handler.getNumUpdates();
					// syncResult.stats.numSkippedEntries += handler.getNumSkips();
				} catch (IOException e) {
					// This happens rather frequently with an EOF exception
					LOGE(TAG, "Problem syncing status [" + statuses[i] + "] (continuing with next status)", e);
					syncResult.stats.numIoExceptions++;
				}
			}
			if (!cancelled) {
				accountManager
					.setUserData(account, SyncService.TIMESTAMP_COLLECTION_PARTIAL, String.valueOf(startTime));
			}
			SyncService.hIndex(executor.getContext());
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	public long getLong(Account account, AccountManager accountManager, String key) {
		String l = accountManager.getUserData(account, key);
		return TextUtils.isEmpty(l) ? 0 : Long.parseLong(l);
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_partial;
	}
}
