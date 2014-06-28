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
 * Syncs all games in the collection that have not been updated completely.
 */
public class SyncCollectionDetailUnupdated extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailUnupdated.class);
	private static final int GAMES_PER_FETCH = 25;

	public SyncCollectionDetailUnupdated(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing unupdated games in the collection...");
		try {
			List<String> gameIds = ResolverUtils.queryStrings(context.getContentResolver(), Games.CONTENT_URI,
				Games.GAME_ID, "games." + Games.UPDATED + "=0 OR games." + Games.UPDATED + " IS NULL", null);
			LOGI(TAG, "...found " + gameIds.size() + " games to update");
			if (gameIds.size() > 0) {
				for (int i = 0; i < gameIds.size(); i += GAMES_PER_FETCH) {
					if (isCancelled()) {
						break;
					}
					if (i <= gameIds.size()) {
						List<String> ids = gameIds.subList(i, Math.min(i + GAMES_PER_FETCH, gameIds.size()));
						LOGI(TAG, "...updating " + ids.size() + " games");
						GamePersister gp = new GamePersister(context);
						ThingResponse response = mService.thing(TextUtils.join(",", gameIds), 1);
						int count = gp.save(response.games);
						LOGI(TAG, "...saved " + count + " rows");
						// syncResult.stats.numUpdates += ...
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
