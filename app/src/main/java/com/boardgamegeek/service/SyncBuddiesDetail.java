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

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

public abstract class SyncBuddiesDetail extends SyncTask {
	private static final String TAG = makeLogTag(SyncBuddiesDetail.class);
	private static final int MAX_RETRIES = 4;
	private static final int RETRY_BACKOFF_IN_MS = 5000;

	public SyncBuddiesDetail(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, SyncResult syncResult) {
		LOGI(TAG, getLogMessage());
		try {
			if (!PreferencesUtils.getSyncBuddies(mContext)) {
				LOGI(TAG, "...buddies not set to sync");
				return;
			}

			List<String> names = getBuddyNames();
			LOGI(TAG, "...found " + names.size() + " buddies to update");
			if (names.size() > 0) {
				showNotification(StringUtils.formatList(names));
				List<User> buddies = new ArrayList<User>(names.size());
				BuddyPersister persister = new BuddyPersister(mContext);
				for (String name : names) {
					if (isCancelled()) {
						LOGI(TAG, "...canceled while syncing buddies");
						break;
					}
					User user = getUser(mService, name);
					if (user != null) {
						buddies.add(user);
					}
				}

				int count = persister.save(buddies);
				syncResult.stats.numUpdates += buddies.size();
				LOGI(TAG, "...saved " + count + " records for " + buddies.size() + " buddies");
			} else {
				LOGI(TAG, "...no buddies to update");
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	protected User getUser(BggService service, String name) {
		int retries = 0;
		while (true) {
			try {
				return service.user(name);
			} catch (Exception e) {
				if (e instanceof RetryableException || e.getCause() instanceof RetryableException) {
					retries++;
					if (retries > MAX_RETRIES) {
						break;
					}
					try {
						LOGI(TAG, "...retrying #" + retries);
						Thread.sleep(retries * retries * RETRY_BACKOFF_IN_MS);
					} catch (InterruptedException e1) {
						LOGI(TAG, "Interrupted while sleeping before retry " + retries);
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
