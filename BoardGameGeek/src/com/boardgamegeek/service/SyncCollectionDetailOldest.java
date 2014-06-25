package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ResolverUtils;

public class SyncCollectionDetailOldest extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionDetailOldest.class);
	private static final int SYNC_GAME_LIMIT = 25;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing oldest games in the collection...");
		try {
			List<String> gameIds = ResolverUtils.queryStrings(executor.getContext().getContentResolver(),
				Games.CONTENT_URI, Games.GAME_ID, null, null, "games." + Games.UPDATED + " LIMIT " + SYNC_GAME_LIMIT);
			LOGI(TAG, "...found " + gameIds.size() + " games to update");
			if (gameIds.size() > 0) {
				BggService service = Adapter.create();
				long time = System.currentTimeMillis();
				ThingResponse response = service.thing(TextUtils.join(",", gameIds), 1);
				GamePersister gp = new GamePersister(executor.getContext(), response.games, time);
				int count = gp.save();
				LOGI(TAG, "...saved " + count + " rows");
				// syncResult.stats.numUpdates += ...;
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_oldest;
	}
}
