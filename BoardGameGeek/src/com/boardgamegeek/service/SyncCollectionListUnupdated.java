package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.SyncResult;

import com.boardgamegeek.R;
import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.CollectionUrlBuilder;

public class SyncCollectionListUnupdated extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionListUnupdated.class);
	private static final int GAME_PER_FETCH = 25;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing unupdated collection list...");
		try {
			List<Integer> gameIds = ResolverUtils.queryInts(executor.getContext().getContentResolver(),
				Collection.CONTENT_URI, Collection.GAME_ID, "collection." + Collection.UPDATED + "=0 OR collection."
					+ Collection.UPDATED + " IS NULL", null, Collection.COLLECTION_ID + " LIMIT " + GAME_PER_FETCH);
			LOGI(TAG, "...found " + gameIds.size() + " collection items to update");
			if (gameIds.size() > 0) {
				RemoteCollectionHandler handler = new RemoteCollectionHandler(System.currentTimeMillis(), true, true);
				CollectionUrlBuilder builder = new CollectionUrlBuilder(account.name).showPrivate().stats();
				for (int i = 0; i < gameIds.size(); i++) {
					if (isCancelled()) {
						break;
					}
					int gameId = gameIds.get(i);
					builder.addGameId(gameId);
				}
				String url = builder.build();
				executor.safelyExecuteGet(url, handler);
				// syncResult.stats.numUpdates += handler.getCount();
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_unupdated;
	}

}
