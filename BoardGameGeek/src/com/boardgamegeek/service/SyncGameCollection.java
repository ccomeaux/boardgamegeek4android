package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RetryableException;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;

public class SyncGameCollection extends UpdateTask {
	private static final String TAG = makeLogTag(SyncGameCollection.class);
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_BACKOFF = 100;

	private int mGameId;

	public SyncGameCollection(int gameId) {
		mGameId = gameId;
	}

	@Override
	public String getDescription() {
		return "Sync collection for game ID=" + mGameId;
	}

	@Override
	public void execute(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		CollectionPersister persister = new CollectionPersister(context).includePrivateInfo().includeStats();
		BggService service = Adapter.createWithAuthRetry(context);
		CollectionResponse response = null;

		int retries = 0;
		while (true) {
			try {
				response = service.collectionForGame(account.name, 1, 1, mGameId);
				break;
			} catch (Exception e) {
				if (e instanceof RetryableException || e.getCause() instanceof RetryableException) {
					retries++;
					if (retries > MAX_RETRIES) {
						break;
					}
					try {
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
		if (response == null || response.items == null || response.items.size() == 0) {
			LOGI(TAG, "No collection items for game ID=" + mGameId);
		}
		persister.save(response.items);
		LOGI(TAG, "Synced " + response.items.size() + " collection item(s) for game ID=" + mGameId);
	}
}
