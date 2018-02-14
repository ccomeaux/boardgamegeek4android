package com.boardgamegeek.service;

import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;

import java.util.List;

import timber.log.Timber;

/**
 * Removes games that aren't in the collection and haven't been viewed in 72 hours.
 */
public class SyncGamesRemove extends SyncTask {
	private static final int HOURS_OLD = 72;

	public SyncGamesRemove(Context context, BggService service, @NonNull SyncResult syncResult) {
		super(context, service, syncResult);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD;
	}

	@Override
	public void execute() {
		Timber.i("Removing games not in the collection...");
		try {
			List<Integer> gameIds = getGameIds();
			if (gameIds.size() > 0) {
				Timber.i("...found %,d games to delete", gameIds.size());
				updateProgressNotification("Deleting " + gameIds.size() + " games from your collection");

				int count = 0;
				// NOTE: We're deleting one at a time, because a batch doesn't perform the game/collection join
				for (Integer gameId : gameIds) {
					Timber.i("...deleting game ID=%s", gameId);
					count += context.getContentResolver().delete(Games.buildGameUri(gameId), null, null);
				}
				syncResult.stats.numDeletes += count;
				Timber.i("...deleted %,d games", count);
			} else {
				Timber.i("...no games need deleting");
			}
		} finally {
			Timber.i("...complete!");
		}
	}

	/**
	 * Get a list of games, sorted by least recently updated, that
	 * 1. have no associated collection record
	 * 2. haven't been viewed in 72 hours
	 * 3. and have 0 plays (if plays are being synced
	 */
	private List<Integer> getGameIds() {
		long hoursAgo = DateTimeUtils.hoursAgo(HOURS_OLD);

		String date = DateUtils.formatDateTime(context, hoursAgo,
			DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME);
		Timber.i("...not viewed since %s", date);


		String selection = "collection." + Collection.GAME_ID + " IS NULL AND games." + Games.LAST_VIEWED + "<?";
		if (PreferencesUtils.isStatusSetToSync(context, BggService.COLLECTION_QUERY_STATUS_PLAYED)) {
			selection += " AND games." + Games.NUM_PLAYS + "=0";
		}
		return ResolverUtils.queryInts(context.getContentResolver(),
			Games.CONTENT_URI,
			Games.GAME_ID,
			selection,
			new String[] { String.valueOf(hoursAgo) },
			"games." + Games.UPDATED);
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_collection_missing;
	}
}
