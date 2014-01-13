package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.SyncResult;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ResolverUtils;

public class SyncCollectionDetailMissing extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailMissing.class);
	private static final int HOURS_OLD = 72;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Deleting missing games from the collection...");
		try {
			long hoursAgo = DateTimeUtils.hoursAgo(HOURS_OLD);
			String arg = String.valueOf(hoursAgo);
			LOGI(
				TAG,
				"...since "
					+ DateUtils.formatDateTime(executor.getContext(), hoursAgo, DateUtils.FORMAT_SHOW_DATE
						| DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME));
			ContentResolver resolver = executor.getContext().getContentResolver();
			List<Integer> gameIds = ResolverUtils
				.queryInts(resolver, Games.CONTENT_URI, Games.GAME_ID,
					"collection.game_id IS NULL AND games.updated_list < ?", new String[] { arg }, "games."
						+ Games.UPDATED);
			LOGI(TAG, "...found " + gameIds.size() + " games to delete");
			if (gameIds.size() > 0) {
				int count = 0;
				for (Integer gameId : gameIds) {
					LOGI(TAG, "...deleting game ID=" + gameId);
					count += resolver.delete(Games.buildGameUri(gameId), null, null);
				}
				LOGI(TAG, "...deleted " + count + " games");
				// syncResult.stats.numDeletes += count;
			}
			// NOTE: We're not deleting on with the selection because it doesn't perform the game/collection join
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_missing;
	}
}
