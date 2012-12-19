package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.RemoteArtistHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.util.HttpUtils;

public class SyncArtist extends UpdateTask {
	private static final String TAG = makeLogTag(SyncArtist.class);
	private int mArtistId;

	public SyncArtist(int artistId) {
		mArtistId = artistId;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) {
		RemoteArtistHandler handler = new RemoteArtistHandler(mArtistId);
		String url = HttpUtils.constructArtistUrl(mArtistId);
		safelyExecuteGet(executor, url, handler);
		LOGI(TAG, "Synched Artist " + mArtistId);
	}
}
