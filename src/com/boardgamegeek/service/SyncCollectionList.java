package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.SyncResult;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteCollectionDeleteHandler;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;

public class SyncCollectionList extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionList.class);

	private final static int DAYS_BETWEEN_FULL_SYNCS = 7;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing collection list...");
		try {
			final long startTime = System.currentTimeMillis();
			String[] statuses = BggApplication.getInstance().getSyncStatuses();

			if (statuses != null && statuses.length > 0) {
				long modifiedSince = BggApplication.getInstance().getCollectionPartSyncTimestamp();

				for (int i = 0; i < statuses.length; i++) {
					LOGI(TAG, "Syncing status [" + statuses[i] + "]");
					try {
						RemoteCollectionHandler handler = new RemoteCollectionHandler(startTime);
						get(executor,
							((modifiedSince > 0) ? HttpUtils.constructCollectionUrl(account.name, statuses[i],
								modifiedSince) : HttpUtils.constructCollectionUrl(account.name, statuses[i])), handler);
						syncResult.stats.numInserts += handler.getNumInserts();
						syncResult.stats.numUpdates += handler.getNumUpdates();
						syncResult.stats.numSkippedEntries += handler.getNumSkips();
					} catch (IOException e) {
						// This happens rather frequently with an EOF exception
						LOGE(TAG, "Problem syncing status [" + statuses[i] + "] (continuing with next status)", e);
						syncResult.stats.numIoExceptions++;
					}
				}

				if (needsFullSync()) {
					LOGI(TAG, "Full sync needed");
					for (int i = 0; i < statuses.length; i++) {
						get(executor, HttpUtils.constructBriefCollectionUrl(account.name, statuses[i]),
							new RemoteCollectionDeleteHandler(startTime));
					}

					LOGI(TAG, "Deleting old collection entries");
					// TODO: delete thumbnail images associated with this list (both collection and game
					// This next delete removes old collection entries for current games
					int count = executor
						.getContext()
						.getContentResolver()
						.delete(Collection.CONTENT_URI, Collection.UPDATED_LIST + "<?",
							new String[] { String.valueOf(startTime) });
					syncResult.stats.numDeletes += count;
					BggApplication.getInstance().putCollectionFullSyncTimestamp(startTime);
				}
			}

			BggApplication.getInstance().putCollectionPartSyncTimestamp(startTime);
		} finally {
			LOGI(TAG, "Syncing collection list complete.");
		}
	}

	private boolean needsFullSync() {
		long lastFullSync = BggApplication.getInstance().getCollectionFullSyncTimestamp();
		return DateTimeUtils.howManyDaysOld(lastFullSync) > DAYS_BETWEEN_FULL_SYNCS;
	}

	private void get(RemoteExecutor executor, String url, RemoteBggHandler handler) throws IOException,
		XmlPullParserException {
		executor.executeGet(url, handler);
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_list;
	}
}
