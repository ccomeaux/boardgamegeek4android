package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

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
	private SyncResult syncResult;
	@StringRes private int currentDetailResId;

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
		this.syncResult = syncResult;
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

			updateNotification(R.string.sync_notification_buddies_list_downloading);
			User user = requestUser(account, syncResult);
			if (user == null) return;

			updateNotification(R.string.sync_notification_buddies_list_storing);
			storeUserInAuthenticator(user);
			BuddyPersister persister = new BuddyPersister(context);
			persistUser(user, persister);

			updateNotification(R.string.sync_notification_buddies_list_pruning);
			pruneOldBuddies(persister);

			Authenticator.putLong(context, SyncService.TIMESTAMP_BUDDIES, persister.getTimestamp());
		} finally {
			Timber.i("...complete!");
		}
	}

	private void updateNotification(@StringRes int detailResId) {
		currentDetailResId = R.string.sync_notification_buddies_list_downloading;
		updateProgressNotification(context.getString(detailResId));
	}

	private User requestUser(@NonNull Account account, @NonNull SyncResult syncResult) {
		User user = null;
		Call<User> call = service.user(account.name, 1, 1);
		try {
			Response<User> response = call.execute();
			if (!response.isSuccessful()) {
				showError(context.getString(currentDetailResId), response.code());
				syncResult.stats.numIoExceptions++;
				cancel();
			}
			user = response.body();
		} catch (IOException e) {
			showError(context.getString(currentDetailResId), e);
			syncResult.stats.numIoExceptions++;
			cancel();
		}
		return user;
	}

	private void storeUserInAuthenticator(User user) {
		Authenticator.putInt(context, Authenticator.KEY_USER_ID, user.getId());
		AccountUtils.setUsername(context, user.name);
		AccountUtils.setFullName(context, PresentationUtils.buildFullName(user.firstName, user.lastName));
		AccountUtils.setAvatarUrl(context, user.avatarUrl);
	}

	private void persistUser(User user, BuddyPersister persister) {
		int count = 0;
		count += persister.saveBuddy(Buddy.fromUser(user));
		count += persister.saveBuddies(user.getBuddies());
		syncResult.stats.numEntries += count;
		Timber.i("Synced %,d buddies", count);
	}

	private void pruneOldBuddies(BuddyPersister persister) {
		ContentResolver resolver = context.getContentResolver();
		int count = resolver.delete(Buddies.CONTENT_URI,
			Buddies.UPDATED_LIST + "<?",
			new String[] { String.valueOf(persister.getTimestamp()) });
		syncResult.stats.numDeletes += count;
		Timber.i("Pruned %,d users who are no longer buddies", count);
	}

	@Override
	public int getNotificationSummaryMessageId() {
		return R.string.sync_notification_buddies_list;
	}
}
