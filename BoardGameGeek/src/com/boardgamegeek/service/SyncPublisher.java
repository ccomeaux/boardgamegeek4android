package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemotePublisherHandler;
import com.boardgamegeek.util.HttpUtils;

public class SyncPublisher extends UpdateTask {
	private static final String TAG = makeLogTag(SyncPublisher.class);
	private int mPublisherId;

	public SyncPublisher(int publisherId) {
		mPublisherId = publisherId;
	}

	@Override
	public void execute(RemoteExecutor executor, Context context) {
		RemotePublisherHandler handler = new RemotePublisherHandler(mPublisherId);
		String url = HttpUtils.constructPublisherUrl(mPublisherId);
		safelyExecuteGet(executor, url, handler);
		LOGI(TAG, "Synched Publisher " + mPublisherId);
	}
}
