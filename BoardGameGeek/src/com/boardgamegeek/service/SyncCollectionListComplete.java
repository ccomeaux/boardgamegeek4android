package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.CollectionUrlBuilder;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;

public class SyncCollectionListComplete extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionListComplete.class);

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing full collection list...");
		boolean success = true;
		try {
			String[] statuses = PreferencesUtils.getSyncStatuses(executor.getContext());
			if (statuses == null || statuses.length == 0) {
				LOGI(TAG, "...no statuses set to sync");
				return;
			}

			AccountManager accountManager = AccountManager.get(executor.getContext());
			String s = accountManager.getUserData(account, SyncService.TIMESTAMP_COLLECTION_COMPLETE);
			long lastCompleteSync = TextUtils.isEmpty(s) ? 0 : Long.parseLong(s);
			if (lastCompleteSync >= 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < 7) {
				LOGI(TAG, "...skipping; we did a full sync already this week");
				return;
			}

			final long startTime = System.currentTimeMillis();

			for (int i = 0; i < statuses.length; i++) {
				LOGI(TAG, "...syncing status [" + statuses[i] + "]");
				try {
					RemoteCollectionHandler handler = new RemoteCollectionHandler(startTime, false);
					String url = new CollectionUrlBuilder(account.name).status(statuses[i]).brief().build();
					executor.executeGet(url, handler);
					// syncResult.stats.numInserts += handler.getNumInserts();
					// syncResult.stats.numUpdates += handler.getNumUpdates();
					// syncResult.stats.numSkippedEntries += handler.getNumSkips();
				} catch (IOException e) {
					// This happens rather frequently with an EOF exception
					LOGE(TAG, "  Problem syncing status [" + statuses[i] + "] (continuing with next status)", e);
					syncResult.stats.numIoExceptions++;
					success = false;
				}
			}

			if (success) {
				LOGI(TAG, "...deleting old collection entries");
				// Delete all collection items that weren't updated in the sync above
				executor
					.getContext()
					.getContentResolver()
					.delete(Collection.CONTENT_URI, Collection.UPDATED_LIST + "<?",
						new String[] { String.valueOf(startTime) });
				// syncResult.stats.numDeletes += count;
				// TODO: delete games as well?!
				// TODO: delete thumbnail images associated with this list (both collection and game

				accountManager.setUserData(account, SyncService.TIMESTAMP_COLLECTION_COMPLETE,
					String.valueOf(startTime));
				accountManager
					.setUserData(account, SyncService.TIMESTAMP_COLLECTION_PARTIAL, String.valueOf(startTime));
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_full;
	}
}
