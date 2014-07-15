package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.net.SocketTimeoutException;
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
	private static final int GAMES_PER_FETCH = 16;

	public SyncCollectionDetailUnupdated(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing unupdated games in the collection...");
		try {
			int gamesPerFetch = GAMES_PER_FETCH;
			int numberOfFetches = 0;
			do {
				if (isCancelled()) {
					break;
				}
				numberOfFetches++;
				List<String> gameIds = ResolverUtils.queryStrings(context.getContentResolver(), Games.CONTENT_URI,
					Games.GAME_ID, "games." + Games.UPDATED + "=0 OR games." + Games.UPDATED + " IS NULL", null,
					"games." + Games.UPDATED_LIST + " DESC LIMIT " + gamesPerFetch);
				if (gameIds.size() > 0) {
					LOGI(TAG, "...found " + gameIds.size() + " games to update [" + TextUtils.join(", ", gameIds) + "]");
					try {
						GamePersister persister = new GamePersister(context);
						ThingResponse response = mService.thing(TextUtils.join(",", gameIds), 1);
						int count = persister.save(response.games);
						// syncResult.stats.numUpdates += gameIds.size();
						LOGI(TAG, "...saved " + count + " rows for " + gameIds.size() + " games");
					} catch (Exception e) {
						if (e.getCause() instanceof SocketTimeoutException) {
							if (gamesPerFetch == 1) {
								LOGI(TAG, "...timeout with only 1 game; aborting.");
								break;
							}
							gamesPerFetch = gamesPerFetch / 2;
							LOGI(TAG, "...timeout - reducing games per fetch to " + gamesPerFetch);
						}
						throw e;
					}
				} else {
					LOGI(TAG, "...no more unupdated games");
					break;
				}
			} while (numberOfFetches < 100);
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_games_unupdated;
	}
}
