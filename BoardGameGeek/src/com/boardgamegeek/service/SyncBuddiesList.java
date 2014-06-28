package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Buddy;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;

/**
 * Syncs the list of buddies. Only runs every few days.
 */
public class SyncBuddiesList extends SyncTask {
	private static final String TAG = makeLogTag(SyncBuddiesList.class);

	public SyncBuddiesList(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing list of buddies in the collection...");
		try {
			if (!PreferencesUtils.getSyncBuddies(context)) {
				LOGI(TAG, "...buddies not set to sync");
				return;
			}

			long lastCompleteSync = Authenticator.getLong(context, SyncService.TIMESTAMP_BUDDIES);
			if (lastCompleteSync >= 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < 3) {
				LOGI(TAG, "...skipping; we synced already within the last 3 days");
				return;
			}

			BuddyPersister persister = new BuddyPersister(context);

			// XXX: buddies don't seem to be paged at 100. I get 204 on the first page
			User user = mService.user(account.name, 1, 1);

			Authenticator.putInt(context, Authenticator.KEY_USER_ID, user.id);

			int count = 0;
			count += persister.saveList(Buddy.fromUser(user));
			count += persister.saveList(user.getBuddies());
			LOGI(TAG, "Synced " + count + " buddies");
			// TODO: update syncResult.stats

			// TODO: delete avatar images associated with this list
			// Actually, these are now only in the cache!
			ContentResolver resolver = context.getContentResolver();
			count = resolver.delete(Buddies.CONTENT_URI, Buddies.UPDATED_LIST + "<?",
				new String[] { String.valueOf(persister.getTimestamp()) });
			// syncResult.stats.numDeletes += count;

			Authenticator.putLong(context, SyncService.TIMESTAMP_BUDDIES, persister.getTimestamp());
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_buddies_list;
	}
}
