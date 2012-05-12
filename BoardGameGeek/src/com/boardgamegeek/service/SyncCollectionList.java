package com.boardgamegeek.service;

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

	private final static int DAYS_BETWEEN_FULL_SYNCS = 7;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		final long startTime = System.currentTimeMillis();

		String[] statuses = BggApplication.getInstance().getSyncStatuses();

		if (statuses != null && statuses.length > 0) {
			String username = BggApplication.getInstance().getUserName();
			long modifiedSince = BggApplication.getInstance().getCollectionPartSyncTimestamp();

			for (int i = 0; i < statuses.length; i++) {
				get(executor,
						((modifiedSince > 0) ? HttpUtils.constructCollectionUrl(username, statuses[i], modifiedSince)
								: HttpUtils.constructBriefCollectionUrl(username, statuses[i])),
						new RemoteCollectionHandler(startTime));
				if (isBggDown()) {
					return;
				}
			}

			if (needsFullSync()) {
				for (int i = 0; i < statuses.length; i++) {
					get(executor, HttpUtils.constructBriefCollectionUrl(username, statuses[i]),
							new RemoteCollectionDeleteHandler(startTime));
					if (isBggDown()) {
						return;
					}
				}
			}
		}

		if (needsFullSync()) {
			// This next delete removes old collection entries for current games
			context.getContentResolver().delete(Collection.CONTENT_URI, Collection.UPDATED_LIST + "<?",
					new String[] { String.valueOf(startTime) });
			BggApplication.getInstance().putCollectionFullSyncTimestamp(startTime);
		}
		BggApplication.getInstance().putCollectionPartSyncTimestamp(startTime);
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
