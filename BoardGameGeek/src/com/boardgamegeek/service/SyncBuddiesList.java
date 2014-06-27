package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Buddy;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;

public class SyncBuddiesList extends SyncTask {
	private static final String TAG = makeLogTag(SyncBuddiesList.class);

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing list of buddies in the collection...");
		try {
			if (!PreferencesUtils.getSyncBuddies(context)) {
				LOGI(TAG, "...buddies not set to sync");
				return;
			}

			AccountManager accountManager = AccountManager.get(context);
			String s = accountManager.getUserData(account, SyncService.TIMESTAMP_BUDDIES);
			long lastCompleteSync = TextUtils.isEmpty(s) ? 0 : Long.parseLong(s);
			if (lastCompleteSync >= 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < 3) {
				LOGI(TAG, "...skipping; we synced already within the last 3 days");
				return;
			}

			long startTime = System.currentTimeMillis();
			BggService service = Adapter.create();

			// XXX: buddies don't seem to be paged at 100. I get 204 on the first page
			int page = 1;
			User user = service.user(account.name, 1, page);

			accountManager.setUserData(account, Authenticator.KEY_USER_ID, String.valueOf(user.id));

			int count = 0;
			count += BuddyPersister.saveList(context, Buddy.fromUser(user), startTime);
			count += BuddyPersister.saveList(context, user.getBuddies(), startTime);
			LOGI(TAG, "Synced " + count + " buddies");
			// TODO: update syncResult.stats

			// TODO: delete avatar images associated with this list
			// Actually, these are now only in the cache!
			ContentResolver resolver = context.getContentResolver();
			count = resolver.delete(Buddies.CONTENT_URI, Buddies.UPDATED_LIST + "<?",
				new String[] { String.valueOf(startTime) });
			// syncResult.stats.numDeletes += count;

			accountManager.setUserData(account, SyncService.TIMESTAMP_BUDDIES, String.valueOf(startTime));
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_buddies_list;
	}
}
