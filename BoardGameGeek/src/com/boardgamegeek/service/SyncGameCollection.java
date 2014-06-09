package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;

public class SyncGameCollection extends UpdateTask {
	private static final String TAG = makeLogTag(SyncGameCollection.class);
	private int mGameId;

	public SyncGameCollection(int gameId) {
		mGameId = gameId;
	}

	@Override
	public void execute(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		BggService service = Adapter.createWithAuth(context);
		long t = System.currentTimeMillis();
		CollectionResponse response = service.collection(account.name, mGameId, 1, 0);
		if (response.items == null || response.items.size() == 0) {
			LOGI(TAG, "No collection items for game ID=" + mGameId);
		}
		CollectionPersister.save(context, response.items, t);
		LOGI(TAG, "Synced " + response.items.size() + " collection item(s) for game ID=" + mGameId);
	}
}
