package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncResult;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ResolverUtils;

public class SyncCollectionDetailMissing extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailMissing.class);
	private static final int HOURS_OLD = 0;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Removing missing games from the collection...");
		try {
			String arg = String.valueOf(DateTimeUtils.hoursAgo(HOURS_OLD));
			LOGI(TAG, "...since " + arg);
			ContentResolver resolver = executor.getContext().getContentResolver();
			List<Integer> gameIds = ResolverUtils.queryInts(resolver, Games.CONTENT_URI, Games.GAME_ID,
				"collection.game_id IS NULL AND games.updated_list<?", new String[] { arg }, "games." + Games.UPDATED);
			LOGI(TAG, "...found " + gameIds.size() + " games to remove");
			if (gameIds.size() > 0) {
				for (Integer gameId : gameIds) {
					LOGI(TAG, "Deleting game ID=" + gameId);
					resolver.delete(Games.buildGameUri(gameId), null, null);
				}
				// syncResult.stats.numDeletes += gameIds.size();
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_missing;
	}
}
