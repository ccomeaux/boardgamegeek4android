package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BoardGameGeekService;
import com.boardgamegeek.io.UserRequest;
import com.boardgamegeek.model.Buddy;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;

import timber.log.Timber;

/**
 * Syncs the list of buddies. Only runs every few days.
 */
public class SyncBuddiesList extends SyncTask {
	public SyncBuddiesList(Context context, BoardGameGeekService service) {
		super(context, service);
	}

	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_BUDDIES;
	}

	@Override
	public void execute(@NonNull Account account, @NonNull SyncResult syncResult) {
		Timber.i("Syncing list of buddies...");
		try {
			if (!PreferencesUtils.getSyncBuddies(context)) {
				Timber.i("...buddies not set to sync");
				return;
			}

			long lastCompleteSync = Authenticator.getLong(context, SyncService.TIMESTAMP_BUDDIES);
			if (lastCompleteSync >= 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < 3) {
				Timber.i("...skipping; we synced already within the last 3 days");
				return;
			}

			showNotification("Downloading list of GeekBuddies");
			User user = new UserRequest(service, account.name, true).execute();
			if (user == null) {
				return;
			}

			showNotification("Storing list of GeekBuddies");

			Authenticator.putInt(context, Authenticator.KEY_USER_ID, user.getId());

			BuddyPersister persister = new BuddyPersister(context);
			int count = 0;
			count += persister.saveList(Buddy.fromUser(user));
			count += persister.saveList(user.getBuddies());
			syncResult.stats.numEntries += count;
			Timber.i("Synced " + count + " buddies");

			showNotification("Discarding old GeekBuddies");
			// TODO: delete avatar images associated with this list
			// Actually, these are now only in the cache!
			ContentResolver resolver = context.getContentResolver();
			count = resolver.delete(Buddies.CONTENT_URI, Buddies.UPDATED_LIST + "<?",
				new String[] { String.valueOf(persister.getTimestamp()) });
			syncResult.stats.numDeletes += count;
			Timber.i("Removed " + count + " people who are no longer buddies");

			Authenticator.putLong(context, SyncService.TIMESTAMP_BUDDIES, persister.getTimestamp());
		} finally {
			Timber.i("...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_buddies_list;
	}
}
