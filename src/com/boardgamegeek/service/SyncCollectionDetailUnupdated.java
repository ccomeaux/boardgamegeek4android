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
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.GameUrlBuilder;

public class SyncCollectionDetailUnupdated extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailUnupdated.class);
	private static final int GAMES_PER_FETCH = 25;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing unupdated games in the collection...");
		try {
			List<String> gameIds = ResolverUtils.queryStrings(executor.getContext().getContentResolver(),
				Games.CONTENT_URI, Games.GAME_ID, SyncColumns.UPDATED + "=0 OR " + SyncColumns.UPDATED + " IS NULL",
				null);
			LOGI(TAG, "...found " + gameIds.size() + " games to update");
			if (gameIds.size() > 0) {
				for (int i = 0; i < gameIds.size(); i += GAMES_PER_FETCH) {
					if (i <= gameIds.size()) {
						List<String> ids = gameIds.subList(i, Math.min(i + GAMES_PER_FETCH, gameIds.size()));
						LOGI(TAG, "...updating " + ids.size() + " games");
						RemoteBggHandler handler = new RemoteGameHandler();
						String url = new GameUrlBuilder(ids).stats().build();
						executor.executeGet(url, handler);
						syncResult.stats.numUpdates += handler.getCount();
					}
				}
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}
}
