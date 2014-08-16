package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ResolverUtils;

/**
 * Syncs a number of games that haven't been updated in a long time.
 * 
 */
public class SyncCollectionDetailOldest extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailOldest.class);
	private static final int SYNC_GAME_LIMIT = 8;

	public SyncCollectionDetailOldest(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing oldest games in the collection...");
		try {
			List<String> gameIds = ResolverUtils.queryStrings(context.getContentResolver(), Games.CONTENT_URI,
				Games.GAME_ID, null, null, "games." + Games.UPDATED + " LIMIT " + SYNC_GAME_LIMIT);
			if (gameIds.size() > 0) {
				LOGI(TAG, "...found " + gameIds.size() + " games to update [" + TextUtils.join(", ", gameIds) + "]");
				GamePersister gp = new GamePersister(context);
				ThingResponse response = mService.thing(TextUtils.join(",", gameIds), 1);
				if (response.games != null && response.games.size() > 0) {
					int count = gp.save(response.games);
					syncResult.stats.numUpdates += response.games.size();
					LOGI(TAG, "...saved " + count + " rows for " + response.games.size() + "  games");
				} else {
					LOGI(TAG, "...no games returned (shouldn't happen)");
				}
			} else {
				LOGI(TAG, "...found no old games to update (this should only happen with empty collections)");
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_games_oldest;
	}
}
