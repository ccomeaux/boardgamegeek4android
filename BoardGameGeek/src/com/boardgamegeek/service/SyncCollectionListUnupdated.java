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
import com.boardgamegeek.io.RetryableException;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.ResolverUtils;

public class SyncCollectionListUnupdated extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionListUnupdated.class);
	private static final int GAME_PER_FETCH = 25;
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_BACKOFF = 100;

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing unupdated collection list...");
		try {
			List<Integer> gameIds = ResolverUtils.queryInts(executor.getContext().getContentResolver(),
				Collection.CONTENT_URI, Collection.GAME_ID, "collection." + Collection.UPDATED + "=0 OR collection."
					+ Collection.UPDATED + " IS NULL", null, Collection.COLLECTION_ID + " LIMIT " + GAME_PER_FETCH);
			LOGI(TAG, "...found " + gameIds.size() + " collection items to update");
			if (gameIds.size() > 0) {
				CollectionPersister persister = new CollectionPersister(executor.getContext()).includePrivateInfo()
					.includeStats();
				BggService service = Adapter.create();
				CollectionResponse response = getResponse(service, account.name, gameIds);
				if (response != null) {
					persister.save(response.items);
				}
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_unupdated;
	}

	private CollectionResponse getResponse(BggService service, String username, List<Integer> gameIds) {
		int retries = 0;
		while (true) {
			try {
				return service.collectionForGame(username, 1, 1, TextUtils.join(",", gameIds));
			} catch (Exception e) {
				if (e instanceof RetryableException || e.getCause() instanceof RetryableException) {
					retries++;
					if (retries > MAX_RETRIES) {
						break;
					}
					try {
						LOGI(TAG, "...retrying #" + retries);
						Thread.sleep(retries * retries * RETRY_BACKOFF);
					} catch (InterruptedException e1) {
						LOGI(TAG, "Interrupted while sleeping before retry " + retries);
						break;
					}
				} else {
					throw e;
				}
			}
		}
		return null;
	}
}
