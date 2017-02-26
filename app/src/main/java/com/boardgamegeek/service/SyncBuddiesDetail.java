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
	private static final int BATCH_SIZE = 16;
	private static final long SLEEP_MILLIS = 2000L;
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
			Timber.i("...found %,d buddies to update", names.size());
			if (names.size() > 0) {
				for (String name : names) {
					if (isCancelled()) {
						Timber.i("...canceled while syncing buddies");
						break;
					}

					updateProgressNotification(R.string.sync_notification_buddy, name);

					User user = null;
					try {
						Call<User> call = service.user(name);
						Response<User> response = call.execute();
						if (!response.isSuccessful()) {
							showError(String.format("Unsuccessful user fetch with code: %s", response.code()));
							syncResult.stats.numIoExceptions++;
						}
						user = response.body();
					} catch (IOException e) {
						showError(String.format("Unsuccessful user fetch with exception: %s", e.getLocalizedMessage()));
						syncResult.stats.numIoExceptions++;
					}

					if (user == null) {
						break;
					}

					save(syncResult, user);
					count++;

					// pause between fetching users
					try {
						Thread.sleep(SLEEP_MILLIS);
					} catch (InterruptedException e) {
						Timber.w(e, "Interrupted while sleeping between user fetches.");
						break;
					}
				}
			} else {
				Timber.i("...no buddies to update");
			}
			Timber.i("...saved %,d records", count);
		} finally {
			Timber.i("...complete!");
		}
	}

	private void save(@NonNull SyncResult syncResult, @NonNull User user) {
		updateProgressNotification(R.string.sync_notification_buddies_list_storing);
		persister.save(user);
		syncResult.stats.numUpdates++;
	}

	/**
	 * Returns a log message to use for debugging purposes.
	 */
	@NonNull
	protected abstract String getLogMessage();

	protected abstract List<String> getBuddyNames();
}
