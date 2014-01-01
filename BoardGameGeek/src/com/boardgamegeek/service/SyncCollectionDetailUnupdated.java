package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.SyncResult;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteGameHandler;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.url.GameUrlBuilder;

public class SyncCollectionDetailUnupdated extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailUnupdated.class);
	private static final int GAMES_PER_FETCH = 25;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing unupdated games in the collection...");
		try {
			List<String> gameIds = ResolverUtils.queryStrings(executor.getContext().getContentResolver(),
				Games.CONTENT_URI, Games.GAME_ID, Games.UPDATED + "=0 OR " + Games.UPDATED + " IS NULL", null);
			LOGI(TAG, "...found " + gameIds.size() + " games to update");
			if (gameIds.size() > 0) {
				for (int i = 0; i < gameIds.size(); i += GAMES_PER_FETCH) {
					if (isCancelled()) {
						break;
					}
					if (i <= gameIds.size()) {
						List<String> ids = gameIds.subList(i, Math.min(i + GAMES_PER_FETCH, gameIds.size()));
						LOGI(TAG, "...updating " + ids.size() + " games");
						RemoteBggHandler handler = new RemoteGameHandler(System.currentTimeMillis());
						String url = new GameUrlBuilder(ids).stats().build();
						executor.executeGet(url, handler);
						// syncResult.stats.numUpdates += handler.getCount();
					}
				}
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_games_unupdated;
	}
}
