package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.SyncResult;

import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteGameHandler;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.GameUrlBuilder;

public class SyncCollectionDetailOldest extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailUnsynced.class);
	private static final int SYNC_GAME_LIMIT = 25;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing oldest games in the collection...");
		try {
			List<String> gameIds = ResolverUtils.queryStrings(executor.getContext().getContentResolver(),
				Games.CONTENT_URI, Games.GAME_ID, null, null, Games.UPDATED + " LIMIT " + SYNC_GAME_LIMIT);
			LOGI(TAG, "...found " + gameIds.size() + " games to update");
			if (gameIds.size() > 0) {
				RemoteBggHandler handler = new RemoteGameHandler();
				String url = new GameUrlBuilder(gameIds).stats().build();
				executor.executeGet(url, handler);
				syncResult.stats.numUpdates += handler.getCount();
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}
}
