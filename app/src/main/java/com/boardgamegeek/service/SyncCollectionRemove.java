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
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;

/**
 * Removes games that aren't in the collection and haven't been viewed in 72 hours.
 */
public class SyncCollectionRemove extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionRemove.class);
	private static final int HOURS_OLD = 72;
	private static final String STATUS_PLAYED = "played";

	public SyncCollectionRemove(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		LOGI(TAG, "Removing games not in the collection...");
		try {
			long hoursAgo = DateTimeUtils.hoursAgo(HOURS_OLD);

			String date = DateUtils.formatDateTime(mContext, hoursAgo, DateUtils.FORMAT_SHOW_DATE
				| DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME);
			LOGI(TAG, "...not viewed since " + date);

			ContentResolver resolver = mContext.getContentResolver();
			String selection = "collection." + Collection.GAME_ID + " IS NULL AND games." + Games.LAST_VIEWED + "<?";
			if (PreferencesUtils.isSyncStatus(mContext, STATUS_PLAYED)) {
				selection += " AND games." + Games.NUM_PLAYS + "=0";
			}
			List<Integer> gameIds = ResolverUtils.queryInts(resolver, Games.CONTENT_URI, Games.GAME_ID, selection,
				new String[] { String.valueOf(hoursAgo) }, "games." + Games.UPDATED);
			if (gameIds.size() > 0) {
				LOGI(TAG, "...found " + gameIds.size() + " games to delete");
				showNotification("Deleting " + gameIds.size() + " games from your collection");

				int count = 0;
				// NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
				for (Integer gameId : gameIds) {
					LOGI(TAG, "...deleting game ID=" + gameId);
					count += resolver.delete(Games.buildGameUri(gameId), null, null);
					count += resolver.delete(Collection.CONTENT_URI, Collection.GAME_ID + "=?", new String[] { String.valueOf(gameId) });
				}
				syncResult.stats.numDeletes += count;
				LOGI(TAG, "...deleted " + count + " games");
			} else {
				LOGI(TAG, "...no games need deleting");
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
