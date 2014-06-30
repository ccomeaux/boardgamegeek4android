package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.List;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ResolverUtils;

/**
 * Deletes games that aren't in the collection and haven't been viewed in 72 hours. NOTE: This will probably remove
 * games that are marked as played, but not otherwise in the collection.
 */
public class SyncCollectionDetailMissing extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailMissing.class);
	private static final int HOURS_OLD = 72;

	public SyncCollectionDetailMissing(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Deleting missing games from the collection...");
		try {
			long hoursAgo = DateTimeUtils.hoursAgo(HOURS_OLD);

			String date = DateUtils.formatDateTime(context, hoursAgo, DateUtils.FORMAT_SHOW_DATE
				| DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME);
			LOGI(TAG, "...not viewed since " + date);

			ContentResolver resolver = context.getContentResolver();
			List<Integer> gameIds = ResolverUtils.queryInts(resolver, Games.CONTENT_URI, Games.GAME_ID, "collection."
				+ Collection.GAME_ID + " IS NULL AND games." + Games.LAST_VIEWED + " < ?",
				new String[] { String.valueOf(hoursAgo) }, "games." + Games.UPDATED);
			LOGI(TAG, "...found " + gameIds.size() + " games to delete");
			if (gameIds.size() > 0) {
				int count = 0;
				for (Integer gameId : gameIds) {
					LOGI(TAG, "...deleting game ID=" + gameId);
					count += resolver.delete(Games.buildGameUri(gameId), null, null);
				}
				LOGI(TAG, "...deleted " + count + " games");
			}
			// NOTE: We're not deleting one at a time, because a batch doesn't perform the game/collection join
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_missing;
	}
}
