package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.ResolverUtils;

/**
 * Syncs all buddies that haven't been updated completely.
 */
public class SyncBuddiesDetailUnupdated extends SyncTask {
	private static final String TAG = makeLogTag(SyncBuddiesDetailUnupdated.class);

	public SyncBuddiesDetailUnupdated(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing unupdated buddies...");
		try {
			List<String> names = ResolverUtils.queryStrings(context.getContentResolver(), Buddies.CONTENT_URI,
				Buddies.BUDDY_NAME, SyncColumns.UPDATED + "=0 OR " + SyncColumns.UPDATED + " IS NULL", null);
			LOGI(TAG, "...found " + names.size() + " buddies to update");
			if (names.size() > 0) {
				List<User> buddies = new ArrayList<User>(names.size());
				BuddyPersister persister = new BuddyPersister(context);
				for (String name : names) {
					if (isCancelled()) {
						LOGI(TAG, "...canceled while syncing buddies");
						break;
					}
					buddies.add(mService.user(name));
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

	@Override
	public int getNotification() {
		return R.string.sync_notification_buddies_unupdated;
	}
}
