package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Buddy;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Syncs the list of buddies. Only runs every few days.
 */
public class SyncBuddiesList extends SyncTask {
	public SyncBuddiesList(Context context, BggService service) {
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

			updateProgressNotification(R.string.sync_notification_buddies_list_downloading);

			User user = null;
			Call<User> call = service.user(account.name, 1, 1);
			try {
				Response<User> response = call.execute();
				if (!response.isSuccessful()) {
					showError(String.format("Unsuccessful user fetch with code: %s", response.code()));
				}
				user = response.body();
			} catch (IOException e) {
				showError(String.format("Unsuccessful user fetch with exception: %s", e.getLocalizedMessage()));
			}
			if (user == null) {
				return;
			}

			updateProgressNotification(R.string.sync_notification_buddies_list_storing);

			Authenticator.putInt(context, Authenticator.KEY_USER_ID, user.getId());
			AccountUtils.setUsername(context, user.name);
			AccountUtils.setFullName(context, PresentationUtils.buildFullName(user.firstName, user.lastName));
			AccountUtils.setAvatarUrl(context, user.avatarUrl);

			BuddyPersister persister = new BuddyPersister(context);
			int count = 0;
			count += persister.saveList(Buddy.fromUser(user));
			count += persister.saveList(user.getBuddies());
			syncResult.stats.numEntries += count;
			Timber.i("Synced %,d buddies", count);

			updateProgressNotification(R.string.sync_notification_buddies_list_pruning);
			ContentResolver resolver = context.getContentResolver();
			count = resolver.delete(Buddies.CONTENT_URI,
				Buddies.UPDATED_LIST + "<?",
				new String[] { String.valueOf(persister.getTimestamp()) });
			syncResult.stats.numDeletes += count;
			Timber.i("Pruned %,d users who are no longer buddies", count);

			Authenticator.putLong(context, SyncService.TIMESTAMP_BUDDIES, persister.getTimestamp());
		} finally {
			Timber.i("...complete!");
		}
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_buddies_list;
	}
}
