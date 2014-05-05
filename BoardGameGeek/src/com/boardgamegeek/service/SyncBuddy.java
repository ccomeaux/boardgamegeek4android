package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.RemoteBuddyUserHandler;
import com.boardgamegeek.util.url.UserUrlBuilder;

public class SyncBuddy extends UpdateTask {
	private static final String TAG = makeLogTag(SyncBuddy.class);
	private String mName;

	public SyncBuddy(String name) {
		mName = name;
	}

	@Override
	public void execute(Context context) {
		RemoteBuddyUserHandler handler = new RemoteBuddyUserHandler(System.currentTimeMillis());
		String url = new UserUrlBuilder(mName).build();
		safelyExecuteGet(mExecutor, url, handler);
		LOGI(TAG, "Synced Buddy " + mName);
	}
}
