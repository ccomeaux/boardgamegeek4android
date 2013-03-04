package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.RemoteCollectionHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.util.CollectionUrlBuilder;

public class SyncGameCollection extends UpdateTask {
	private static final String TAG = makeLogTag(SyncGameCollection.class);
	private int mGameId;

	public SyncGameCollection(int gameId) {
		mGameId = gameId;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		RemoteCollectionHandler handler = new RemoteCollectionHandler(System.currentTimeMillis());
		String url = new CollectionUrlBuilder(account.name).gameId(mGameId).showPrivate().stats().build();
		executor.safelyExecuteGet(url, handler);
		LOGI(TAG, "Synced collection for game " + mGameId);
	}
}
