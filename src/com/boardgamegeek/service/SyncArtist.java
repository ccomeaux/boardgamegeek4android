package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.RemoteArtistHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.util.HttpUtils;

public class SyncArtist extends SyncTask {
	private static final String TAG = makeLogTag(SyncArtist.class);
	private int mArtistId;

	public SyncArtist(int artistId) {
		mArtistId = artistId;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		RemoteArtistHandler handler = new RemoteArtistHandler(mArtistId);
		executor.executeGet(HttpUtils.constructArtistUrl(mArtistId), handler);
		setIsBggDown(handler.isBggDown());
		LOGI(TAG, "Synched Artist " + mArtistId);
	}
}
