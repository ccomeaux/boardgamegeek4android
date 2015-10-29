package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;

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
	private BuddyPersister persister;

	public SyncBuddiesDetail(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, @NonNull SyncResult syncResult) {
		Timber.i(getLogMessage());
		try {
			if (!PreferencesUtils.getSyncBuddies(context)) {
				Timber.i("...buddies not set to sync");
				return;
			}

			persister = new BuddyPersister(context);
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
					User user = new UserRequest(bggService, name).execute();
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

	private int save(@NonNull SyncResult syncResult, @NonNull List<User> buddies) {
		int count = persister.save(buddies);
		Timber.i("...saved " + buddies.size() + " buddies");
		syncResult.stats.numUpdates += buddies.size();
		return count;
	}

	/**
	 * Returns a log message to use for debugging purposes.
	 */
	@NonNull
	protected abstract String getLogMessage();

	protected abstract List<String> getBuddyNames();
}
