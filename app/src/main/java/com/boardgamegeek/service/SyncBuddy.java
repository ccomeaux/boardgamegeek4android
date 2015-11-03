package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract;

import timber.log.Timber;

public class SyncBuddy extends UpdateTask {
	private final String name;

	public SyncBuddy(String name) {
		this.name = name;
	}

	@NonNull
	@Override
	public String getDescription(Context context) {
		if (isValid()) {
			return context.getString(R.string.sync_msg_buddy_valid, name);
		}
		return context.getString(R.string.sync_msg_buddy_invalid);
	}

	@Override
	public boolean isValid() {
		return !TextUtils.isEmpty(name);
	}

	@Override
	public void execute(Context context) {
		BggService service = Adapter.create();
		User user = service.user(name);

		if (user == null || user.getId() == 0 || user.getId() == BggContract.INVALID_ID) {
			Timber.i("Invalid user: " + name);
			return;
		}
		BuddyPersister persister = new BuddyPersister(context);
		persister.save(user);
		Timber.i("Synced Buddy " + name);
	}
}
