package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.UserRequest;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public abstract class SyncBuddiesDetail extends SyncTask {
	private BuddyPersister mPersister;

	public SyncBuddiesDetail(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		Timber.i(getLogMessage());
		try {
			if (!PreferencesUtils.getSyncBuddies(mContext)) {
				Timber.i("...buddies not set to sync");
				return;
			}

			mPersister = new BuddyPersister(mContext);
			int count = 0;
			List<String> names = getBuddyNames();
			Timber.i("...found " + names.size() + " buddies to update");
			if (names.size() > 0) {
				showNotification(StringUtils.formatList(names));
				List<User> buddies = new ArrayList<>(names.size());
				for (String name : names) {
					if (isCancelled()) {
						Timber.i("...canceled while syncing buddies");
						break;
					}
					User user = new UserRequest(mService, name).execute();
					if (user != null) {
						buddies.add(user);
					}
					int BATCH_SIZE = 16;
					if (buddies.size() >= BATCH_SIZE) {
						count += save(syncResult, buddies);
						buddies.clear();
					}
				}
				if (buddies.size() > 0) {
					count += save(syncResult, buddies);
				}
			} else {
				Timber.i("...no buddies to update");
			}
			Timber.i("...saved " + count + " records");
		} finally {
			Timber.i("...complete!");
		}
	}

	private int save(SyncResult syncResult, List<User> buddies) {
		int count = mPersister.save(buddies);
		Timber.i("...saved " + buddies.size() + " buddies");
		syncResult.stats.numUpdates += buddies.size();
		return count;
	}

	protected abstract String getLogMessage();

	protected abstract List<String> getBuddyNames();
}
