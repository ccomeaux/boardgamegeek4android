package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.Context;

import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;

public class SyncBuddy extends UpdateTask {
	private static final String TAG = makeLogTag(SyncBuddy.class);
	private String mName;

	public SyncBuddy(String name) {
		mName = name;
	}

	@Override
	public String getDescription() {
		return "Sync artist name=" + mName;
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		User user = service.user(mName);

		if (user == null || user.id == 0) {
			LOGI(TAG, "Invalid user: " + mName);
			return;
		}

		BuddyPersister.save(context, user);
		LOGI(TAG, "Synced Buddy " + mName);
	}
}
