package com.boardgamegeek.service;

import android.content.ContentResolver;
import android.content.Context;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteCollectionDeleteHandler;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;

public class SyncCollectionList extends SyncTask {

	private final static int DAYS_BETWEEN_FULL_SYNCS = 7;

	private String mUsername;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		final long startTime = System.currentTimeMillis();

		mUsername = BggApplication.getInstance().getUserName();
		String[] statuses = BggApplication.getInstance().getSyncStatuses();
		boolean needsFullSync = false;

		if (statuses != null && statuses.length > 0) {
			long modifiedSince = BggApplication.getInstance().getCollectionPartSyncTimestamp();
			for (int i = 0; i < statuses.length; i++) {
				String url = HttpUtils.constructCollectionUrl(mUsername, statuses[i]);
				if (modifiedSince > 0) {
					url = HttpUtils.constructCollectionUrl(mUsername, statuses[i], modifiedSince);
				}
				executor.executeGet(url, new RemoteCollectionHandler(startTime));
			}

			long lastFullSync = BggApplication.getInstance().getCollectionFullSyncTimestamp();
			needsFullSync = DateTimeUtils.howManyDaysOld(lastFullSync) > DAYS_BETWEEN_FULL_SYNCS;
			if (needsFullSync) {
				for (int i = 0; i < statuses.length; i++) {
					String url = HttpUtils.constructCollectionUrl(mUsername, statuses[i]);
					executor.executeGet(url, new RemoteCollectionDeleteHandler(startTime));
				}
			}
		}

		if (needsFullSync) {
			// This next delete removes old collection entries for current games
			ContentResolver resolver = context.getContentResolver();
			String[] selectionArgs = new String[] { String.valueOf(startTime) };
			resolver.delete(Collection.CONTENT_URI, Collection.UPDATED_LIST + "<?", selectionArgs);
			BggApplication.getInstance().putCollectionFullSyncTimestamp(startTime);
		}
		BggApplication.getInstance().putCollectionPartSyncTimestamp(startTime);
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_list;
	}
}
