package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import android.content.Context;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteCollectionDeleteHandler;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;

public class SyncCollectionList extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionList.class);

	private final static int DAYS_BETWEEN_FULL_SYNCS = 7;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		LOGI(TAG, "Syncing collection list...");
		try {
			final long startTime = System.currentTimeMillis();
			String[] statuses = BggApplication.getInstance().getSyncStatuses();

			if (statuses != null && statuses.length > 0) {
				String username = BggApplication.getInstance().getUserName();
				long modifiedSince = BggApplication.getInstance().getCollectionPartSyncTimestamp();

				for (int i = 0; i < statuses.length; i++) {
					LOGI(TAG, "Syncing status [" + statuses[i] + "]");
					try {
						get(executor,
							((modifiedSince > 0) ? HttpUtils.constructCollectionUrl(username, statuses[i],
								modifiedSince) : HttpUtils.constructCollectionUrl(username, statuses[i])),
							new RemoteCollectionHandler(startTime));
					} catch (HandlerException e) {
						// This happens rather frequently with an EOF exception
						LOGE(TAG, "Problem syncing status [" + statuses[i] + "] (continuing with next status)", e);
					}
					if (isBggDown()) {
						LOGW(TAG, "BGG down while syncing status " + statuses[i]);
						return;
					}
				}

				if (needsFullSync()) {
					LOGI(TAG, "Full sync needed");
					for (int i = 0; i < statuses.length; i++) {
						get(executor, HttpUtils.constructBriefCollectionUrl(username, statuses[i]),
							new RemoteCollectionDeleteHandler(startTime));
						if (isBggDown()) {
							LOGW(TAG, "BGG down while full-syncing");
							return;
						}
					}
				}
			}

			if (needsFullSync()) {
				LOGI(TAG, "Deleting old collection entries");
				// TODO: delete thumbnail images associated with this list (both collection and game
				// This next delete removes old collection entries for current games
				context.getContentResolver().delete(Collection.CONTENT_URI, Collection.UPDATED_LIST + "<?",
					new String[] { String.valueOf(startTime) });
				BggApplication.getInstance().putCollectionFullSyncTimestamp(startTime);
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

	private void get(RemoteExecutor executor, String url, RemoteBggHandler handler) throws HandlerException {
		executor.executeGet(url, handler);
		setIsBggDown(handler.isBggDown());
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_list;
	}
}
