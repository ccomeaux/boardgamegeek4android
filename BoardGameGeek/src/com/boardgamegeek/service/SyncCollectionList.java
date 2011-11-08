package com.boardgamegeek.service;

import android.content.ContentResolver;
import android.content.Context;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.HttpUtils;

public class SyncCollectionList extends SyncTask {

	private String mUsername;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		final long startTime = System.currentTimeMillis();
		ContentResolver resolver = context.getContentResolver();

		mUsername = BggApplication.getInstance().getUserName();
		String[] statuses = BggApplication.getInstance().getSyncStatuses();

		if (statuses != null && statuses.length > 0) {
			for (int i = 0; i < statuses.length; i++) {
				executor.executeGet(HttpUtils.constructCollectionUrl(mUsername, statuses[i]),
						new RemoteCollectionHandler(startTime));
			}
		}

		String[] selectionArgs = new String[] { String.valueOf(startTime) };
		resolver.delete(Games.CONTENT_URI, Games.UPDATED_LIST + "<?", selectionArgs);
		// This next delete removes old collection entries for current games
		resolver.delete(Collection.CONTENT_URI, Collection.UPDATED_LIST + "<?", selectionArgs);
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_list;
	}
}
