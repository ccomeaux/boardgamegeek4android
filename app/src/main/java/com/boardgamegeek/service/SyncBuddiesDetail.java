package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.User;
import com.boardgamegeek.model.persister.BuddyPersister;
import com.boardgamegeek.util.PreferencesUtils;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public abstract class SyncBuddiesDetail extends SyncTask {
	private static final long SLEEP_MILLIS = 2000L;
	private SyncResult syncResult;
	private String notificationMessage;

	public SyncBuddiesDetail(Context context, BggService service) {
		super(context, service);
	}

	@Override
	public void execute(Account account, @NonNull SyncResult syncResult) {
		Timber.i(getLogMessage());
		this.syncResult = syncResult;
		try {
			if (!PreferencesUtils.getSyncBuddies(context)) {
				Timber.i("...buddies not set to sync");
				return;
			}

			BuddyPersister persister = new BuddyPersister(context);
			int count = 0;
			List<String> names = getBuddyNames();
			Timber.i("...found %,d buddies to update", names.size());
			if (names.size() > 0) {
				for (String name : names) {
					if (isCancelled()) {
						Timber.i("...canceled while syncing buddies");
						break;
					}

					notificationMessage = context.getString(R.string.sync_notification_buddy, name);
					updateProgressNotification(notificationMessage);

					User user = requestUser(name);

					if (user == null) break;

					persister.saveUser(user);
					syncResult.stats.numUpdates++;
					count++;

					if (wasSleepInterrupted(SLEEP_MILLIS)) break;
				}
			} else {
				Timber.i("...no buddies to update");
			}
			Timber.i("...saved %,d records", count);
		} finally {
			Timber.i("...complete!");
		}
	}

	private User requestUser(String name) {
		User user = null;
		try {
			Call<User> call = service.user(name);
			Response<User> response = call.execute();
			if (!response.isSuccessful()) {
				showError(notificationMessage, response.code());
				syncResult.stats.numIoExceptions++;
				cancel();
			}
			user = response.body();
		} catch (IOException e) {
			showError(notificationMessage, e);
			syncResult.stats.numIoExceptions++;
			cancel();
		}
		return user;
	}

	/**
	 * Returns a log message to use for debugging purposes.
	 */
	@NonNull
	protected abstract String getLogMessage();

	/**
	 * Get a list of usernames to sync.
	 */
	protected abstract List<String> getBuddyNames();
}
