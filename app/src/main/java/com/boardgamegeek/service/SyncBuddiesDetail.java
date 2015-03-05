package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RetryableException;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public abstract class SyncBuddiesDetail extends SyncTask {
	private static final int MAX_RETRIES = 4;
	private static final int RETRY_BACKOFF_IN_MS = 5000;
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
					User user = getUser(mService, name);
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

	protected User getUser(BggService service, String name) {
		Timber.i("...syncing username=" + name);
		int retries = 0;
		while (true) {
			try {
				return service.user(name);
			} catch (Exception e) {
				if (e.getCause() instanceof RetryableException) {
					retries++;
					if (retries > MAX_RETRIES) {
						Timber.i("...giving up - too many retries");
						break;
					}
					try {
						long time = retries * retries * RETRY_BACKOFF_IN_MS;
						Timber.i("...retry #" + retries + " in " + time / 1000 + "s");
						Thread.sleep(time);
					} catch (InterruptedException e1) {
						Timber.i("Interrupted while sleeping before retry " + retries);
						break;
					}
				} else {
					throw e;
				}
			}
		}

		return null;
	}

	protected abstract String getLogMessage();

	protected abstract List<String> getBuddyNames();
}
