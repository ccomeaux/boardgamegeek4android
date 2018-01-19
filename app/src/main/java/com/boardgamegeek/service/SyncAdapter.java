package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.SyncCompleteEvent;
import com.boardgamegeek.events.SyncEvent;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.util.BatteryUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.NetworkUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;

import org.greenrobot.eventbus.EventBus;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private final Context context;
	private SyncTask currentTask;
	private boolean isCancelled;

	@DebugLog
	public SyncAdapter(Context context) {
		super(context, false);
		this.context = context;

		if (!BuildConfig.DEBUG) {
			Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable throwable) {
					Timber.e(throwable, "Uncaught sync exception, suppressing UI in release build.");
				}
			});
		}
	}

	/**
	 * Perform a sync. This builds a list of sync tasks from the types specified in the {@code extras bundle}, iterating
	 * over each. It posts and removes a {@code SyncEvent} with the type of sync task. As well as showing the progress
	 * in a notification.
	 */
	@DebugLog
	@Override
	public void onPerformSync(@NonNull Account account, @NonNull Bundle extras, String authority, ContentProviderClient provider, @NonNull SyncResult syncResult) {
		isCancelled = false;
		final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
		final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
		final boolean initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);
		final int type = extras.getInt(SyncService.EXTRA_SYNC_TYPE, SyncService.FLAG_SYNC_ALL);

		Timber.i("Beginning sync for account %s, uploadOnly=%s manualSync=%s initialize=%s, type=%d", account.name, uploadOnly, manualSync, initialize, type);

		if (initialize) {
			ContentResolver.setIsSyncable(account, authority, 1);
			ContentResolver.setSyncAutomatically(account, authority, true);
			Bundle b = new Bundle();
			ContentResolver.addPeriodicSync(account, authority, b, 24 * 60 * 60); // 24 hours
		}

		if (!shouldContinueSync()) return;

		toggleCancelReceiver(true);
		List<SyncTask> tasks = createTasks(context, type, uploadOnly);
		for (int i = 0; i < tasks.size(); i++) {
			if (isCancelled) {
				Timber.i("Cancelling all sync tasks");
				if (currentTask != null) {
					notifySyncIsCancelled(currentTask.getNotificationSummaryMessageId());
				}
				break;
			}
			currentTask = tasks.get(i);
			try {
				EventBus.getDefault().postSticky(new SyncEvent(currentTask.getSyncType()));
				currentTask.updateProgressNotification();
				currentTask.execute(account, syncResult);
				EventBus.getDefault().removeStickyEvent(SyncEvent.class);
				if (currentTask.isCancelled()) {
					Timber.i("Sync task %s has requested the sync operation to be cancelled", currentTask);
					break;
				}
			} catch (Exception e) {
				Timber.e(e, "Syncing %s", currentTask);
				syncResult.stats.numIoExceptions += 10;
				showException(currentTask, e);
				if (e.getCause() instanceof SocketTimeoutException) {
					break;
				}
			}
		}
		NotificationUtils.cancel(context, NotificationUtils.TAG_SYNC_PROGRESS);
		toggleCancelReceiver(false);
		EventBus.getDefault().post(new SyncCompleteEvent());
	}

	/**
	 * Indicates that a sync operation has been canceled.
	 */
	@DebugLog
	@Override
	public void onSyncCanceled() {
		super.onSyncCanceled();
		Timber.i("Sync cancel requested.");
		isCancelled = true;
		if (currentTask != null) currentTask.cancel();
	}

	/**
	 * Determine if the sync should continue based on the current state of the device.
	 */
	@DebugLog
	private boolean shouldContinueSync() {
		if (NetworkUtils.isOffline(context)) {
			Timber.i("Skipping sync; offline");
			return false;
		}

		if (PreferencesUtils.getSyncOnlyCharging(context) && !BatteryUtils.isCharging(context)) {
			Timber.i("Skipping sync; not charging");
			return false;
		}

		if (PreferencesUtils.getSyncOnlyWifi(context) && !NetworkUtils.isOnWiFi(context)) {
			Timber.i("Skipping sync; not on wifi");
			return false;
		}

		if (BatteryUtils.isBatteryLow(context)) {
			Timber.i("Skipping sync; battery low");
			return false;
		}

		return true;
	}

	/**
	 * Create a list of sync tasks based on the specified type.
	 */
	@DebugLog
	@NonNull
	private List<SyncTask> createTasks(Context context, final int typeList, boolean uploadOnly) {
		BggService service = Adapter.createForXmlWithAuth(context);
		List<SyncTask> tasks = new ArrayList<>();
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_COLLECTION_UPLOAD)) {
			tasks.add(new SyncCollectionUpload(context, service));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_COLLECTION_DOWNLOAD) && !uploadOnly) {
			if (PreferencesUtils.isCollectionSetToSync(context)) {
				long lastCompleteSync = Authenticator.getLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE);
				if (lastCompleteSync >= 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < 7) {
					tasks.add(new SyncCollectionModifiedSince(context, service));
				} else {
					tasks.add(new SyncCollectionComplete(context, service));
				}
			} else {
				Timber.i("...no statuses set to sync");
			}

			tasks.add(new SyncCollectionUnupdated(context, service));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_GAMES) && !uploadOnly) {
			tasks.add(new SyncGamesRemove(context, service));
			tasks.add(new SyncGamesOldest(context, service));
			tasks.add(new SyncGamesUnupdated(context, service));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_PLAYS_UPLOAD)) {
			tasks.add(new SyncPlaysUpload(context, service));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_PLAYS_DOWNLOAD) && !uploadOnly) {
			tasks.add(new SyncPlays(context, service));
		}
		if (shouldCreateTask(typeList, SyncService.FLAG_SYNC_BUDDIES) && !uploadOnly) {
			tasks.add(new SyncBuddiesList(context, service));
			tasks.add(new SyncBuddiesDetailOldest(context, service));
			tasks.add(new SyncBuddiesDetailUnupdated(context, service));
		}
		return tasks;
	}

	private boolean shouldCreateTask(int typeList, int type) {
		return (typeList & type) == type;
	}

	/**
	 * Enable or disable the cancel receiver. (There's no reason for the receiver to be enabled when the sync isn't running.
	 */
	@DebugLog
	private void toggleCancelReceiver(boolean enable) {
		ComponentName receiver = new ComponentName(context, CancelReceiver.class);
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(receiver, enable ?
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
			PackageManager.DONT_KILL_APP);
	}

	/**
	 * Show a notification of any exception thrown by a sync task that isn't caught by the task.
	 * ]
	 */
	@DebugLog
	private void showException(@NonNull SyncTask task, @NonNull Throwable t) {
		String message = t.getMessage();
		if (TextUtils.isEmpty(message)) {
			Throwable t1 = t.getCause();
			if (t1 != null) {
				message = t1.toString();
			}
		}

		Timber.w(message);

		if (!PreferencesUtils.getSyncShowErrors(context)) return;

		final int messageId = task.getNotificationSummaryMessageId();
		if (messageId != SyncTask.NO_NOTIFICATION) {
			CharSequence text = context.getText(messageId);
			NotificationCompat.Builder builder = NotificationUtils
				.createNotificationBuilder(context, R.string.sync_notification_title_error, NotificationUtils.CHANNEL_ID_ERROR)
				.setContentText(text)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setCategory(NotificationCompat.CATEGORY_ERROR);
			if (!TextUtils.isEmpty(message)) {
				builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message).setSummaryText(text));
			}
			NotificationUtils.notify(context, NotificationUtils.TAG_SYNC_ERROR, 0, builder);
		}
	}

	/**
	 * Show that the sync was cancelled in a notification. This may be useless since the notification is cancelled
	 * almost immediately after this is shown.
	 */
	@DebugLog
	private void notifySyncIsCancelled(int messageId) {
		if (!PreferencesUtils.getSyncShowNotifications(context)) return;

		CharSequence contextText = messageId == SyncTask.NO_NOTIFICATION ? "" : context.getText(messageId);

		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.sync_notification_title_cancel, NotificationUtils.CHANNEL_ID_SYNC_PROGRESS)
			.setContentText(contextText)
			.setCategory(NotificationCompat.CATEGORY_SERVICE);
		NotificationUtils.notify(context, NotificationUtils.TAG_SYNC_PROGRESS, 0, builder);
	}
}
