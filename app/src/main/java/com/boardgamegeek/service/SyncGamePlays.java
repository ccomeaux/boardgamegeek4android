package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.PlaysResponse;
import com.boardgamegeek.model.persister.PlayPersister;
import com.boardgamegeek.provider.BggContract.Games;

public class SyncGamePlays extends UpdateTask {
	private static final String TAG = makeLogTag(SyncGamePlays.class);
	private int mGameId;

	public SyncGamePlays(int gameId) {
		mGameId = gameId;
	}

	@Override
	public String getDescription() {
		return "Sync plays for game ID=" + mGameId;
	}

	@Override
	public void execute(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return;
		}

		BggService service = Adapter.create();

		PlaysResponse response = null;
		try {
			long startTime = System.currentTimeMillis();
			response = service.playsByGame(account.name, mGameId);
			PlayPersister.save(context, response.plays, startTime);
			updateGameTimestamp(context);
			SyncService.hIndex(context);
			LOGI(TAG, "Synced plays for game id=" + mGameId);
		} catch (Exception e) {
			// TODO bubble error up?
			LOGW(TAG, "Problem syncing plays for game id=" + mGameId, e);
		}
	}

	private void updateGameTimestamp(Context context) {
		ContentValues values = new ContentValues(1);
		values.put(Games.UPDATED_PLAYS, System.currentTimeMillis());
		context.getContentResolver().update(Games.buildGameUri(mGameId), values, null, null);
	}
}
