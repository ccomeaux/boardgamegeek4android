package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.model.ThingResponse;
import com.boardgamegeek.model.persister.GamePersister;

public class SyncGame extends UpdateTask {
	private static final String TAG = makeLogTag(SyncGame.class);
	private int mGameId;
	protected RemoteExecutor mExecutor;
	private String mErrorMessage;

	public String getErrorMessage() {
		return mErrorMessage;
	}

	public void setExecutor(RemoteExecutor executor) {
		mExecutor = executor;
	}

	public SyncGame(int gameId) {
		mGameId = gameId;
	}

	@Override
	public void execute(Context context) {
		long startTime = System.currentTimeMillis();
		BggService service = Adapter.create();
		ThingResponse response = service.thing(mGameId, 1);
		GamePersister gp = new GamePersister(context, response.games, startTime);
		gp.save();
		LOGI(TAG, "Synced Game " + mGameId);
	}

	protected void safelyExecuteGet(RemoteExecutor executor, String url, RemoteBggHandler handler) {
		try {
			executor.executeGet(url, handler);
		} catch (IOException e) {
			mErrorMessage = e.toString();
		} catch (XmlPullParserException e) {
			mErrorMessage = e.toString();
		}
	}
}
